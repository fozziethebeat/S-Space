/*
 * Copyright 2010 Keith Stevens 
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
import edu.ucla.sspace.vector.MaskedDoubleVectorView;

import java.util.HashSet;
import java.util.Set;


/**
 * This {@link Matrix} decorator allows every row and column index to be
 * remapped to new indices.  The size of this matrix will depend on the number
 * of mapped rows and columns.  Write-through access is allowed for each cell in
 * the underlying matrix through the {@code set} method.  Array based accessors
 * and getters are disabled when using this decorator.
 *
 * @author Keith Stevens
 */
public class CellMaskedMatrix implements Matrix {
    
    /**
     * The mapping for rows from new indices to old indices.
     */
    protected final int[] rowMaskMap;

    /**
     * The mapping for columns from new indices to old indices.
     */
    protected final int[] colMaskMap;

    /**
     * The original underlying matrix.
     */
    private final Matrix matrix;

    /**
     * Creates a new {@link CellMaskedMatrix} from a given {@link Matrix} and
     * maps, one for the row indices and one for the column indices.  Only valid
     * mappings should be included.
     *
     * @param matrix The underlying matrix to decorate
     * @param rowMaskMap A mapping from new indices to old indices in the
     *        original map for rows.
     * @param colMaskMap A mapping from new indices to old indices in the
     *        original map for columns.
     */
    public CellMaskedMatrix(Matrix matrix, int[] rowMaskMap, int[] colMaskMap) {
        this.matrix = matrix;
        this.rowMaskMap = rowMaskMap;
        this.colMaskMap = colMaskMap;
        assert arrayToSet(rowMaskMap).size() == rowMaskMap.length
            : "input mapping contains duplicates mappings to the same row";
        assert arrayToSet(colMaskMap).size() == colMaskMap.length
            : "input mapping contains duplicates mappings to the same column";
    }

    /**
     * Returns an integer {@link Set} containing all of the values in {@code
     * arr}.
     */
    private Set<Integer> arrayToSet(int[] arr) {
        Set<Integer> s = new HashSet<Integer>();
        for (int value : arr)
            s.add(value);
        return s;
    }

    /**
     * Returns the new index value for a given index from a given mapping.
     * Returns -1 if no mapping is found for the requested row.
     */
    protected int getIndexFromMap(int[] maskMap, int index) {
        if (index < 0 || index >= maskMap.length)
            throw new IndexOutOfBoundsException(
                    "The given index is beyond the bounds of the matrix");
        int newIndex = maskMap[index];
        if (newIndex < 0 ||
            maskMap == rowMaskMap && newIndex >= matrix.rows() ||
            maskMap == colMaskMap && newIndex >= matrix.columns())
            throw new IndexOutOfBoundsException(
                    "The mapped index is beyond the bounds of the base matrix");
        return newIndex;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        row = getIndexFromMap(rowMaskMap, row);
        col = getIndexFromMap(colMaskMap, col);

        return matrix.get(row, col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        column = getIndexFromMap(colMaskMap, column);
        double[] values = new double[rows()];
        for (int r = 0; r < rows(); ++r)
            values[r] = matrix.get(getIndexFromMap(rowMaskMap, r), column);
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        column = getIndexFromMap(colMaskMap, column);
        DoubleVector v = matrix.getColumnVector(column);
        return new MaskedDoubleVectorView(v, rowMaskMap);
    }


    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        row = getIndexFromMap(rowMaskMap, row);
        double[] values = new double[columns()];
        for (int c = 0; c < columns(); ++c)
            values[c] = matrix.get(row, getIndexFromMap(colMaskMap, c));
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        row = getIndexFromMap(rowMaskMap, row);
        DoubleVector v = matrix.getRowVector(row);
        return new MaskedDoubleVectorView(v, colMaskMap);
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return colMaskMap.length;
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] values = new double[rows()][columns()];
        for (int r = 0; r < rows(); ++r)
            for (int c = 0; c < columns(); ++c)
                values[r][c] = get(r, c);
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rowMaskMap.length;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        row = getIndexFromMap(rowMaskMap, row);
        col = getIndexFromMap(colMaskMap, col);

        matrix.set(row, col, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        column = getIndexFromMap(colMaskMap, column);
        for (int r = 0; r < rows(); ++r)
            matrix.set(getIndexFromMap(rowMaskMap, r), column, values[r]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        column = getIndexFromMap(colMaskMap, column);
        for (int r = 0; r < rows(); ++r)
            matrix.set(getIndexFromMap(rowMaskMap, r), column, values.get(r));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        row = getIndexFromMap(rowMaskMap, row);
        for (int c = 0; c < columns(); ++c)
            matrix.set(row, getIndexFromMap(colMaskMap, c), columns[c]);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        row = getIndexFromMap(rowMaskMap, row);
        for (int c = 0; c < columns(); ++c)
            matrix.set(row, getIndexFromMap(colMaskMap, c), values.get(c));
    }
}
