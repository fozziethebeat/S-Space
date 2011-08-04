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
import edu.ucla.sspace.vector.SparseVector;

/**
 * An interface specification for interacting with {@code Matrix} objects.
 *
 * @see MatrixIO
 * @see Matrix.Type
 *
 * @author David Jurgens
 */
public interface Matrix {

    /**
     * The type of {@code Matrix} instance.  This enum should be used as a hint
     * for algorithms that create {@code Matrix} instances as to what
     * implementation to use.
     */
    public enum Type {
        /**
         * A {@code Matrix} where the majority of the values are 0, and is
         * small enough to fit into memory.
         */
        SPARSE_IN_MEMORY,

        /**
         * A {@code Matrix} that contains very few 0 values and is small
         * enough to fit into memory.
         */
        DENSE_IN_MEMORY,

        /**
         * A {@code Matrix} with very few non-zero values and is sufficiently
         * large enough that it would not fit in memory.
         */
        SPARSE_ON_DISK,

        /**
         * A {@code Matrix} with very few zero values and is sufficiently large
         * enough that it would not fit in memory.
         */
        DENSE_ON_DISK,

        /**
         * A diagonal {@code Matrix}, saving a little more meory than a sparse
         * {@code Matrix}.
         */
        DIAGONAL_IN_MEMORY
    }

    /**
     * Returns the value of the {@code Matrix} at the provided row and column.
     *
     * @param row The row values of the cell to return.
     * @param col The column value of the cell to return.
     *
     * @return The value at the cell specified by {@code row} and {@code col}.
     */
    double get(int row, int col);

    /**
     * Returns the entire column.
     *
     * @param column The column to return an array for.
     *
     * @return A double array representing the column at {@code column}.
     */
    double[] getColumn(int column);

    /**
     * Returns the column as a vector.  Whether updates to the vector are
     * written through to the backing matrix is left open to the specific
     * implementation.  Implementations that maintain their data in a sparse
     * format are encouraged to return a {@link SparseVector} instance.
     *
     * @param column The column to return a {@code DoubleVector} for.
     *
     * @return A {@code DoubleVector} representing the column at {@code column}.
     */
    DoubleVector getColumnVector(int column);

    /**
     * Returns the entire row.
     *
     * @param row The row to return an array for.
     *
     * @return A double array representing the row at {@code row}.
     */
    double[] getRow(int row);

    /**
     * Returns a {@code DoubleVector} for an entire row.  Implementations should
     * return an approriately typed DoubleVector.  Whether updates to the vector
     * are written through to the backing matrix is left open to the specific
     * implementation.  Implementations that maintain their data in a sparse
     * format are encouraged to return a {@link SparseVector} instance.
     *
     * @param row the index of the row to return.
     *
     * @return A {@code DoubleVector} representing the row at {@code row}.
     */
    DoubleVector getRowVector(int row);

    /**
     * Returns the number of columns in this {@code Matrix}.
     *
     * @return The number of columns in this {@code Matrix}.
     */
    int columns();
    
    /**
     * Converts the {@code Matrix} to a two dimensional array.  Note that for
     * large matrices, this may exhaust all available memory.
     *
     * @return A double array version of this {@code Matrix}.
     */
    double[][] toDenseArray();

    /**
     * Returns the number of rows in this {@code Matrix}.
     *
     * @return The number of columns in this {@code Matrix}.
     */
    int rows();    

    /**
     * Sets the location at the row and column to the provided value.
     *
     * @param row The row of the cell to update.
     * @param col The column of the cell to update.
     * @param val The new value of the specified cell.
     */
    void set(int row, int col, double val);

    /**
     * Sets the values for the column of this {@code Matrix} using the provided
     * array.  Note that the array itself is not made internal to the instance
     * itself.
     *
     * @param column The column to update.
     * @param values The values to update into {@code column}.
     */
    void setColumn(int column, double[] values);

    /**
     * Sets the values for the column of this {@code Matrix} using the provided
     * {@code DoubleVector}.  Note that the {@code DoubleVector} itself is not
     * made internal to the instance itself.
     *
     * @param column The column to update.
     * @param values The values to update into {@code column}.
     */
    void setColumn(int column, DoubleVector values);

    /**
     * Sets the values for the row of this {@code Matrix} using the provided
     * array.  Note that the array itself is not made internal to the instance
     * itself.
     *
     * @param row The row to update.
     * @param values The values to update into {@code row}.
     */
    void setRow(int row, double[] values);

    /**
     * Sets the values for the row of this {@code Matrix} using the provided
     * {@code DoubleVector}.  Note that the {@code DoubleVector} itself is not
     * made internal to the instance itself.
     *
     * @param row The row to update.
     * @param values The values to update into {@code row}.
     */
    void setRow(int row, DoubleVector values);
}
