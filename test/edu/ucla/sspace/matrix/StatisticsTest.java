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

public class StatisticsTest {
  @Test public void testRowAverage() {
    double[][] v = {{1, 1, 1, 1}, {1, 2, 3, 4}, {5, 5, 5, 5}};
    Matrix m = new ArrayMatrix(v);
    Matrix expected = new ArrayMatrix(new double[][] {{1}, {2.5}, {5}});
    Matrix actual = Statistics.average(m, Statistics.Dimension.ROW);
    assertEquals(expected.rows(), actual.rows()); 
    assertEquals(expected.columns(), actual.columns());
    for (int i = 0; i < actual.rows(); ++i) {
      for (int j = 0; j < actual.columns(); ++j) {
        assertEquals(expected.get(i, j), actual.get(i, j), .0001);
      }
    }
  }

  @Test public void testRowStd() {
    double[][] v = {{1, 1, 1, 1}, {1, 2, 3, 4}, {5, 5, 5, 5}};
    Matrix m = new ArrayMatrix(v);
    Matrix expected = new ArrayMatrix(new double[][] {{0}, {1.11803399}, {0}});
    Matrix actual = Statistics.std(m, null, Statistics.Dimension.ROW);
    assertEquals(expected.rows(), actual.rows()); 
    assertEquals(expected.columns(), actual.columns());
    for (int i = 0; i < actual.rows(); ++i) {
      for (int j = 0; j < actual.columns(); ++j) {
        assertEquals(expected.get(i, j), actual.get(i, j), .0001);
      }
    }
  }

  @Test public void testRowAverageIgnoreErrors() {
    double[][] v = {{1, -2, 1, 1}, {1, -2, 3, 4}, {5, 5, -2, 5}};
    Matrix m = new ArrayMatrix(v);
    Matrix expected = new ArrayMatrix(new double[][] {{1}, {2.66666}, {5}});
    Matrix actual = Statistics.average(m, Statistics.Dimension.ROW, -2);
    assertEquals(expected.rows(), actual.rows()); 
    assertEquals(expected.columns(), actual.columns());
    for (int i = 0; i < actual.rows(); ++i) {
      for (int j = 0; j < actual.columns(); ++j) {
        assertEquals(expected.get(i, j), actual.get(i, j), .0001);
      }
    }
  }

  @Test public void testRowStdIgnoreErrors() {
    double[][] v = {{1, 1, -2, 1}, {1, 2, 3, 4}, {5, 5, 5, -2}};
    Matrix m = new ArrayMatrix(v);
    Matrix expected = new ArrayMatrix(new double[][] {{0}, {1.11803399}, {0}});
    Matrix actual = Statistics.std(m, null, Statistics.Dimension.ROW, -2);
    assertEquals(expected.rows(), actual.rows()); 
    assertEquals(expected.columns(), actual.columns());
    for (int i = 0; i < actual.rows(); ++i) {
      for (int j = 0; j < actual.columns(); ++j) {
        assertEquals(expected.get(i, j), actual.get(i, j), .0001);
      }
    }
  }
}
