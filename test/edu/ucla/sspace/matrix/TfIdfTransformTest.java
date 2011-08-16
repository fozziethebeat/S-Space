/*
 * Copyright 2010 Keith Stevens
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

import java.util.Arrays;

public class TfIdfTransformTest {

    @Test public void testMatrixTransformSize() {
        int[][] testInput = {{0, 0, 0, 0, 1, 1, 2, 4, 5, 0},
                             {0, 1, 1, 2, 0, 1, 5, 2, 8,10},
                             {1, 5, 0, 0, 1, 0, 6, 3, 7, 9},
                             {0, 1, 0, 1, 0, 1, 2, 0, 3, 0},
                             {1, 5, 7, 0, 0, 1, 6,10, 2,45},
                             {1, 1, 1, 1, 1, 1, 1, 1, 1, 1}};

        double[][] testOutput= {
            { 0.00000, 0.00000, 0.00000, 0.00000, 0.17028, 0.10217, 0.04644, 0.10217, 0.09824, 0.00000, },
            { 0.00000, 0.00810, 0.01171, 0.05268, 0.00000, 0.02107, 0.02395, 0.01054, 0.03242, 0.01621, },
            { 0.07438, 0.08582, 0.00000, 0.00000, 0.07438, 0.00000, 0.06086, 0.03347, 0.06008, 0.03090, },
            { 0.00000, 0.03929, 0.00000, 0.12771, 0.00000, 0.10217, 0.04644, 0.00000, 0.05894, 0.00000, },
            { 0.03512, 0.04052, 0.08195, 0.00000, 0.00000, 0.02107, 0.02873, 0.05268, 0.00810, 0.07294, },
            { -0.03177, -0.00733, -0.01059, -0.02383, -0.03177, -0.01906, -0.00433, -0.00477, -0.00367, -0.00147, }};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 10);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 10; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new TfIdfTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 10; ++col)
                assertEquals(testOutput[row][col],
                             outputMatrix.get(row, col), error);
        }
    }

    @Test public void testNonSquareMatrix() {
        int[][] testInput = {{6, 0, 5},
                             {0, 1, 1},
                             {1, 5, 0},
                             {0, 1, 0},
                             {1, 5, 7},
                             {1, 1, 1}};

        double[][] testOutput= {{}};

        double error = .00001;
        Matrix inputMatrix = new YaleSparseMatrix(6, 3);
        for (int row = 0; row < 6; ++row)
            for (int col = 0; col < 3; ++col)
                inputMatrix.set(row, col, testInput[row][col]);

        Transform transform = new TfIdfTransform();
        Matrix outputMatrix = transform.transform(inputMatrix);
        for (int row = 0; row < 6; ++row) {
            for (int col = 0; col < 3; ++col)
                System.out.println(outputMatrix.get(row, col));
                //assertEquals(testOutput[row][col],
                //             outputMatrix.get(row, col), error);
        }
    }
}
