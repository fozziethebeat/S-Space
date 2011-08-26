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

import org.junit.*;

import static org.junit.Assert.*;


public class CorrelationTransformTest {

    @Test public void testCorrelationWithSaveNegs() {
        // 13 x 13 matrix.
        double[] testCorrelations = {0, 5, 9, 6, 1,10, 4, 8,18, 9,10, 0, 0,
                                     5, 4, 2, 1, 0, 0, 7,10, 3, 2, 1, 0, 5,
                                     9, 2, 0, 8, 0, 5, 1, 9,11, 2, 4, 3, 3,
                                     6, 1, 8, 0, 0, 4, 0, 6, 8, 0, 2, 2, 2,
                                     1, 0, 0, 0, 0, 0, 4, 3, 0, 2, 0, 0, 0,
                                     10,0, 5, 4, 0, 0, 0, 0,10, 3, 8, 0, 0,
                                     4, 7, 1, 0, 4, 0, 0,10, 2, 3, 0, 0, 3,
                                     8,10, 9, 6, 3, 0,10, 2, 8, 5, 0, 4, 6,
                                     18,3,11, 8, 0,10, 2, 8, 0, 8, 10,1, 1,
                                     9, 2, 2, 0, 2, 3, 3, 5, 8, 0, 5, 0, 0,
                                     10, 1, 4, 2, 0, 8, 0, 0,10, 5,0, 0, 0,
                                     0, 0, 3, 2, 0, 0, 0, 4, 1, 0, 0, 0, 0,
                                     0, 5, 3, 2, 0, 0, 3, 6, 1, 0, 0, 0, 0};
        Matrix testIn = new ArrayMatrix(13, 13, testCorrelations);

        double[][] testResults = {
            {0, 0, 0.120275, 0.092693, 0, 0.291412, 0, 0, 0.309570, 0.262176, 0.291412, 0, 0},
            {0, 0.175412, 0, 0, 0, 0, 0.364324, 0.320477, 0, 0, 0, 0, 0.365295},
            {0.120275, 0, 0, 0.305794, 0, 0.145923, 0, 0.177408, 0.219592, 0, 0, 0.297310, 0.175178},
            {0.092693, 0, 0.305794, 0, 0, 0.181871, 0, 0.149168, 0.220654, 0, 0, 0.262782, 0.151498},
            {0, 0, 0, 0, 0, 0, 0.437693, 0.264944, 0, 0.262782, 0, 0, 0},
            {0.291412, 0, 0.145923, 0.181871, 0, 0, 0, 0, 0.291412, 0.076401, 0.372104, 0, 0},
            {0, 0.364324, 0, 0, 0.437693, 0, 0, 0.357573, 0, 0.136273, 0, 0, 0.268243},
            {0, 0.320477, 0.177408, 0.149168, 0.264944, 0, 0.357573, 0, 0, 0.034164, 0, 0.332746, 0.316521},
            {0.309570, 0, 0.219592, 0.220654, 0, 0.291412, 0, 0, 0, 0.220654, 0.291412, 0, 0},
            {0.262176, 0, 0, 0, 0.262782, 0.076401, 0.136273, 0.034164, 0.220654, 0, 0.245595, 0, 0},
            {0.291412, 0, 0, 0, 0, 0.372104, 0, 0, 0.291412, 0.245595, 0, 0, 0},
            {0, 0, 0.297310, 0.262782, 0, 0, 0, 0.332746, 0, 0, 0, 0, 0},
            {0, 0.365295, 0.175178, 0.151498, 0, 0, 0.268243, 0.316521, 0, 0, 0, 0, 0},
        };

        Transform correlation = new CorrelationTransform();
        testIn = correlation.transform(testIn);
        for (int i = 0; i < 13; ++i) {
            for (int j = 0; j < 13; ++j)
                assertEquals(testResults[i][j], testIn.get(i, j), .001);
        }
    }
}
