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

import edu.ucla.sspace.vector.AbstractDoubleVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.io.Serializable;

import java.util.Arrays;


/**
 * A {@code Matrix} backed by an array.  The matrix is represented in
 * row-striped format, so row-based access will have better performance.
 *
 * <p> The {@link DoubleVector} views of this matrix are back by data in the
 * array so changes to the vectors will be reflected in the matrix and vice
 * versa.
 *
 * @author David Jurgens
 */
public class ArrayMatrix extends AbstractMatrix implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * The number of rows stored in this {@code ArrayMatrix}.
     */
    private final int rows;

    /**
     * The number of columns stored in this {@code ArrayMatrix}.
     */
    private final int cols;
    
    /**
     * A single dimensionaly array storing the values of this {@code
     * ArrayMatrix}.
     */
    private final double[] matrix;

    /**
     * Create a {@code ArrayMatrix} of size {@code rows} by {@code cols}.
     *
     * @param rows The number of rows this {@code ArrayMatrix} will represent.
     * @param cols The number of columns this {@code ArrayMatrix} will
     *             represent.
     */
    public ArrayMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        matrix = new double[rows*cols];
    }

    /**
     * Create a {@code ArrayMatrix} from a two dimensional array.
     *
     * @param matrix2d The two dimensional array this {@code ArrayMatrix} will
    *                represent.
     * @throws IllegalArgumentExceptiona if matrix2d is invalid.
     */
    public ArrayMatrix(double[][] matrix2d) {
      if (matrix2d == null)
          throw new IllegalArgumentException("invalid matrix dimensions");

      rows = matrix2d.length;
      if (rows < 1)
          throw new IllegalArgumentException("invalid matrix dimensions");

      cols = matrix2d[0].length;
      if (cols < 1)
          throw new IllegalArgumentException("invalid matrix dimensions");

      matrix = new double[rows*cols];
      for (int i = 0; i < rows; ++i) {
        if (cols != matrix2d[i].length)
            throw new IllegalArgumentException("invalid matrix dimensions");
        for (int j = 0; j < cols; ++j)
            set(i, j, matrix2d[i][j]);
      }
    }

    /**
     * Create a new {@code ArrayMatrix} which is of dimensions {@code rows} by
     * {@code cols}, and takes ownership of {@code matrix1d} as the values of
     * this {@code ArrayMatrix}.  Note that {@code matrix1D} should be of length
     * {@code rows} * {@code cols}.
     *
     * @param rows The number of rows this {@code ArrayMatrix} will represent.
     * @param cols The number of columns this {@code ArrayMatrix} will
     *             represent.
     * @param matrix1D A 1 dimensional representation of the vaues this {@code
     *                 ArrayMatrix} will have.
     * @throws IllegalArgumentException if either {@code rows} or {@code cols}
     *                                  is negative, or matrix1d is invalid.
     */
    public ArrayMatrix(int rows, int cols, double[] matrix1D) {
        this.rows = rows;
        this.cols = cols;
        if (rows < 1 || cols < 1)
            throw new IllegalArgumentException("invalid matrix dimensions");
        if (matrix1D == null)
            throw new NullPointerException("provided matrix cannot be null");
        if (matrix1D.length != (rows * cols))
            throw new IllegalArgumentException("provided matrix is wrong size");

        matrix = matrix1D;
    }
    
    /**
     * Check that the indices of a requested cell are within bounds.
     *
     * @param row The row of the cell to check.
     * @param col The column of the cell to check.
     *
     * @throws ArrayIndexOutOfBoundsException if 
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
        checkIndices(row, col);

        int index = getIndex(row, col);
        return matrix[index];
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        checkIndices(0, column);

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
        checkIndices(row, 0);

        double[] rowArr = new double[cols];
        int index = getIndex(row, 0);
        for (int i = 0; i < cols; ++i)
            rowArr[i] = matrix[index++];
        return rowArr;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        checkIndices(row, 0);
        return new RowVector(getIndex(row, 0));
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }
 
    /**
     * Returns the one-dimension index in the matrix for the row and column.
     */
    private int getIndex(int row, int col) {
        return (row * cols) + col;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);

        int index = getIndex(row, col);
        matrix[index] = val;
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        checkIndices(values.length - 1, column);

        for (int row = 0; row < rows; ++row)
            matrix[getIndex(row,column)] = values[column];
    }
    
    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        checkIndices(values.length() - 1, column);

        for (int row = 0; row < rows; ++row)
            matrix[getIndex(row,column)] = values.get(row);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        checkIndices(row, columns.length - 1);

        for (int col = 0; col < cols; ++col)
            matrix[getIndex(row,col)] = columns[col];
    }
    
    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        checkIndices(row, values.length() - 1);

        for (int col = 0; col < cols; ++col)
            matrix[getIndex(row,col)] = values.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        int i = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col)
                m[row][col] = matrix[i++];
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }
    
    /**
     * A matrix-internal view of a row vector that exposes is contents as a
     * {@link DoubleVector} without duplicating the data
     */
    class RowVector extends AbstractDoubleVector implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The index into the {@linke ArrayMatrix#matrix} array where the data
         * for this row is stored
         */
        final int startIndex;

        /**
         * The magnitude of this vector, cached for better performance.
         */
        double magnitude;

        public RowVector(int startIndex) {
            this.startIndex = startIndex;
            magnitude = -1;
        }

        /**
         * Maps the index of this vector an index in the matrix or throws an
         * {@link IndexOutOfBoundsException} of the vector index is outside of
         * its dimensions.
         */
        private int toMatrixIndex(int index) {
            int i = startIndex + index;
            if (i < 0 || i >= startIndex + cols)
                throw new IndexOutOfBoundsException(
                    index + " outside vector bounds");
            return i;
        }

        /**
         * {@inheritDoc}
         */
        public double add(int index, double delta) {
            int i = toMatrixIndex(index);
            magnitude = -1;
            matrix[i] += delta;
            return matrix[i];
        }     

        /**
         * {@inheritDoc}
         */
        public void set(int index, double value) {
            int i = toMatrixIndex(index);
            magnitude = -1;
            matrix[i] = value;
        }

        /**
         * {@inheritDoc}
         */
        public void set(int index, Number value) {
            set(index, value.doubleValue());
        }

        /**
         * {@inheritDoc}
         */
        public double get(int index) {
            int i = toMatrixIndex(index);
            return matrix[i];
        }

        /**
         * {@inheritDoc}
         */
        public Double getValue(int index) {
            return get(index);
        }

        /**
         * {@inheritDoc}
         */
        public double magnitude() {
            if (magnitude < 0) {
                double m = 0;
                for (int i = startIndex; i < startIndex + cols; ++i) {
                    double d = matrix[i];
                    m += d * d;
                }
                magnitude = Math.sqrt(m);
            }
            return magnitude;
        }

        /**
         * {@inheritDoc}
         */
        public double[] toArray() {
            return Arrays.copyOfRange(matrix, startIndex, startIndex + cols);
        }

        /**
         * {@inheritDoc}
         */
        public int length() {
            return cols;
        }
    }
}
