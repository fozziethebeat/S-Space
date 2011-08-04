/*
 * Copyright 2009 David Jurgens 
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

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.AtomicVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.Arrays;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A concurrent, thread-safe, growable {@code Matrix} class.  This class allows
 * multiple threads to operate on the same matrix where all methods are
 * concurrent to the fullest extent possible.
 *
 * @author David Jurgens
 */
public class AtomicGrowingMatrix implements AtomicMatrix {
    
    /**
     * The read lock for reading rows from this {@code AtomicGrowingMatrix}.
     */
    private Lock rowReadLock;

    /**
     * The write lock for adding rows to this {@code AtomicGrowingMatrix}.
     */
    private Lock rowWriteLock;

    /**
     * The read lock for reading from the internal rows.
     */
    private Lock denseArrayReadLock;

    /**
     * The write lock for writing to internal rows.
     */
    private Lock denseArrayWriteLock;

    /**
     * The number of rows represented in this {@code AtomicGrowingMatrix}.
     */
    private AtomicInteger rows;

    /**
     * The number of columns represented in this {@code AtomicGrowingMatrix}.
     */
    private AtomicInteger cols;
  
    /**
     * Each row is defined as a {@link AtomicVector} which does most of the
     * work.
     */
    private final Map<Integer, AtomicVector> sparseMatrix;

    /**
     * Create an {@code AtomicGrowingMatrix} with 0 rows and 0 columns.
     */
    public AtomicGrowingMatrix() {
        this.rows = new AtomicInteger(0);
        this.cols = new AtomicInteger(0);
        sparseMatrix = new IntegerMap<AtomicVector>();
        
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        rowReadLock = rwLock.readLock();
        rowWriteLock = rwLock.writeLock();

        rwLock = new ReentrantReadWriteLock();
        denseArrayReadLock = rwLock.readLock();
        denseArrayWriteLock = rwLock.writeLock();
    }
    
    /**
     * {@inheritDoc}
     */
    public double addAndGet(int row, int col, double delta) {
        checkIndices(row, col);
        AtomicVector rowEntry = getRow(row, col, true);
        return rowEntry.addAndGet(col, delta);    
    }

