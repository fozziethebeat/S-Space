/*
 * Copyright 2009 Keith Stevens 
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


/**
 * A special-case {@code Matrix} implementation for diagonal matrices. This
 * class provides a memory efficient representation and additional bounds
 * checking to ensure non-diagonal elements cannot be set.
 *
 * @author Keith Stevens 
 */
public class DiagonalMatrix implements SparseMatrix {
        
    /**
     * The number diagonal values in this {@code Matrix}.
     */
    private double[] values;
    
    /**
     * Creates a new {@code DiagonalMatrix} with {@code numValues} rows and
     * columns.
     *
     * @param numValues The number of rows, columns, and diagonals in this
     *                  {@code DiagonalMatrix}.
     */
    public DiagonalMatrix(int numValues) {
        values = new double[numValues];
    }

    /**
     * Creates a new {@code DiagonalMatrix} with {@code newValues} as the
     * diagonal values.
     *
     * @param newValues The values to use as the diagonals of this {@code
     *                  Matrix}.
     */
    public DiagonalMatrix(double[] newValues) {
        values = new double[newValues.length];
        for (int i = 0; i < values.length; ++i)
            values[i] = newValues[i];
    }

    /**
     * Checks that the given row and column values are non-negative, and less
     * than the number of diagonals in this {@code DiagonalMatrix}.
     *
     * @param row The row index to check.
     * @param col The col index to check.
     *
     * @throws IllegalArgumentException if either index is invalid.
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0 || row >= values.length || col >= values.length)
            throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        checkIndices(row, col);

        if (row == col)
            return values[row];
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        checkIndices(0, column);

        double[] columnValues = new double[values.length];
        columnValues[column] = values[column];
        return columnValues;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int column) {
        checkIndices(0, column);
        SparseDoubleVector columnValues =
            new SparseHashDoubleVector(values.length);
        columnValues.set(column, values[column]);
        return columnValues;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        checkIndices(row, 0);

        double[] returnRow = new double[values.length];
        returnRow[row] = values[row];
        return returnRow;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        checkIndices(row, 0);
        SparseDoubleVector vector = new SparseHashDoubleVector(values.length);
        vector.set(row, values[row]);
        return vector;
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return values.length;
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if {@code row != col}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);

        if (row != col) {
            throw new IllegalArgumentException(
                    "cannot set non-diagonal elements in a DiagonalMatrix");
        }
        values[row] = val;
    }

    /**
     * {@inheritDoc}
     *
     * Note that any values are not on the diagonal are ignored.
     */
    public void setColumn(int column, double[] values) {
        checkIndices(values.length - 1, column);

        values[column] = values[column];
    }
    
    /**
     * {@inheritDoc}
     *
     * Note that any values are not on the diagonal are ignored.
     */
    public void setColumn(int column, DoubleVector vector) {
        checkIndices(vector.length() - 1, column);

        values[column] = vector.get(column);
    }

    /**
     * {@inheritDoc}
     *
     * Note that any values are not on the diagonal are ignored.
     */
    public void setRow(int row, double[] values) {
        checkIndices(row, values.length - 1);

        values[row] = values[row];
    }
    
    /**
     * {@inheritDoc}
     *
     * Note that any values are not on the diagonal are ignored.
     */
    public void setRow(int row, DoubleVector vector) {
        checkIndices(row, vector.length() - 1);

        values[row] = vector.get(row);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[values.length][values.length];
        for (int r = 0; r < values.length; ++r) {
            m[r][r] = values[r];
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return values.length;
    }
}
