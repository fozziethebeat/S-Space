/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.vector.DoubleVector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author David Jurgens
 */
public class ArrayMatrixTests {

    @Test public void testConstructor() {
        Matrix matrix = new ArrayMatrix(8, 8);
        assertEquals(8, matrix.rows());
        assertEquals(8, matrix.columns());
    }

    @Test public void test2dArrayConstructor() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 9, 0},
        };

        Matrix matrix = new ArrayMatrix(values);
        assertEquals(3, matrix.rows());
        assertEquals(4, matrix.columns());
        for (int r = 0; r < matrix.rows(); ++r)
            for (int c = 0; c < matrix.columns(); ++c)
                assertEquals(values[r][c], matrix.get(r, c), .0001);
    }

    @Test public void testEquals() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 9, 0},
        };

        Matrix m1 = new ArrayMatrix(2, 2);
        Matrix m2 = new ArrayMatrix(2, 2);
        assertEquals(m1, m2);
        assertEquals(m1, m1);

        m1 = new ArrayMatrix(values);
        m2 = new ArrayMatrix(values);
        assertEquals(m1, m2);
        assertEquals(m1, m1);
    }

    @Test public void testGet() {
        Matrix matrix = new ArrayMatrix(10, 10);
        GenericMatrixUtil.testGet(matrix);
    }

    @Test public void testSet() {
        Matrix matrix = new ArrayMatrix(5, 3);
        GenericMatrixUtil.testSet(matrix);
    }

    @Test public void testRowVector() {
        Matrix matrix = new ArrayMatrix(10, 10);
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < 10; ++j)
                matrix.set(i, j, i*j);
        }
        
        DoubleVector row5 = matrix.getRowVector(5);
        for (int j = 0; j < 10; ++j) 
            assertEquals(5 * j, row5.get(j), 0.001d);
        assertEquals(10, row5.length());
        row5.set(5, -1d);
        assertEquals(-1, row5.get(5), 0.001d);
        assertEquals(-1, matrix.get(5,5), 0.001d);
    }   
}
