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

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class RowScaledSparseMatrixTest {

    public static final double[][] VALUES = new double[][] {
        {1, 2, 3, 4, 5},
        {5, 6, 7, 8, 9},
        {1, 1, 0, 0, 1},
    };
    public static final double[] SCALES = new double[] {10, .4, 1};
    public static final DoubleVector SCALE = new DenseVector(SCALES);

    public SparseMatrix matrix() {
        SparseMatrix m = new YaleSparseMatrix(VALUES.length, VALUES[0].length);
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                if (VALUES[r][c] != 0d)
                    m.set(r, c, VALUES[r][c]);
        return m;
    }

    @Test public void testScaledSparseMatrix() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);

        assertEquals(base.rows(), scaled.rows());
        assertEquals(base.columns(), scaled.columns());
        for (int r = 0; r < base.rows(); ++r)
            for (int c = 0; c < base.columns(); ++c)
                assertEquals(base.get(r, c) * SCALE.get(r), scaled.get(r, c), .0001);

        for (int r = 0; r < base.rows(); ++r) {
            DoubleVector v = scaled.getRowVector(r);
            assertEquals(base.columns(), v.length());
            for (int c = 0; c < base.columns(); ++c)
                assertEquals(base.get(r, c) * SCALE.get(r), v.get(c), .0001);
        }
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailGetColumn() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.getColumn(0);
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailGetColumnVector() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.getColumnVector(0);
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailSet() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.set(0, 0, 0);
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailSetColumn() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.setColumn(0, new double[] {1, 1, 1});
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailSetColumnFull() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.setColumn(0, new DenseVector(new double[] {1, 1, 1}));
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailSetRow() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.setRow(0, VALUES[0]);
    }

    @Test (expected=UnsupportedOperationException.class)
    public void testFailSetRowFull() {
        SparseMatrix base = matrix();
        Matrix scaled = new RowScaledSparseMatrix(base, SCALE);
        scaled.setRow(0, new DenseVector(VALUES[0]));
    }
}
