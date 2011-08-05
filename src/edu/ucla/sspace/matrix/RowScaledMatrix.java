/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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
import edu.ucla.sspace.vector.ScaledDoubleVector;


/**
 * A decorator over {@link Matrix}s.  Each row can be scaled by some
 * scalar constant in O(1) time by applying the scalar value for each row during
 * all gets and sets to values in the row.
 *
 * @author Keith Stevens
 */
public class RowScaledMatrix implements Matrix {

    /**
     * The backing instance of the matrix.
     */
    private final Matrix m;

    /**
     * The {@link DoubleVector} for row scales.
     */
    private final DoubleVector scales;

    /**
     * Creates a {@code RowScaledMatrix} that provides scaled read only access
     * to the provided {@code Matrix} instance.
     */
    public RowScaledMatrix(Matrix matrix, DoubleVector v) {
        this.m = matrix;
        this.scales = v;
    }
    
    /**
     * {@inheritDoc}
     */
    public int columns() {
        return m.columns();
    }
           
    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return m.get(row, col) * scales.get(row);
    }
           
    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        throw new UnsupportedOperationException("Cannot access column");
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        throw new UnsupportedOperationException("Cannot access column");
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        double[] values = m.getRow(row);
        for (int i = 0; i < values.length; ++i)
            values[i] *= scales.get(i);
        return values;
    }
           
    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return new ScaledDoubleVector(m.getRowVector(row), scales.get(row));
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return m.rows();
    }
           
    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] values) {
        throw new UnsupportedOperationException("Cannot set values");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException("Cannot set values");
    }
     
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        return m.toDenseArray();
    }
}
