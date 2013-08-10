/*
 * Copyright 2012 David Jurgens
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
 * A tiled view of a {@code Matrix} instance where selected columns of the instance
 * a represented as a single, contiguous matrix.  This effectively creates a
 * {@code Matrix} out of a possibly non-contiguous selection of the columns of the
 * original.  This class is intended to be use when a large matrix has been
 * created and submatrices of the large matrix need to be treated as full {@code
 * Matrix} instances; rather than copy the data, this class provides a way of
 * representing the original data as a partial view.
 *
 * </p>
 *
 * All methods are write-through to the original backing matrix.
 *
 * </p>
 *
 * This matrix recomputes the mapping if the {@link Matrix} being masked is also
 * a {@link ColumnMaskedMatrix}, thus preventing a recursive call to column lookups.
 *
 * @author David Jurgens
 */
public class ColumnMaskedMatrix extends AbstractMatrix 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The matrix that contains the actual data for this instance
     */
    private final Matrix backingMatrix;

    /**
     * The number of columns in this matrix
     */
    private final int columns;

    /**
     * A mapping from the virtual column number to the actual column number in the
     * backing matrix
     */
    private final int[] columnToReal;

    /**
     * Creates a partial view of the provided matrix using the bits set to
     * {@code true} as the columns that should be included
     *
     * @throws IllegalArgumentException if {@code included} has a bit set whose
     *         index is greater than the number of columns present in {@code
     *         matrix}
     */
    public ColumnMaskedMatrix(Matrix matrix, BitSet included) {
        this.backingMatrix = matrix;
        columnToReal = new int[included.cardinality()];
        for (int i = included.nextSetBit(0), column = 0; i >= 0; 
                 i = included.nextSetBit(i+1), column++) {
            if (i >= matrix.columns())
                throw new IllegalArgumentException(
                    "specified column not present in original matrix: " + i);
            columnToReal[column] = i;
        }
        columns = columnToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * set to specify which columns should be included in the matrix.  Note that
     * the ordering of the columns in the set does not matter; columns will be mapped
     * to the respective indices based on the numeric ordering of the values in
     * the set.
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of columns present in
     *         {@code matrix}
     */
    public ColumnMaskedMatrix(Matrix matrix, Set<Integer> included) {
        backingMatrix = matrix;
        columnToReal = new int[included.size()];
        // Sort the column values in included first so the mapping is set up so
        // that to virtual column refers to a real column whose index is less than any
        // lesser virtual column's real column.
        int[] columnArr = new int[included.size()];
        int i = 0;
        for (Integer j : included) {
            if (j < 0 || j >= matrix.columns())
                throw new IllegalArgumentException("Cannot specify a column " +
                    "outside the original matrix dimensions:" + j);
            columnToReal[i++] = j;
        }
        Arrays.sort(columnToReal);
        columns = columnToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * ordered set.  The ordering of the given set is used to determine the
     * order of the columns in the resulting matrix.
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of columns present in
     *         {@code matrix}
     */
    public ColumnMaskedMatrix(Matrix matrix, LinkedHashSet<Integer> included) {
        backingMatrix = matrix;
        columnToReal = new int[included.size()];

        int i = 0;;
        for (Integer j : included) {
            if (j < 0 || j >= matrix.columns())
                throw new IllegalArgumentException("Cannot specify a column " +
                    "outside the original matrix dimensions:" + j);
            columnToReal[i++] = j;
        }
        columns = columnToReal.length;
    }

    /**
     * Creates a partial view of the provided matrix using the integers in the
     * array of indices.  
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of columns present in
     *         {@code matrix}
     */
    public ColumnMaskedMatrix(Matrix matrix, int[] reordering) {
        columnToReal = new int[reordering.length];
        columns = reordering.length;

        // If the given matrix is already a ColumnMaskedMatrix, connect to the
        // inner backing matrix and compute the transitive column ordering mapping.
        // This will prevent a deep nesting of ColumnMaskMatrix lookups when
        // algorithms recursively remap a mapped matrix.
        if (matrix instanceof ColumnMaskedMatrix) {
            ColumnMaskedMatrix rmm = (ColumnMaskedMatrix) matrix;
            this.backingMatrix = rmm.backingMatrix;

            for (int i = 0; i < reordering.length; ++i) {
                int j = reordering[i];
                if (j < 0 || j >= matrix.columns())
                    throw new IllegalArgumentException("Cannot specify a column " +
                    "outside the original matrix dimensions:" + j);

                columnToReal[i] = rmm.columnToReal[j];
            }
        } else {
            backingMatrix = matrix;
            for (int i = 0; i < reordering.length; ++i) {
                int j = reordering[i];
                if (j < 0 || j >= matrix.columns())
                    throw new IllegalArgumentException("Cannot specify a column " +
                    "outside the original matrix dimensions:" + j);
                columnToReal[i] = j;
            }
        }
    }

    /**
     * Returns the column in the backing matrix that the {@code virtualColumn} value
     * is mapped to in the column-masked matrix.
     */
    protected int getRealColumn(int virtualColumn) {
        if (virtualColumn < 0 || virtualColumn >= columns)
            throw new IndexOutOfBoundsException(
                "column out of bounds: " + virtualColumn);
        return columnToReal[virtualColumn];
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return backingMatrix.get(row, getRealColumn(col));
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        double[] arr = new double[columns];
        for (int i = 0; i < columnToReal.length; ++i)
            arr[i] = backingMatrix.get(row, columnToReal[i]);
        return arr;
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
    public double[] getColumn(int column) {
        return backingMatrix.getColumn(getRealColumn(column));
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        return backingMatrix.getColumnVector(getRealColumn(column));
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return backingMatrix.rows();
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] arr = new double[backingMatrix.rows()][columns];
        for (int i = 0; i < columnToReal.length; ++i)
            arr[i] = backingMatrix.getColumn(columnToReal[i]);
        return arr;
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        backingMatrix.set(row, getRealColumn(col), val);        
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] values) {
        if (values.length != columns)
            throw new IllegalArgumentException("cannot set a row " +
                "whose dimensions are different than the matrix");
        for (int i = 0; i < columnToReal.length; ++i)
            backingMatrix.set(row, columnToReal[i], values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        if (values.length() != columns)
            throw new IllegalArgumentException("cannot set a row " +
                "whose dimensions are different than the matrix");
        if (values instanceof SparseVector) {
            SparseVector sv = (SparseVector)values;
            for (int nz : sv.getNonZeroIndices())
                backingMatrix.set(nz, getRealColumn(nz), values.get(nz));
        }
        else {
            for (int i = 0; i < columnToReal.length; ++i)
                backingMatrix.set(i, columnToReal[i], values.get(i));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] columns) {
        backingMatrix.setColumn(getRealColumn(column), columns);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        backingMatrix.setColumn(getRealColumn(column), values);
    }

    /**
     * Returns the {@code backingMatrix} that is being masked.
     */
    public Matrix backingMatrix() {
        return backingMatrix;
    }

    /**
     * Returns the mapping from indices in this {@link ColumnMaskedMatrix} to the
     * real indices in the {@code backingMatrix}.
     */
    public int[] reordering() {
        return columnToReal;
    }
}
