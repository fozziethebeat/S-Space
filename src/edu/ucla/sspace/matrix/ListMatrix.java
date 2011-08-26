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

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.ArrayList;
import java.util.List;


/**
 * A {@link Matrix} implementation that buildes a matrix out of a list of
 * {@link DoubleVector}s.  
 *
 * @author Keith Stevens
 */
class ListMatrix<T extends DoubleVector> implements Matrix {

    /**
     * The list of {@code DoubleVector}s providing the values for the {@code
     * Matrix}.
     */
    protected List<T> vectors;

    /**
     * The number of columns in the {@code Matrix}
     */
    protected int columns;

    /**
     * Constructs a matrix from the list of vectors where the first list element
     * is treated as the first row in the matrix and so on.
     *
     * @throws IllegalArgumentException if the list is empty or if the vectors
     * have different lengths
     */
    public ListMatrix(List<T> vectors) {
        if (vectors.size() == 0)
            throw new IllegalArgumentException(
                "Must provide at least one vector");
        // Copy the contents to an ArrayList to guarantee O(1) row access
        this.vectors = new ArrayList<T>(vectors.size());
        columns = vectors.get(0).length();
        for (T t : vectors) {
            if (t.length() != columns)
                throw new IllegalArgumentException(
                    "Cannot create ragged matrix from list of vectors");
            this.vectors.add(t);
        }        
    }

    public ListMatrix(List<T> vectors, int columns) {
        if (vectors.size() == 0)
            throw new IllegalArgumentException(
                "Must provide at least one vector");
        // Copy the contents to an ArrayList to guarantee O(1) row access
        this.vectors = new ArrayList<T>(vectors);
        this.columns = columns;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int column) {
        return vectors.get(row).get(column);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        int i = 0;
        double[] columnValues = new double[vectors.size()];

        for (DoubleVector vector : vectors)
            columnValues[i++] = vector.get(column);
        return columnValues;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        int i = 0;
        DoubleVector columnValues = new DenseVector(vectors.size());

        for (DoubleVector vector : vectors)
            columnValues.set(i++, vector.get(column));
        return columnValues;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return vectors.get(row).toArray();
    }

    /**
     * {@inheritDoc}
     */
    public T getRowVector(int row) {
        return vectors.get(row);
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
    public int rows() {
        return vectors.size();
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] result = new double[vectors.size()][columns];
        int row = 0;
        for (DoubleVector vector : vectors) {
            for (int i = 0; i < vector.length(); ++i)
                result[row][i] = vector.get(i);
            row++;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int column, double value) {
        T vector = vectors.get(row);
        vector.set(column, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        int i = 0;
        for (DoubleVector vector : vectors)
            vector.set(column, values[i++]);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        int i = 0;
        for (DoubleVector vector : vectors)
            vector.set(column, values.get(i++));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] values) {
        T v = vectors.get(row);
        for (int i = 0; i < values.length; ++i)
            v.set(i, values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        T v = vectors.get(row);
        for (int i = 0; i < values.length(); ++i)
            v.set(i, values.get(i));
    }
}
