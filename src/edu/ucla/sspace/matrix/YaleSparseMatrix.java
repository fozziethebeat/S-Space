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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.Vectors;


/**
 * A sparse {@code Matrix} based on the Yale Sparse Matrix Format, as
 * implemented in {@link CompactSparseVector}.  Each row is allocated a pair of
 * arrays which keeps the non-zero column values in column order.  Lookups are
 * O(log n) where n is the number of non-zero values for the largest row.  The
 * size of this matrix is fixed, and attempts to access rows or columns beyond
 * the size will throw an {@link IndexOutOfBoundsException}.
 *
 * @author David Jurgens
 */
public class YaleSparseMatrix implements SparseMatrix {

    /**
     * The number of rows contained in this {@code SparseMatrix}.
     */
    private final int rows;

    /**
     * The number of columns contained in this {@code SparseMatrix}.
     */
    private final int cols;

    /**
     * Each row is defined as a {@link CompactSparseVector} which does most of
     * the work.
     */
    private final CompactSparseVector[] sparseMatrix;

    /**
     * Constructs a sparse matrix with the specified dimensions.
     */
    public YaleSparseMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        sparseMatrix = new CompactSparseVector[rows];
        for (int i = 0; i < rows; ++i)
            sparseMatrix[i] = new CompactSparseVector(cols);
    }

    /**
     * Constructs a sparse matrix with the non zero values from the given two
     * dimensional array.
     */
    public YaleSparseMatrix(double[][] values) {
        this.rows = values.length;
        this.cols = values[0].length;

        sparseMatrix = new CompactSparseVector[rows];
        for (int r = 0; r < rows; ++r) {
            sparseMatrix[r] = new CompactSparseVector(cols);
            for (int c = 0; c < cols; ++c)
                if (values[r][c] != 0d)
                    sparseMatrix[r].set(c, values[r][c]);
        }


    }

    /**
     * Checks that the indices are within the bounds of this matrix or throws an
     * {@link IndexOutOfBoundsException} if not.
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0 || row >= rows || col >= cols) {
            throw new IndexOutOfBoundsException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        checkIndices(row, col);
        return sparseMatrix[row].get(col);
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
    public SparseDoubleVector getColumnVector(int column) {
        SparseDoubleVector values = new SparseHashDoubleVector(rows);
        for (int row = 0; row < rows; ++row)
            values.set(row, get(row, column));
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return sparseMatrix[row].toArray();
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        return Vectors.immutable(sparseMatrix[row]);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);
        sparseMatrix[row].set(col, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        if (values.length != rows) {
            throw new IllegalArgumentException(
                    "invalid number of rows: " + values.length);
        }
        for (int row = 0; row < rows; ++row)
            set(row, column, values[row]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        if (values.length() != rows) {
            throw new IllegalArgumentException(
                    "invalid number of rows: " + values.length());
        }
        for (int row = 0; row < rows; ++row)
            set(row, column, values.get(row));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        if (columns.length != cols) {
            throw new IllegalArgumentException(
                "invalid number of columns: " + columns.length);
        }
        for (int col = 0; col < cols; ++col) {
            sparseMatrix[row].set(col, columns[col]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        if (values.length() != cols) {
            throw new IllegalArgumentException(
                "invalid number of columns: " + values.length());
        }
        Vectors.copy(sparseMatrix[row], values);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        for (int r = 0; r < rows; ++r) {
            m[r] = sparseMatrix[r].toArray();
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }
}
