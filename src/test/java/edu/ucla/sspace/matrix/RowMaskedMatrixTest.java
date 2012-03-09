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
public class RowMaskedMatrixTest {

    public static final double[][] VALUES = {
        {1, 2, 4, 5, 6},
        {.1, .2, .3, .4, .5},
        {7, 8, 9, 0, 11},
        {.1, .2, .3, .4, .5},
    };

    @Test public void testReorderdMatrix() {
        Matrix baseMatrix = new ArrayMatrix(VALUES);

        int[] reordering = new int[] {1, 2, 3};
        RowMaskedMatrix mapped = new RowMaskedMatrix(baseMatrix, reordering);

        assertEquals(baseMatrix, mapped.backingMatrix());
        assertEquals(3, mapped.rows());
        assertEquals(5, mapped.columns());

        assertEquals(baseMatrix.get(1, 1), mapped.get(0, 1), .0001);
        assertEquals(baseMatrix.get(2, 1), mapped.get(1, 1), .0001);
        assertEquals(baseMatrix.get(3, 1), mapped.get(2, 1), .0001);

        assertEquals(3, mapped.reordering().length);
        assertEquals(1, mapped.reordering()[0]);
        assertEquals(2, mapped.reordering()[1]);
        assertEquals(3, mapped.reordering()[2]);
    }

    @Test public void testTwoLevelReorderdMatrix() {
        Matrix baseMatrix = new ArrayMatrix(VALUES);

        int[] reordering = new int[] {1, 2, 3};
        RowMaskedMatrix mapped = new RowMaskedMatrix(baseMatrix, reordering);

        reordering = new int[] {0, 2, 1, 0};
        mapped = new RowMaskedMatrix(mapped, reordering);

        assertEquals(baseMatrix, mapped.backingMatrix());
        assertEquals(4, mapped.rows());
        assertEquals(5, mapped.columns());

        assertEquals(baseMatrix.get(1, 1), mapped.get(0, 1), .001);
        assertEquals(baseMatrix.get(1, 1), mapped.get(3, 1), .001);

        assertEquals(baseMatrix.get(3, 1), mapped.get(1, 1), .001);

        assertEquals(baseMatrix.get(2, 1), mapped.get(2, 1), .001);
    }

    @Test (expected=IllegalArgumentException.class)
    public void testBelowZeroOrdering() {
        Matrix baseMatrix = new ArrayMatrix(VALUES);

        int[] reordering = new int[] {1, -1, 3};
        RowMaskedMatrix mapped = new RowMaskedMatrix(baseMatrix, reordering);
    }

    @Test (expected=IllegalArgumentException.class)
    public void testOutOfBoundsOrdering() {
        Matrix baseMatrix = new ArrayMatrix(VALUES);

        int[] reordering = new int[] {1, 2, 4};
        RowMaskedMatrix mapped = new RowMaskedMatrix(baseMatrix, reordering);
    }
}
