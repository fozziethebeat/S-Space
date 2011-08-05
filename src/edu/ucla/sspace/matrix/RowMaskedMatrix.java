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

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseVector;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * A tiled view of a {@code Matrix} instance where selected rows of the instance
 * a represented as a single, contiguous matrix.  This effectively creates a
 * {@code Matrix} out of a possibly non-contiguous selection of the rows of the
 * original.  This class is intended to be use when a large matrix has been
 * created and submatrices of the large matrix need to be treated as full {@code
 * Matrix} instances; rather than copy the data, this class provides a way of
 * representing the original data as a partial view.<p>
 *
 * All methods are write-through to the original backing matrix.
 *
 * @author David Jurgens
 */
public class RowMaskedMatrix implements Matrix {

    /**
     * The matrix that contains the actual data for this instance
     */
    private final Matrix backingMatrix;

    /**
     * The number of rows in this matrix
     */
    private final int rows;

    /**
     * A mapping from the virtual row number to the actual row number in the
     * backing matrix
     */
    private final int[] rowToReal;

    /**
     * Creates a partial view of the provided matrix using the bits set to
     * {@code true} as the rows that should be included
     *
     * @throws IllegalArgumentException if {@code included} has a bit set whose
     *         index is greater than the number of rows present in {@code
     *         matrix}
     */
    public RowMaskedMatrix(Matrix matrix, BitSet included) {
        this.backingMatrix = matrix;
        rowToReal = new int[included.cardinality()];
        for (int i = included.nextSetBit(0), row = 0; i >= 0; 
                 i = included.nextSetBit(i+1), row++) {
            if (i >= matrix.rows())
                throw new IllegalArgumentException(
                    "specified row not present in original matrix: " + i);
            rowToReal[row] = i;
        }
        rows = rowToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * set to specify which rows should be included in the matrix.  Note that
     * the ordering of the rows in the set does not matter; rows will be mapped
     * to the respective indices based on the numeric ordering of the values in
     * the set.
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of rows present in
     *         {@code matrix}
     */
    public RowMaskedMatrix(Matrix matrix, Set<Integer> included) {
        backingMatrix = matrix;
        rowToReal = new int[included.size()];
        // Sort the row values in included first so the mapping is set up so
        // that to virtual row refers to a real row whose index is less than any
        // lesser virtual row's real row.
        int[] rowArr = new int[included.size()];
        int i = 0;
        for (Integer j : included) {
            if (j < 0 || j >= matrix.rows())
                throw new IllegalArgumentException("Cannot specify a row " +
                    "outside the original matrix dimensions:" + j);
            rowToReal[i++] = j;
        }
        Arrays.sort(rowToReal);
        rows = rowToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * ordered set.  The ordering of the given set is used to determine the
     * order of the rows in the resulting matrix.
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of rows present in
     *         {@code matrix}
     */
    public RowMaskedMatrix(Matrix matrix, LinkedHashSet<Integer> included) {
        backingMatrix = matrix;
        rowToReal = new int[included.size()];

        int i = 0;;
        for (Integer j : included) {
            if (j < 0 || j >= matrix.rows())
                throw new IllegalArgumentException("Cannot specify a row " +
                    "outside the original matrix dimensions:" + j);
            rowToReal[i++] = j;
        }
        rows = rowToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * array of indices.  
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of rows present in
     *         {@code matrix}
     */
    public RowMaskedMatrix(Matrix matrix, int[] reordering) {
        backingMatrix = matrix;
        rowToReal = new int[reordering.length];

        for (int i = 0; i < reordering.length; ++i) {
            int j = reordering[i];
            if (j < 0 || j >= matrix.rows())
                throw new IllegalArgumentException("Cannot specify a row " +
                    "outside the original matrix dimensions:" + j);
            rowToReal[i] = j;
        }
        rows = rowToReal.length;
    }

    /**
     * Returns the row in the backing matrix that the {@code virtualRow} value
     * is mapped to in the row-masked matrix.
     */
    protected int getRealRow(int virtualRow) {
        if (virtualRow < 0 || virtualRow >= rows)
            throw new IndexOutOfBoundsException(
                "row out of bounds: " + virtualRow);
        return rowToReal[virtualRow];
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return backingMatrix.get(getRealRow(row), col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        double[] col = new double[rows];
        for (int i = 0; i < rowToReal.length; ++i)
            col[i] = backingMatrix.get(rowToReal[i], column);
        return col;
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
        return backingMatrix.getRow(getRealRow(row));
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return backingMatrix.getRowVector(getRealRow(row));
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return backingMatrix.columns();
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] arr = new double[rows][backingMatrix.columns()];
        for (int i = 0; i < rowToReal.length; ++i)
            arr[i] = backingMatrix.getRow(rowToReal[i]);
        return arr;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        backingMatrix.set(getRealRow(row), col, val);        
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        if (values.length != rows)
            throw new IllegalArgumentException("cannot set a column " +
                "whose dimensions are different than the matrix");
        for (int i = 0; i < rowToReal.length; ++i)
            backingMatrix.set(rowToReal[i], column, values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        if (values.length() != rows)
            throw new IllegalArgumentException("cannot set a column " +
                "whose dimensions are different than the matrix");
        if (values instanceof SparseVector) {
            SparseVector sv = (SparseVector)values;
            for (int nz : sv.getNonZeroIndices())
                backingMatrix.set(getRealRow(nz), nz, values.get(nz));
        }
        else {
            for (int i = 0; i < rowToReal.length; ++i)
                backingMatrix.set(rowToReal[i], i, values.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        backingMatrix.setRow(getRealRow(row), columns);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        backingMatrix.setRow(getRealRow(row), values);
    }
}
