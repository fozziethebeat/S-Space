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

        double[][] testResults = { {-.167,-.014, .014, .009,-.017, .085,-.018,-.033, .096, .069, .085,-.055,-.079},
                                   {-.014, .031,-.048,-.049,-.037,-.077, .133, .103,-.054,-.021,-.050,-.037, .133},
                                   { .014,-.048,-.113, .094,-.045, .021,-.061, .031, .048,-.046,-.002, .088, .031},
                                   { .009,-.049, .094,-.075,-.037, .033,-.070, .022, .049,-.075,-.021, .069, .023},
                                   {-.017,-.037,-.045,-.037,-.018,-.037, .192, .070,-.055, .069,-.037,-.018,-.026},
                                   { .085,-.077, .021, .033,-.037,-.077,-.071,-.106, .085, .006, .138,-.037,-.053},
                                   {-.018, .133,-.061,-.070, .192,-.071,-.065, .128,-.061, .019,-.071,-.034, .072},
                                   {-.033, .103, .031, .022, .070,-.106, .128,-.113,-.033, .001,-.106, .111, .100},
                                   { .096,-.054, .048, .049,-.055, .085,-.061,-.033,-.167, .049, .085,-.017,-.051},
                                   { .069,-.021,-.046,-.075, .069, .006, .019, .001, .049,-.075, .060,-.037,-.053},
                                   { .085,-.050,-.002,-.021,-.037, .138,-.071,-.106, .085, .060,-.077,-.037,-.053},
                                   {-.055,-.037, .088, .069,-.018,-.037,-.034, .111,-.017,-.037,-.037,-.018,-.026},
                                   {-.079, .133, .031, .023,-.026,-.053, .072, .100,-.051,-.053,-.053,-.026,-.037}};

        System.setProperty(CorrelationTransform.SAVE_NEGATIVES_PROPERTY, "");
        Transform correlation = new CorrelationTransform();
        testIn = correlation.transform(testIn);
        for (int i = 0; i < 13; ++i) {
            for (int j = 0; j < 13; ++j)
                assertEquals(0.0, testResults[i][j] - testIn.get(i, j), .001);
        }
    }
}
