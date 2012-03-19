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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class ListMatrixTest {

    private static final double[][] INVALID_VALUES = {
        {1, 2, 4, 5},
        {0, 0, 0, 1, 1},
    };

    private static final double[][] VALUES = {
        {1, 2, 4, 5},
        {0, 0, 0, 1},
        {0, 5, 0, 1},
    };

    @Test public void testSparseListMatrix() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        for (double[] values : VALUES)
            vectors.add(new CompactSparseVector(values));
        Matrix m = new ListMatrix<SparseDoubleVector>(vectors);
        assertEquals(VALUES.length, m.rows());
        assertEquals(VALUES[0].length, m.columns());
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                assertEquals(VALUES[r][c], m.get(r, c), .0001);

        m.set(0, 0, 0);
        assertEquals(0, vectors.get(0).get(0), .0001);

        for (int r = 0; r < m.rows(); ++r)
            assertEquals(vectors.get(r), m.getRowVector(r));
    }

    @Test (expected=IllegalArgumentException.class)
    public void testInvalidSparseListMatrix() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        for (double[] values : INVALID_VALUES)
            vectors.add(new CompactSparseVector(values));
        Matrix m = new ListMatrix<SparseDoubleVector>(vectors);
    }
}
