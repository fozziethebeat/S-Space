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

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;


/**
 * An abstract {@link Matrix} class that provides common implementations for
 * generic matrix operations.  This class assumes that all subclasses are (1)
 * row-based and (2) any changes to the row vectors will be propagated to the
 * backing matrix. Therefore all operations will call {@link #getRowVector(int)
 * getRowVector} to perform their functions.  At a minimum subclasses must
 * define the following three operations to be an immutable matrix:
 *
 * <ul>
 *  <li> {@link #getRowVector(int) getRowVector}
 *  <li> {@link #rows()}
 *  <li> {@link #columns()}
 * </ul>
 *
 * To support modification, implementations need only define
 * <ul>
 *  <li> {@link #set(int, int, double) set}
 * </ul>
 */
public abstract class AbstractMatrix implements Matrix {

    /**
     * {@inheritDoc}
     */
    public abstract int columns();

    /**
     * Returns {@code} if the object is a {@link Matrix} with the same
     * dimensions as whose values are equivalent.  Value equivalence is computed
     * using {@link Vector} equality on the row vectors of each matrix.
     */
    public boolean equals(Object o) {
        if (o instanceof Matrix) {
            Matrix m = (Matrix)o;
            int rows = rows();
            if (m.rows() != rows)
                return false;
            int cols = columns();
            if (m.columns() != cols)
                return false;
            // REMINDER: this could potentially be optimized for sparse
            // matrices.
            for (int r = 0; r < rows; ++r) {
                for (int c = 0; c < cols; ++c) {
                    if (m.get(r, c) != get(r, c))
                        return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return getRowVector(row).get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        return getColumnVector(column).toArray();
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        DoubleVector col = new DenseVector(rows());
        for (int r = 0; r < rows(); ++r)
            col.set(r, getRowVector(r).get(column));
        return col;
    }
    
    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return getRowVector(row).toArray();
    }

    /**
     * {@inheritDoc}
     */
    public abstract DoubleVector getRowVector(int row);

    /**
     * Returns the sum of all rows hash codes.
     */
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < rows(); ++i)
            hash += getRowVector(i).hashCode();
        return hash;
    }
    
    /**
     * {@inheritDoc}
     */
    public abstract int rows();

    /**
     * Throws an {@link UnsupportedOperationException} if called (matrix is
     * unmodifiable).
     */
    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException("Matrix is immutable");
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the length of {@code values} is not
     *         equal to the number of rows
     */
    public void setColumn(int column, double[] values) {
        setColumn(column, Vectors.asVector(values));
    }
                
    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the length of {@code values} is not
     *         equal to the number of rows
     */
    public void setColumn(int column, DoubleVector values) {
        if (values.length() != rows())
            throw new IllegalArgumentException(
                "Number of values is not equal the number of rows");
        for  (int r = 0; r < values.length(); ++r)
            set(r, column, values.get(r));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the length of {@code columns} is not
     *         equal to the number of columns
     */
    public void setRow(int row, double[] columns) {
        setRow(row, Vectors.asVector(columns));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the length of {@code columns} is not
     *         equal to the number of columns
     */
    public void setRow(int row, DoubleVector values) {
        if (values.length() != columns())
            throw new IllegalArgumentException(
                "Number of values is not equal the number of rows");
        for  (int c = 0; c < values.length(); ++c)
            set(row, c, values.get(c));
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows()][columns()];
        for (int r = 0; r < rows(); ++r) {
            m[r] = getRowVector(r).toArray();
        }
        return m;
    }

    public String toString() {
        int rows = rows();
        int columns = columns();
        StringBuilder sb = new StringBuilder(rows * columns * 2);
        for (int r = 0; r < rows; ++r) {
            sb.append('[');
            for (int c = 0; c < columns; ++c) {
                sb.append(get(r, c));
                if (c + 1 < columns)
                    sb.append(", ");
            }
            sb.append("]\n");
        }
        return sb.toString();
    }
}