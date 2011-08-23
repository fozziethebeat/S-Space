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

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
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


/**
 * A Matrix implementation that uses a binary file to read and write Returns a
 * copy of the specified rowvalues of the matrix.  The matrix is stored in
 * row-column order on disk, so in-order column accesses to elments in a row
 * will perform much better than sequential row accesses to the same column.
 *
 * <p>
 *
 * If a {@link IOException} is ever raised as a part of executing an the methods
 * of an instance, the exception is rethrown as a {@link IOError}.
 *
 * @author David Jurgens
 */
public class OnDiskMatrix implements Matrix {
    
    /**
     * The number of bytes in a double.
     */
    private static final int BYTES_PER_DOUBLE = 8;

    private static final int MAX_ELEMENTS_PER_REGION = 
        Integer.MAX_VALUE / BYTES_PER_DOUBLE;

    /**
     * The on-disk storage space for the matrix
     */
    //private final RandomAccessFile matrix; 
    private final DoubleBuffer[] matrixRegions;

    /**
     * The {@code File} instances that back the matrix regions
     */
    private final File[] backingFiles;

    /**
     * The number of rows stored in this {@code Matrix}.
     */
    private final int rows;

    /**
     * The number of columns stored in this {@code Matrix}.
     */
    private final int cols;

    /**
     * Create a matrix of the provided size using a temporary file.
     *
     * @throws IOError if the backing file for this matrix cannot be created
     */
    public OnDiskMatrix(int rows, int cols) {

        if (rows <= 0 || cols <= 0) 
            throw new IllegalArgumentException("dimensions must be positive");
        
        this.rows = rows;
        this.cols = cols;
            
        // Determine how big the array will need to be

        // Note that to map the array into memory, we have to avoid the case
        // where any mapped part of the array is larger than Integer.MAX_VALUE.
        // Therefore, divide the array up into regions less than this size.
        int numRegions = 
            (int)(((long)rows * cols) / MAX_ELEMENTS_PER_REGION) + 1;
        matrixRegions = new DoubleBuffer[numRegions];
        backingFiles = new File[numRegions];
        for (int region = 0; region < numRegions; ++region) {
            int sizeInBytes = (region + 1 == numRegions) 
                ? (int)((((long)rows * cols) 
                         % MAX_ELEMENTS_PER_REGION) * BYTES_PER_DOUBLE)
                : MAX_ELEMENTS_PER_REGION * BYTES_PER_DOUBLE;
            Duple<DoubleBuffer,File> d = createTempBuffer(sizeInBytes);
            matrixRegions[region] = d.x;
            backingFiles[region] = d.y;
        }
     }

    /**
     *
     * @param size the size of the buffer in bytes
     */
    private static Duple<DoubleBuffer,File> createTempBuffer(int size) {
        try {
            File f = File.createTempFile("OnDiskMatrix",".matrix");
            // Make sure the temp file goes away since it can get fairly large
            // for big matrices
            f.deleteOnExit();
            RandomAccessFile raf = new RandomAccessFile(f, "rw");
            FileChannel fc = raf.getChannel();
            DoubleBuffer contextBuffer = 
                fc.map(MapMode.READ_WRITE, 0, size).asDoubleBuffer();
            fc.close();
            return new Duple<DoubleBuffer,File>(contextBuffer, f);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Checks that the indices are within the bounds of the matrix and throws an
     * exception if they are not.
     */
    private void checkIndices(int row, int col) {
        if (row < 0 || row >= rows)
            throw new ArrayIndexOutOfBoundsException("row: " + row);
        else if (col < 0 || col >= cols)
            throw new ArrayIndexOutOfBoundsException("column: " + col);
    }
    
    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        int region = getMatrixRegion(row, col);
        int regionOffset = getRegionOffset(row, col);
        return matrixRegions[region].get(regionOffset);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        double[] values = new double[rows];
        for (int row = 0; row < rows; ++row)
            values[row] = get(row, column);
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        return new DenseVector(getColumn(column));
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        int rowStartRegion = getMatrixRegion(row, 0);
        int rowEndRegion = getMatrixRegion(row + 1, 0);
        double[] rowVal = new double[cols];
        if (rowStartRegion == rowEndRegion) {
            int rowStartIndex = getRegionOffset(row, 0);
            DoubleBuffer region = matrixRegions[rowStartRegion];
            for (int col = 0; col < cols; ++col)
                rowVal[col] = region.get(col + rowStartIndex);
        }
        else {
            DoubleBuffer firstRegion = matrixRegions[rowStartRegion];
            DoubleBuffer secondRegion = matrixRegions[rowEndRegion];
            int rowStartIndex = getRegionOffset(row, 0);
            int rowOffset = 0;
            for (; rowStartIndex + rowOffset < MAX_ELEMENTS_PER_REGION; 
                     ++rowOffset) {
                rowVal[rowOffset] = firstRegion.get(rowOffset + rowStartIndex);
            }
            // Fill from the second region
            for (int i = 0; rowOffset < rowVal.length; ++i, ++rowOffset)
                rowVal[rowOffset] = secondRegion.get(i);
        }
        return rowVal;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return new DenseVector(getRow(row));
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }

    private int getMatrixRegion(long row, long col) {
        long element = row * cols + col;
        return (int)(element / MAX_ELEMENTS_PER_REGION);
    }

    private int getRegionOffset(long row, long col) {
        long element = row * cols + col;
        return (int)(element % MAX_ELEMENTS_PER_REGION);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        int region = getMatrixRegion(row, col);
        int regionOffset = getRegionOffset(row, col);
        matrixRegions[region].put(regionOffset, val);
    }
    
    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        for (int row = 0; row < rows; ++row)
            set(row, column, values[row]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        for (int row = 0; row < rows; ++row)
            set(row, column, values.get(row));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] vals) {
        if (vals.length != cols)
            throw new IllegalArgumentException(
                "The number of values does not match the number of columns");
        for (int i = 0; i < vals.length; ++i)
            set(row, i, vals[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        if (values.length() != cols)
            throw new IllegalArgumentException(
                "The number of values does not match the number of columns");

        if (values instanceof SparseVector) {
            SparseVector sv = (SparseVector)values;
            for (int i : sv.getNonZeroIndices())
                set(row, i, values.get(i));
        }
        else {
            for (int i = 0; i < values.length(); ++i)
                set(row, i, values.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        if (matrixRegions.length > 1)
            throw new UnsupportedOperationException(
                "matrix is too large to fit into memory");
        double[][] m = new double[rows][cols];
        DoubleBuffer b = matrixRegions[0];
        b.rewind();
        for (int row = 0; row < rows; ++row) 
            b.get(m[row]);
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }

    /**
     * Upon finalize, deletes all of the backing files.  This is most necessary
     * when the JVM is long-running with many {@code OnDiskMatrix} instances
     * that are not deleted until exit.
     */
    @Override protected void finalize() { 
        // Delete all of the backing files, silently catching all errors
        for (File f : backingFiles) {           
            try {
                f.delete();
            } catch (Throwable t) {
                // silent
            }
        }
    }
}
