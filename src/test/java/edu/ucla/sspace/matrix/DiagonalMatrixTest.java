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


import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DiagonalMatrixTest {

    @Test public void testDefaultConstructor() {
        DiagonalMatrix matrix = new DiagonalMatrix(5);
        for (int i = 0; i < 5; ++i)
            matrix.set(i, i, i);
        assertEquals(5, matrix.rows());
        assertEquals(5, matrix.columns());
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (j == i)
                    assertEquals(i, matrix.get(i, i), .0001);
                else
                    assertEquals(0, matrix.get(i, j), .0001);
            }
        }
    }

    @Test public void testArrayConstructor() {
        double[] values = {6, 5, 4, 3, 2, 1};
        DiagonalMatrix matrix = new DiagonalMatrix(values);
        assertEquals(6, matrix.rows());
        assertEquals(6, matrix.columns());
        for (int i = 0; i < 6; ++i) {
            for (int j = 0; j < 6; ++j) {
                if (j == i)
                    assertEquals(values[i], matrix.get(i, i), .0001);
                else
                    assertEquals(0, matrix.get(i, j), .0001);
            }
        }
    }

    @Test(expected=IllegalArgumentException.class) 
        public void testInvalidSet() {

        DiagonalMatrix matrix = new DiagonalMatrix(6);
        matrix.set(1, 2, 3);
    }
}
