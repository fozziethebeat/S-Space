/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class YaleSparseMatrixTest {

    @Test public void testConstructor() {
        Matrix matrix = new YaleSparseMatrix(10, 4);
        assertEquals(10, matrix.rows());
        assertEquals(4, matrix.columns());
    }

    @Test public void test2dArrayConstructor() {
        double[][] values = {
            {1, 1, 2, 4},
            {4, 3, 2, 1},
            {7, 8, 9, 0},
        };

        Matrix matrix = new YaleSparseMatrix(values);
        assertEquals(3, matrix.rows());
        assertEquals(4, matrix.columns());
        for (int r = 0; r < matrix.rows(); ++r)
            for (int c = 0; c < matrix.columns(); ++c)
                assertEquals(values[r][c], matrix.get(r, c), .0001);
    }

    @Test public void testGet() {
        Matrix matrix = new YaleSparseMatrix(10, 4);
        GenericMatrixUtil.testGet(matrix);
    }

    @Test public void testSet() {
        Matrix matrix = new YaleSparseMatrix(10, 4);
        GenericMatrixUtil.testSet(matrix);
    }
}
