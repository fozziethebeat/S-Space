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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.HashMap;
import java.util.Map;


/**
 * A growing sparse {@code Matrix} based on the Yale Sparse Matrix Format.  Each
 * row is allocated a pair of arrays which keeps the non-zero column values in
 * column order.  Lookups are O(log n) where n is the number of non-zero values
 * for the largest row.  Calls to set and setColumn can expand the matrix by
 * both rows and columns. <p>
 *
 * Calls to {@link #getRowVector(int) getRowVector} and {@link
 * #getColumnVector(int) getColumnVector} return a snapshot of the matrix data
 * at the time of the call.  Subsequent updates to the matrix will not be
 * reflected in these vectors.  The returned vectors are immutable and any calls
 * to mutating operations will throw an {@code
 * UnsupportedOperationException}. <p>
 *
 * This class is not thread-safe.
 *
 * @author Keith Stevens 
 */
public class GrowingSparseMatrix extends AbstractMatrix
        implements SparseMatrix, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The current number of rows in this {@code GrowingSparseMatrix}.
     */
    private int rows;

    /**
     * The current number of columns in this {@code GrowingSparseMatrix}.
     */
    private int cols;

    /**
     * Each row is defined as a {@link SparseDoubleVector} which does most of
     * the work.
     */
    private final Map<Integer, SparseDoubleVector> rowToColumns;

    /**
     * The default empty vector which will be used to populate new rows as they
     * are generated.
     */
    private final SparseDoubleVector emptyVector;

    /**
     * Create a new empty {@code GrowingSparseMatrix} which uses a {@link
     * CompactSparseVector} as the backing data structure.
     */
    public GrowingSparseMatrix() {
        this(new CompactSparseVector());
    }

    /**
     * Create a new empty {@code GrowingSparseMatrix} with no specified row or
     * column boundaries.
     *
     * @param emptyVector An empty {@link SparseDoubleVector} with no bounds
     *                    that will be used to generate new rows in the matrix.
     */
    public GrowingSparseMatrix(SparseDoubleVector emptyVector) {
        this(0,0, emptyVector);
    }

    /**
     * Create a new empty {@code GrowingSparseMatrix} with the specified
     * dimensions.
     *
     * @param rows the number of rows in the matrix
     * @param columns the number of columns in the matrix
     * @param emptyVector An empty {@link SparseDoubleVector} with no bounds
     *                    that will be used to generate new rows in the matrix.
     */
    public GrowingSparseMatrix(int rows, int columns, SparseDoubleVector emptyVector) {
        if (emptyVector.length() != Integer.MAX_VALUE) 
            throw new IllegalArgumentException(
                    "The given empty vector has a restricted bound.  " +
                    "Given empty vectors must have no bound");

        this.rows = rows;
        this.cols = columns;
        this.emptyVector = emptyVector;
        rowToColumns = new HashMap<Integer,SparseDoubleVector>();
    }

    /**
     * Validate that the row and column indices are non-zero.
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0)
            throw new ArrayIndexOutOfBoundsException("Negative index");
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        checkIndices(row, col);
        SparseDoubleVector sv = rowToColumns.get(row);
        return (sv == null) ? 0 : sv.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        double[] values = new double[rows()];
        for (int row = 0; row < rows(); ++row)
            values[row] = get(row, column);
        return values;
    }

    /**
     * Returns a {@link DoubleVector} of the contents of the column.
     *
     * @param column {@inheritDoc}
     * @return {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int column) {
        SparseDoubleVector values = new SparseHashDoubleVector(rows);
        for (int row = 0; row < rows; ++row) {
            double d = get(row, column);
            if (d != 0)
                values.set(row, d);
        }
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return toArray(rowToColumns.get(row) ,cols);
    }

    /**
     * Returns a {@link DoubleVector} of the contents of the row.  The length of
     * the returned row vector reflects the size of matrix at the time of the
     * call, which may be different from earlier calls to {@link #columns()}.
     *
     * @param row {@inheritDoc}
     * @return {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        SparseDoubleVector v = rowToColumns.get(row);
        return (v != null)
            ? Vectors.subview(v, 0, cols)
            : Vectors.subview(emptyVector, 0, cols);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The size of the matrix will be expanded if either row or col is larger
     * than the largest previously seen row or column value.    When the matrix
     * is expanded by either dimension, the values for the new row/column will
     * all be assumed to be zero.
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);

        // Check whether the dimensions need to be updated
        if (row + 1 > rows)
            rows = row + 1;
        if (col + 1 > cols)
            cols = col + 1;

        updateRow(row).set(col, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        for (int row = 0; row < rows(); ++row)
            set(row, column, values[row]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        for (int row = 0; row < rows(); ++row)
            set(row, column, values.get(row));
    }

    /** 
     * {@inheritDoc}
     *
     * The size of the matrix will be expanded if either row or
     * col is larger than the largest previously seen row or column value.
     * When the matrix is expanded by either dimension, the values for the new
     * row/column will all be assumed to be zero.
     */
    public void setRow(int row, double[] columns) {
        checkIndices(row, columns.length - 1);

        if (cols <= columns.length)
            cols = columns.length;
        
        SparseDoubleVector rowVec = updateRow(row);

        for (int col = 0; col < cols; ++col) {
            double val = columns[col];
            rowVec.set(col, val);
        }
    }

    /**
     * Returns the {@link SparseDoubleVector} at index {@code row}.  If one does
     * not exist, a new vector will be created and stored in the mapping.
     */
    private SparseDoubleVector updateRow(int row) {
        SparseDoubleVector rowVec = rowToColumns.get(row);
        if (rowVec == null) {
            rowVec = emptyVector.instanceCopy();
            rowToColumns.put(row, rowVec);
        }

        return rowVec;
    }

    /** 
     * {@inheritDoc}
     *
     * The size of the matrix will be expanded if either row or
     * col is larger than the largest previously seen row or column value.
     * When the matrix is expanded by either dimension, the values for the new
     * row/column will all be assumed to be zero.
     */
    public void setRow(int row, DoubleVector data) {
        checkIndices(row, data.length() -1);

        if (cols <= data.length())
            cols = data.length();

        Vectors.copy(updateRow(row), data);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];

        for (int r = 0; r < rows; ++r) 
            m[r] = toArray(rowToColumns.get(r), cols);
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
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
