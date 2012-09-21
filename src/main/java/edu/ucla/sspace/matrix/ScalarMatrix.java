/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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


/**
 * This represents an immutable {@link Matrix} with a single value.
 *
 * @author Keith Stevens
 */ public class ScalarMatrix extends AbstractMatrix {

    /**
     * The scalar value for all entries in this matrix.
     */
    private double scalar;

    /**
     * The number of rows.
     */
    private int rows;

    /**
     * The number of columns.
     */
    private int columns;

    /**
     * Creates a new {@link ScalarMatrix} with the given number of rows,
     * columns, and scalar value for every entry.
     */
    public ScalarMatrix(int rows, int columns, double scalar) {
        this.rows = rows;
        this.columns = columns;
        this.scalar = scalar;
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public double add(int row, int col, double delta) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        return scalar;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        return new DenseVector(getRow(row));
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
        return rows;
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public void setColumn(int col, double[] values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public void setColumn(int col, DoubleVector values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public void setRow(int row, double[] values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException
     */
    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException(
                "Cannot modify values in a ScalarMatrix");
    }
}
