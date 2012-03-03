/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class NonNegativeMatrixFactorizationMultiplicativeTest {

    public static final double[][] VALUES = {
        {1, 2, 3},
        {4, 5, 0},
        {7, 8, 9},
        {10, 0, 12},
    };

    public static final double[][] EXPECTED = {
        {2.0605, 1.7483, 2.2105},
        {2.2532, 5.4146, 1.3004},
        {7.9927, 7.7644, 8.2610},
        {9.3377, 0.1572, 12.4930},
    };

    @Test public void testReduction() {
        MatrixFactorization reducer =
            new NonNegativeMatrixFactorizationMultiplicative();
        SparseMatrix matrix = new YaleSparseMatrix(VALUES);

        reducer.factorize(matrix, 2);

        Matrix W = reducer.dataClasses();
        assertEquals(4, W.rows());
        assertEquals(2, W.columns());

        Matrix H = reducer.classFeatures();
        assertEquals(2, H.rows());
        assertEquals(3, H.columns());

        /*
        for (int r = 0; r < 4; ++r) {
            for (int c = 0; c < 3; ++c) {
                double v = 0;
                for (int k = 0; k < 2; ++k)
                    v += W.get(r, k) * H.get(k, c);
<<<<<<< HEAD
                assertEquals(EXPECTED[r][c], v, .01);
=======
                assertEquals(EXPECTED[r][c], v, .1);
>>>>>>> mavenize
            }
        }
        */
    }
}
