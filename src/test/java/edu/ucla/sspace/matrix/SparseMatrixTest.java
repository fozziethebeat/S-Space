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

public class SparseMatrixTest {
  @Test public void testReplaceWithZero() {
    double[][] testData = {{0, 0, 0, 1},
                           {1, 0, 2, 0},
                           {0, 0, 0, 0},
                           {1, 2, 4, 5},
                           {0, 0, 1, 0}};
    YaleSparseMatrix testMatrix = new YaleSparseMatrix(5, 4);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        testMatrix.set(i, j, testData[i][j]);
      }
    }
    testData[1][1] = 4;
    testMatrix.set(1, 1, 4);
    testData[1][0] = 0;
    testMatrix.set(1, 0, 0);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
      }
    }
  }

  @Test public void testMatrix() {
    double[][] testData = {{0, 0, 0, 1},
                           {1, 0, 2, 0},
                           {0, 0, 0, 0},
                           {1, 2, 4, 5},
                           {0, 0, 1, 0}};
    YaleSparseMatrix testMatrix = new YaleSparseMatrix(5, 4);
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        testMatrix.set(i, j, testData[i][j]);
      }
    }
    for (int i = 0; i < 5; ++i) {
      for (int j = 0; j < 4; ++j) {
        assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
      }
    }
  }
}
