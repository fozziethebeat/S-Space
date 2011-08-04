/*
 * Copyright 2010 David Jurgens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.AtomicSparseVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A concurrent, thread-safe, growable {@code SparseMatrix} class that is
 * optimized for operations only access one value of the matrix at a time.  This
 * class allows multiple threads to operate on the same matrix where all methods
 * are concurrent to the fullest extent possible.<p>
 *
 * This class offers different performance trade-offs than the {@link
 * AtomicGrowingSparseMatrix}.  Specifically, this class supports concurrent
 * write access to the same row.  Atomicity is localized to the specific matrix
 * entry, which ensures high concurrency for workloads consisting primarily of
 * {@code get}, {@code addAndGet}, {@code getAndAdd} and {@code set}.  This
 * performance comes at a cost for the full row- or column-related operations.
 * Each time these operations are used after a modification to the matrix, a
 * full {@code O(m * n)} computation must be done to determine the structure of
 * the matrix.  An subsequent calls until the next modification will not incur
 * this penalty and will operate in {@code O(k)} where {@code k} is the number
 * of non-zero entries in the row or column.  It is therefore highly recommended
 * that vector method be used only during periods when the matrix is not likely
 * to be modified.<p>
 *
 * This class also provides support for access the matrix data without ensuring
 * data coherency (i.e. non-atomic) through the {@link #getRowVectorUnsafe(int)}
 * and {@link #getColumnVectorUnsafe(int)} methods.  These methods are designed
 * to be used only in circumstances where the matrix is guaranteed to only be
 * accessed by one thread.  In such circumstances, the overhead of locking row
 * or column data can be avoided with no side-effects. <i>Use these methods with
 * great caution</i>.<p>
 *
 * @author David Jurgens
 *
 * @see AtomicGrowingSparseMatrix
 * @see Matrices#synchronizedMatrix(Matrix)
 */