    /**
     * Verify that the given row and column value is non-negative
     *
     * @param row The row index to check.
     * @param the The column index to check.
     */    
    private void checkIndices(int row, int col) {
         if (row < 0 || col < 0) {
             throw new ArrayIndexOutOfBoundsException();
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
        checkIndices(row, col);
        AtomicVector rowEntry = getRow(row, col, false);
        return (rowEntry == null) ? 0d : rowEntry.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double getAndAdd(int row, int col, double delta) {
        checkIndices(row, col);
        AtomicVector rowEntry = getRow(row, col, true);
        return rowEntry.getAndAdd(col, delta);
    }


    /**
     * {@inheritDoc} The length of the returned column reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #rows()}
     */
    public double[] getColumn(int column) {
        checkIndices(0, column);
        rowReadLock.lock();
        double[] values = new double[rows.get()];
        for (int row = 0; row < rows.get(); ++row)
            values[row] = get(row, column);
        rowReadLock.unlock();
        return values;
    }

    /**
     * {@inheritDoc} The length of the returned row vector reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #rows()}
     */
    public DoubleVector getColumnVector(int column) {
        checkIndices(0, column);
        rowReadLock.lock();
        DoubleVector values = new DenseVector(rows.get());
        for (int row = 0; row < rows.get(); ++row) {
            double value = get(row, column);
            if (value != 0d)
                values.set(row, value);
        }
        rowReadLock.unlock();
        return values;
    }

    /**
     * Returns an immutable view of the columns's data as a non-atomic vector,
     * which may present an inconsistent view of the data if this matrix is
     * being concurrently modified.  This method should only be used in special
     * cases where the vector is being accessed at a time when the matrix (or
     * this particular row) will not be modified.
     */
    public DoubleVector getColumnVectorUnsafe(int column) {
        checkIndices(0, column);
        DoubleVector values = new DenseVector(rows.get());
        for (int row = 0; row < rows.get(); ++row) {
            AtomicVector rowEntry = getRow(row, -1, false);            
            double value = 0;
            if (rowEntry != null && (value = rowEntry.get(column)) != 0)
                values.set(row, value);
        }
        return values;
    }

    /**
     * {@inheritDoc} The length of the returned row reflects the size of matrix
     * at the time of the call, which may be different from earlier calls to
     * {@link #columns()}.
     */
    public double[] getRow(int row) {
        checkIndices(row, 0);
        AtomicVector rowEntry = getRow(row, -1, false);
        return (rowEntry == null)
            ? new double[cols.get()]
            : toArray(rowEntry, cols.get());
    }

    /**
     * Gets the {@code AtomicVector} associated with the index, or {@code null}
     * if no row entry is present, or if {@code createIfAbsent} is {@code true},
     * creates the missing row and returns that.
     *
     * @param row the row to get
     * @param col the column in the row that will be accessed or {@code -1} if
     *        the entire row is needed.  This value is only used to resize the
     *        matrix dimensions if the row is to be created.
     * @param createIfAbsent {@true} if a row that is requested but not present
     *        should be created
     *
     * @return the row at the entry or {@code null} if the row is not present
     *         and it was not to be created if absent
     */
    private AtomicVector getRow(int row, int col, boolean createIfAbsent) {
        rowReadLock.lock();
        AtomicVector rowEntry = sparseMatrix.get(row);
        rowReadLock.unlock();
         
        // If no row existed, create one
        if (rowEntry == null && createIfAbsent) {
            rowWriteLock.lock();
            // ensure that another thread has not already added this row while
            // this thread was waiting on the lock
            rowEntry = sparseMatrix.get(row);
            if (rowEntry == null) {
                rowEntry = new AtomicVector(new CompactSparseVector());

                // update the bounds as necessary
                if (row >= rows.get()) {
                    rows.set(row + 1);
                }
                if (col >= cols.get()) {
                    cols.set(col + 1);
                }
                sparseMatrix.put(row, rowEntry);
            }
            rowWriteLock.unlock();
        }
        return rowEntry;
    }

    /**
     * {@inheritDoc} The length of the returned row vector reflects the size of
     * matrix at the time of the call, which may be different from earlier calls
     * to {@link #columns()}.
     */
    public DoubleVector getRowVector(int row) {
        DoubleVector v = getRow(row, -1, false);
        // If no row was currently assigned in the matrix, then return an empty
        // vector in its place.  Otherwise, return a view on top of the vector
        // with its current length
        return (v == null) 
            ? new CompactSparseVector(cols.get())
            : Vectors.subview(v, 0, cols.get());
    }

    /**
     * Returns an immutable view of the row's data as a non-atomic vector, which
     * may present an inconsistent view of the data if this matrix is being
     * concurrently modified.  This method should only be used in special cases
     * where the vector is being accessed at a time when the matrix (or this
     * particular row) will not be modified.
     *
     * @param row the row whose values should be returned
     *
     * @return an unsafe, non-atomic view of the row's data
     */
    public DoubleVector getRowVectorUnsafe(int row) {
        AtomicVector rowEntry = sparseMatrix.get(row);
        return (rowEntry == null)
            ? new CompactSparseVector(cols.get())
            : Vectors.immutable(Vectors.subview(rowEntry.getVector(), 
                                                0, cols.get()));
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
        checkIndices(row, col);

        AtomicVector rowEntry = getRow(row, col, true);
        denseArrayReadLock.lock();
        rowEntry.set(col, val);
        denseArrayReadLock.unlock();
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        checkIndices(0, column);
        for (int row = 0; row < rows.get(); ++row)
            set(row, column, values[row]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        checkIndices(0, column);
        for (int row = 0; row < rows.get(); ++row)
            set(row, column, values.get(row));
    }
  
    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        checkIndices(row, 0);
        AtomicVector rowEntry = getRow(row, columns.length - 1, true);
        denseArrayReadLock.lock();
        for (int i = 0; i < columns.length; ++i)
            rowEntry.set(i, columns[i]);
        denseArrayReadLock.unlock();
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        checkIndices(row, 0);
        AtomicVector rowEntry = getRow(row, values.length() - 1, true);
        denseArrayReadLock.lock();
        Vectors.copy(rowEntry, values);
        denseArrayReadLock.unlock();
    }
  
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        // Grab the write lock to prevent any new rows from being updated
        rowWriteLock.lock();
        // Then grab the whole matrix lock to prevent any values from being set
        // while this method converts the rows into arrays.
        denseArrayWriteLock.lock();
        int c = cols.get();
        double[][] m = new double[rows.get()][c];
        for (Map.Entry<Integer, AtomicVector> e : sparseMatrix.entrySet()) {
            m[e.getKey()] = toArray(e.getValue(), c);
        }
        denseArrayWriteLock.unlock();
        rowWriteLock.unlock();
        return m;
    }

    /**
     * Returns an array of the specified length using the data in the provided
     * vector.  This method allows row vectors to be converted to arrays based
     * on the size of the matrix at the time of the call, thereby prevent
     * changes in length due to external vector modifications.
     */
    private static double[] toArray(DoubleVector v, int length) {
        double[] arr = new double[length];
        for (int i = 0; i < arr.length; ++i) {
            arr[i] = v.get(i);
        }
        return arr;
    }
}
