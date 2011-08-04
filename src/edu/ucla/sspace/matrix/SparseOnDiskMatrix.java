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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.DoubleBuffer;

import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.util.WeakHashMap;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A {@code SparseMatrix} implementation that uses a binary file to read and
 * write. The matrix is still stored in a dense row-column order on disk, so
 * in-order column accesses to elments in a row will perform much better than
 * sequential row accesses to the same column.  However calls to {@link
 * #getRowVector(int) getRowVector} and {@link #getColumnVector(int)
 * getColumnVector} will return {@link SparseVector} instances.  This class is
 * intended for large matrices that need to be on disk due to their dimensions,
 * but whose data is mostly sparse <p>
 * 
 * The {@link DoubleVector} representations returned reflect a snapshot of the state
 * of the matrix at the time of access.  Subsequent updates to the matrix will
 * not be reflected in these vectors, nor will changes to the vector be
 * propagated to the matrix.
 *
 * <p>
 *
 * If a {@link IOException} is ever raised as a part of executing an the methods
 * of an instance, the exception is rethrown as a {@link IOError}.
 *
 * @author David Jurgens
 */
public class SparseOnDiskMatrix extends OnDiskMatrix implements SparseMatrix {

    /**
     * A weak mapping from the row index to a {@link SparseVector} that was
     * created for that row.
     */
    private final Map<Integer,VersionedVector> rowToVectorCache;

    /**
     * A weak mapping from the column index to a {@link SparseVector} that was
     * created for that column.
     */
    private final Map<Integer,VersionedVector> colToVectorCache;
    
    /**
     * A counter to keep track of any modifications to this matrix.  Any
     * modification causes this counter to increment.
     *
     * @see VersionedVector
     */
    private final AtomicInteger version;

    /**
     * Create a matrix of the provided size using a temporary file.
     *
     * @throws IOError if the backing file for this matrix cannot be created
     */
    public SparseOnDiskMatrix(int rows, int cols) {        
        super(rows, cols);
        version = new AtomicInteger(0);
        rowToVectorCache = new WeakHashMap<Integer,VersionedVector>();
        colToVectorCache = new WeakHashMap<Integer,VersionedVector>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SparseDoubleVector getColumnVector(int column) {
        // Check whether we have this column cached
        VersionedVector cachedCol = colToVectorCache.get(column);
        // If the cache was empty or if the matrix has been updated since this
        // vector was created, recreate the vector and cache it before returning
        if (cachedCol == null || cachedCol.version != version.get()) {
            cachedCol = new VersionedVector(rows(), version.get());
            double[] col = getColumn(column);
            for (int i = 0; i < col.length; ++i) {
                double d = col[i];
                if (d != 0)
                    cachedCol.set(i, d);
            }
            colToVectorCache.put(column, cachedCol);
        }
        return cachedCol;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SparseDoubleVector getRowVector(int row) {
        // Check whether we have this row cached
        VersionedVector cachedRow = colToVectorCache.get(row);
        // If the cache was empty or if the matrix has been updated since this
        // vector was created, recreate the vector and cache it before returning
        if (cachedRow == null || cachedRow.version != version.get()) {
            cachedRow = new VersionedVector(columns(), version.get());
            double[] r = getRow(row);
            for (int i = 0; i < r.length; ++i) {
                double d = r[i];
                if (d != 0)
                    cachedRow.set(i, d);
            }
            rowToVectorCache.put(row, cachedRow);
        }
        return cachedRow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(int row, int col, double val) {
        super.set(row, col, val);
        version.incrementAndGet();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setColumn(int column, double[] values) {
        super.setColumn(column, values);
        version.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColumn(int column, DoubleVector values) {
        super.setColumn(column, values);
        version.incrementAndGet();        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRow(int row, double[] vals) {
        super.setRow(row, vals);
        version.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRow(int row, DoubleVector values) {
        super.setRow(row, values);
        version.incrementAndGet();
    }

    /**
     * A {@code DoubleVector} instance that keeps track of a versioned state.
     * This class is intended to mark when the instance was created to enable
     * checked whether its data might be inconsistent with the matrix from which
     * it was generated.
     */
    private static class VersionedVector extends SparseHashDoubleVector {

        private static final long serialVersionUID = 1L;

        private final int version;

        public VersionedVector(int length, int version) {
            super(length);
            this.version = version;
        }
    }    
}
