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


/**
 * A {@code Matrix} decorator class that tranposes the data in the backing
 * matrix.  This class provides a way to quickly tranpose the matrix data
 * without the need for copying it.
 */
public class TransposedMatrix implements Matrix {
    
    /**
     * The backing instance of the matrix.
     */
    final Matrix m;

    /**
     * Creates a {@code Matrix} that provides a transposed view of the original
     * matrix.
     */
    public TransposedMatrix(Matrix matrix) {
        this.m = matrix;
    }
    
    /**
     * {@inheritDoc}
     */
    public int columns() {
        return m.rows();
    }
           
    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return m.get(col, row);
    }
           
    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        return m.getRow(column);
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        return m.getRowVector(column);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        return m.getColumn(row);
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return m.getColumnVector(row);
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return m.columns();
    }
           
    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        m.set(col, row, val);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        m.setRow(column, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        m.setRow(column, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] values) {
        m.setColumn(row, values);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        m.setColumn(row, values);
    }
     
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] arr = new double[columns()][0];
        int cols = columns();
        for (int col = 0; col < cols; ++col)
            arr[col] = m.getRow(col);
        return arr;
    }
}
