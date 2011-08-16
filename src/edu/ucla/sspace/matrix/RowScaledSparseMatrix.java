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
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * A decorator over {@link SparseMatrix}s.  Each row can be scaled by some
 * scalar constant in O(1) time by applying the scalar value for each row during
 * all gets and sets to values in the row.
 *
 * @author Keith Stevens
 */
public class RowScaledSparseMatrix extends RowScaledMatrix
                                   implements SparseMatrix {

    /**
     * The backing instance of the matrix.
     */
    private final SparseMatrix m;

    /**
     * The {@link DoubleVector} for row scales.
     */
    private final DoubleVector scales;

    /**
     * Creates a {@code RowScaledSparseMatrix} that provides scaled read only
     * access to the provided {@code SparseMatrix} instance.
     */
    public RowScaledSparseMatrix(SparseMatrix matrix, DoubleVector v) {
        super(matrix, v);
        this.m = matrix;
        this.scales = v;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int row) {
        throw new UnsupportedOperationException("cannot get row");
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        return new ScaledSparseDoubleVector(
                m.getRowVector(row), scales.get(row));
    }
}
