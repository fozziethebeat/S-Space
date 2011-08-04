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
 * A {@code Matrix} decorator class that provides thread safe access to a
 * backing {@code Matrix} instance.
 */

public class SynchronizedMatrix implements Matrix, AtomicMatrix {
    
    /**
     * The backing instance of the matrix.
     */
    private final Matrix m;

    /**
     * Creates a {@code SynchronizedMatrix} that provides thread-safe access to
     * the provided {@code Matrix} instance.
     */
    public SynchronizedMatrix(Matrix matrix) {
        this.m = matrix;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized double addAndGet(int row, int col, double delta) {
        double value = m.get(row, col) + delta;
        m.set(row, col, value);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double getAndAdd(int row, int col, double delta) {
        double value = m.get(row, col);
        m.set(row, col, value + delta);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int columns() {
        return m.columns();
    }
           
    /**
     * {@inheritDoc}
     */
    public synchronized double get(int row, int col) {
        return m.get(row, col);
    }
           
    /**
     * {@inheritDoc}
     */
    public synchronized double[] getColumn(int column) {
        return m.getColumn(column);
    }
           
    /**
     * {@inheritDoc}
     */
    public synchronized DoubleVector getColumnVector(int column) {
        return m.getColumnVector(column);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double[] getRow(int row) {
        return m.getRow(row);
    }
           
    /**
     * {@inheritDoc}
     */
    public synchronized DoubleVector getRowVector(int row) {
        return m.getRowVector(row);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int rows() {
        return m.rows();
    }
           
    /**
     * {@inheritDoc}
     */
    public synchronized void set(int row, int col, double val) {
        m.set(row, col, val);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setColumn(int column, double[] values) {
        m.setColumn(column, values);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setColumn(int column, DoubleVector values) {
        m.setColumn(column, values);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setRow(int row, double[] values) {
        m.setRow(row, values);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setRow(int row, DoubleVector values) {
        m.setRow(row, values);
    }
     
    /**
     * {@inheritDoc}
     */
    public synchronized double[][] toDenseArray() {
        return m.toDenseArray();
    }
}