public class AtomicGrowingSparseHashMatrix 
        implements AtomicMatrix, SparseMatrix {
    
    /**
     * The matrix entries that are currently being write-locked by some thread.
     * The value of this map is not meaningful.
     */
    private final ConcurrentMap<Entry,Object> lockedEntries;

    /**
     * A mapping from row, column to the value at that entry in the matrix.
     */
    private final ConcurrentMap<Entry,Double> matrixEntries;

    /**
     * The number of rows represented in this {@code AtomicGrowingSparseMatrix}.
     */
    private final AtomicInteger rows;

    /**
     * The number of columns represented in this {@code
     * AtomicGrowingSparseMatrix}.
     */
    private final AtomicInteger cols;

    /**
     * A counter of the number of modications to this matrix.  This value is
     * compared with the {@link #lastVectorCacheUpdate} to determine whether the
     * vector cache is out of date.
     */
    private final AtomicInteger modifications;

    /**
     * The value of {@link #modifications} when {@link #updateVectorCache()} was
     * last called.
     */
    private final AtomicInteger lastVectorCacheUpdate;

    /**
     * A mapping from row to the columns that contain non-zero values.  This
     * mapping is only valid when the vector-cache is valid.
     *
     * @see #updateVectorCache()
     */
    private int[][] rowToColsCache;

    /**
     * A mapping from column to the rows that contain non-zero values.  This
     * mapping is only valid when the vector-cache is valid.
     *
     * @see #updateVectorCache()
     */
    private int[][] colToRowsCache;

    /**
     * Create an {@code AtomicGrowingSparseMatrix} with 0 rows and 0 columns.
     */
    public AtomicGrowingSparseHashMatrix() {
        this.rows = new AtomicInteger(0);
        this.cols = new AtomicInteger(0);
        modifications = new AtomicInteger(0);
        lastVectorCacheUpdate = new AtomicInteger(0);

        rowToColsCache = null;
        colToRowsCache = null;

        // Base the concurrency of the maps on the number of avaible processors,
        // which assumes that all use cases use this value as a hint for
        // concurrency
        int threads = Runtime.getRuntime().availableProcessors();
        lockedEntries = new ConcurrentHashMap<Entry,Object>(
            1000, .75f, threads * 16);
        matrixEntries = new ConcurrentHashMap<Entry,Double>(
            10000, 4f, threads * 16);
    }
    
    /**
     * {@inheritDoc}
     */
    public double addAndGet(int row, int col, double delta) {
        checkIndices(row, col, true);
        Entry e = new Entry(row, col);
        // Spin waiting for the entry to be unlocked
        while (lockedEntries.putIfAbsent(e, new Object()) != null)
            ;
        Double val = matrixEntries.get(e);
        double newVal = (val == null) ? delta : delta + val;
        if (newVal != 0) {
            matrixEntries.put(e, newVal);
            // Only invalidate the cache if the number of rows or columns
            // containing data has changed
            if (val == null)
                modifications.incrementAndGet();
        }
        else {
            matrixEntries.remove(e);
            modifications.incrementAndGet();
        }
        lockedEntries.remove(e);
        return newVal;
    }

    /**
     * Verify that the given row and column value is non-negative, and
     * optionally expand the size of the matrix if the row or column are outside
     * the current bounds.
     *
     * @param row the row index to check.
     * @param the the column index to check.
     * @param expand {@code true} if the current dimensions of the matrix should
     *        be updated if either parameter exceeds the current values
     */    
    private void checkIndices(int row, int col, boolean expand) {
         if (row < 0 || col < 0) {
             throw new ArrayIndexOutOfBoundsException();
         }
         if (expand) {
             int r = row + 1;
             int cur = 0;
             while (r > (cur = rows.get()) && !rows.compareAndSet(cur, r))
                 ;
             int c = col + 1;
             cur = 0;
             while (c > (cur = cols.get()) && !cols.compareAndSet(cur, c))
                 ;
         }
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols.get();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        checkIndices(row, col, false);
        Double val = matrixEntries.get(new Entry(row, col));
        return (val == null) ? 0 : val;
    }

    /**
     * {@inheritDoc}
     */
    public double getAndAdd(int row, int col, double delta) {
        checkIndices(row, col, true);
        Entry e = new Entry(row, col);
        // Spin waiting for the entry to be unlocked
        while (lockedEntries.putIfAbsent(e, new Object()) != null)
            ;
        Double val = matrixEntries.get(e);
        double newVal = (val == null) ? delta : delta + val;
        if (newVal != 0) {
            matrixEntries.put(e, newVal);
            // Only invalidate the cache if the number of rows or columns
            // containing data has changed
            if (val == null)
                modifications.incrementAndGet();
        }
        else {
            matrixEntries.remove(e);
            modifications.incrementAndGet();
        }
        lockedEntries.remove(e);
        return (val == null) ? 0 : val;
    }


    /**
     * {@inheritDoc} The length of the returned column reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #rows()}
     */
    public double[] getColumn(int column) {
        return getColumnVector(column).toArray();
    }

    /**
     * {@inheritDoc} The length of the returned row vector reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #rows()}
     */
    public SparseDoubleVector getColumnVector(int column) {
        return getColumnVector(column, true);
    }

    /**
     * Provides non-atomic access to the data at the specified column, which may
     * present an inconsistent view of the data if this matrix is being
     * concurrently modified.  This method should only be used in special cases
     * where the vector is being accessed at a time when the matrix (or this
     * particular column) will not be modified.
     */
    public SparseDoubleVector getColumnVectorUnsafe(int column) {
        return getColumnVector(column, false);
    }

    /**
     * Returns the column vector, locking the data if {@code shouldLock} is
     * {@code true}.
     */
    private SparseDoubleVector getColumnVector(int column, boolean shouldLock) {
        int r = rows.get();
        if (shouldLock)
            lockColumn(column, r);
        // Ensure that the column data is up to date 
        while (lastVectorCacheUpdate.get() != modifications.get()) 
            updateVectorCache();
        int[] rowArr = colToRowsCache[column];
        SparseDoubleVector colVec = new SparseHashDoubleVector(r);
        for (int row : rowArr)
            colVec.set(row, matrixEntries.get(new Entry(row, column)));
        if (shouldLock)
            unlockColumn(column, r);
        return colVec;
    }

    /**
     * {@inheritDoc} The length of the returned row reflects the size of matrix
     * at the time of the call, which may be different from earlier calls to
     * {@link #columns()}.
     */
    public double[] getRow(int row) {
        return getRowVector(row).toArray();
    }

    /**
     * {@inheritDoc} The length of the returned row vector reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #columns()}.
     */
    public SparseDoubleVector getRowVector(int row) {
        return getRowVector(row, true);
    }

    /**
     * Provides non-atomic access to the data at the specified row, which may
     * present an inconsistent view of the data if this matrix is being
     * concurrently modified.  This method should only be used in special cases
     * where the vector is being accessed at a time when the matrix (or this
     * particular row) will not be modified.
     */
    public SparseDoubleVector getRowVectorUnsafe(int row) {
        return getRowVector(row, false);
    }

    /**
     * Returns the row vector, locking the data if {@code shouldLock} is {@code
     * true}.
     */
    private SparseDoubleVector getRowVector(int row, boolean shouldLock) {
        int c = cols.get();
        if (shouldLock)
            lockRow(row, c);
        // Ensure that the column data is up to date 
        while (lastVectorCacheUpdate.get() != modifications.get()) 
            updateVectorCache();
        int[] colArr = rowToColsCache[row];
        SparseDoubleVector rowVec = new SparseHashDoubleVector(c);
        for (int column : colArr)
            rowVec.set(column, matrixEntries.get(new Entry(row, column)));
        if (shouldLock)
            unlockRow(row, c);
        return rowVec;
    }

    /**
     * Locks all the column entries for this row, thereby preventing write or
     * read access to the values.  Note that the number of columns to lock
     * <b>must</b> be the same value used with {@link #unlockRow(int,int)},
     * otherwise the unlock may potentially unlock matrix entries associated
     * with this lock call.
     *
     * @param row the row to lock
     * @param colsToLock the number of rows to lock.  This value should be the
     *        number of {@link #cols} at the time of the call.
     */
    private void lockRow(int row, int colsToLock) {
        // Put in an entry for all the row's columns
        for (int col = 0; col < colsToLock; ++col) {
            Entry e = new Entry(row, col);
            // Spin waiting for the entry to be unlocked
            while (lockedEntries.putIfAbsent(e, new Object()) != null)
                ;
        }
    }
    
    /**
     * Locks all the row entries for this column, thereby preventing write or
     * read access to the values.  Note that the number of rows to lock
     * <b>must</b> be the same value used with {@link #unlockColumn(int,int)},
     * otherwise the unlock may potentially unlock matrix entries associated
     * with this lock call.
     *
     * @param col the column to lock
     * @param rowsToLock the number of rows to lock.  This value should be the
     *        number of {@link #rows} at the time of the call.
     */
    private void lockColumn(int col, int rowsToLock) {
        // Put in an entry for all the columns's rows
        for (int row = 0; row < rowsToLock; ++row) {
            Entry e = new Entry(row, col);
            // Spin waiting for the entry to be unlocked
            while (lockedEntries.putIfAbsent(e, new Object()) != null)
                ;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows.get();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col, true);
        Entry e = new Entry(row, col);
        // Spin waiting for the entry to be unlocked
        while (lockedEntries.putIfAbsent(e, new Object()) != null)
            ;
        boolean present = matrixEntries.containsKey(e);
        if (val != 0) {
            matrixEntries.put(e, val);
            // Only invalidate the cache if the number of rows or columns
            // containing data has changed
            if (!present)
                modifications.incrementAndGet();
        }
        else if (present) {
            matrixEntries.remove(e);
            modifications.incrementAndGet();
        }

        lockedEntries.remove(e);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        setColumn(column, Vectors.asVector(values));
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector rowValues) {
        checkIndices(rowValues.length(), column, true);
        int r = rows.get();
        lockColumn(column, r);
        boolean modified = false;
        for (int row = 0; row < r; ++row) {
            double val = rowValues.get(row);
            Entry e = new Entry(row, column);
            boolean present = matrixEntries.containsKey(e);
            if (val != 0) {
                matrixEntries.put(e, val);
                // Only invalidate the cache if the number of rows or columns
                // containing data has changed
                modified = modified || !present;
            }
            else if (present) {
                matrixEntries.remove(e);
                modified = true;
            }
        }
        if (modified)
            modifications.incrementAndGet();
        unlockColumn(column, r);
    }
  
    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        setRow(row, Vectors.asVector(columns));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector colValues) {
        checkIndices(row, colValues.length(), true);
        int c = cols.get();
        lockRow(row, c);
        boolean modified = false;
        for (int col = 0; col < c; ++col) {
            double val = colValues.get(col);
            Entry e = new Entry(row, col);
            boolean present = matrixEntries.containsKey(e);
            if (val != 0) {
                matrixEntries.put(e, val);
                // Only invalidate the cache if the number of rows or columns
                // containing data has changed
                modified = modified || !present;
            }
            else if (present) {
                matrixEntries.remove(e);
                modified = true;
            }
        }
        if (modified)
            modifications.incrementAndGet();
        unlockRow(row, c);
    }
  
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        int r = rows.get();
        int c = cols.get();
        for (int i = 0; i < r; ++i) 
            lockRow(i, c);

        double[][] m = new double[r][0];
        for (int i = 0; i < r; ++i) {
            DoubleVector row = getRowVector(i);
            // Ensure that we see a consistent length for all the rows
            if (row.length() != c)
                row = Vectors.subview(row, 0, c);
            m[i] = row.toArray();
        }

        for (int i = 0; i < r; ++i) 
            unlockRow(i, c);
        
        return m;
    }

    /**
     * Unlocks the column for write access based on the number of rows that were
     * initially locked
     *
     * @param col the column to unlock
     * @param rowsToUnlock the number of rows locked by the previous {@link
     *        #lockColumn(int, int) call}
     */
    private void unlockColumn(int col, int rowsToUnlock) {
        // Remove the locks on all of the columns's columns
        for (int row = 0; row < rowsToUnlock; ++row) 
            lockedEntries.remove(new Entry(row, col));
    }

    /**
     * Unlocks the row for write access based on the number of columns that were
     * initially locked
     *
     * @param row the row to unlock
     * @param colsToUnlock the number of cols locked by the previous {@link
     *        #lockRow(int, int) call}
     */
    private void unlockRow(int row, int colsToUnlock) {        
        // Remove the locks on all of the row's columns
        for (int col = 0; col < colsToUnlock; ++col) 
            lockedEntries.remove(new Entry(row, col));
    }

    /**
     * Updates the {@link Vector} cache data so that any call to {@link
     * #getRowVector(int)} or {@link #getColumnVector(int)} has an accurate
     * mapping of that row or column's non-zero values.  Should the matrix be
     * modifed during this call, the cache will be repeatedly computed until it
     * is up-to-date.
     */
    private synchronized void updateVectorCache() {
        // NOTE: this method is synchronized to prevent having mulitple threads
        // potentiall recomputing the cache at the same time.
        while (lastVectorCacheUpdate.get() != modifications.get()) {
            lastVectorCacheUpdate.set(modifications.get());
            Map<Integer,List<Integer>> rowVals = 
                new HashMap<Integer,List<Integer>>();
            Map<Integer,List<Integer>> colVals = 
                new HashMap<Integer,List<Integer>>();

            for (Entry e : matrixEntries.keySet()) {
                int row = e.row;
                int col = e.col;
                List<Integer> c = rowVals.get(row);
                if (c == null) {
                    c = new ArrayList<Integer>();
                    rowVals.put(row, c);
                }
                c.add(col);
                List<Integer> r = colVals.get(col);
                if (r == null) {
                    r = new ArrayList<Integer>();
                    colVals.put(col, r);
                }
                r.add(row);
            }
            
            // Convert the object version to integer lists to save space and
            // improve access
            rowToColsCache = new int[rows.get()][0];
            for (Map.Entry<Integer,List<Integer>> e : rowVals.entrySet()) {
                int row = e.getKey();
                List<Integer> cols = e.getValue();
                int[] colArr = new int[cols.size()];
                for (int i = 0; i < cols.size(); ++i)
                    colArr[i] = cols.get(i);
                rowToColsCache[row] = colArr;
                // null out the list for GC to try to cancel out the overhead of
                // having both the array and the list in memory at the same time
                e.setValue(null);
            }
            rowVals = null;
        
            colToRowsCache = new int[cols.get()][0];
            for (Map.Entry<Integer,List<Integer>> e : colVals.entrySet()) {
                int col = e.getKey();
                List<Integer> rows = e.getValue();
                int[] rowArr = new int[rows.size()];
                for (int i = 0; i < rows.size(); ++i)
                    rowArr[i] = rows.get(i);
                colToRowsCache[col] = rowArr;
                // null out the list for GC to try to cancel out the overhead of
                // having both the array and the list in memory at the same time
                e.setValue(null);                
            }
        }
    }

    /**
     * A utility class for holding the row-value pair
     */
    private static class Entry {
        
        final int row;
        final int col;
        public Entry(int row, int col) {
            this.row = row;
            this.col = col;
        }

        public boolean equals(Object o) {
            if (o instanceof Entry) {
                Entry e = (Entry)o;
                return e.row == row && e.col == col;
            }
            return false;
        }

        public int hashCode() {
            return (row << 16) ^ col;
        }
    }
}
