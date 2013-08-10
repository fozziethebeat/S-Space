/*
 * Copyright 2012 David Jurgens
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

import java.util.*;


/**
 * @author David Jurgens
 */
public class ColumnMaskedMatrixTest {

    public static final double[][] VALUES = new double[][] {
        { 1, 2, 3, 4, 5},
        { 1, 2, 3, 4, 5},
        { 1, 2, 3, 4, 5},
        { 1, 2, 3, 4, 5},
    };

    @Test public void testDimensions() throws Exception {
        Matrix baseMatrix = new ArrayMatrix(VALUES);
        int[] toKeep = new int[] {1, 2, 3};
        ColumnMaskedMatrix masked = new ColumnMaskedMatrix(baseMatrix, toKeep);
        assertEquals(4, masked.rows());
        assertEquals(3, masked.columns());
    }

    @Test public void testGet() throws Exception {
        Matrix baseMatrix = new ArrayMatrix(VALUES);
        Integer[] toKeep = new Integer[] {1, 2, 3};
        ColumnMaskedMatrix masked = new ColumnMaskedMatrix(baseMatrix, 
                                                           new HashSet<Integer>(Arrays.asList(toKeep)));
        assertEquals(2, masked.get(0, 0), 0.01);
        assertEquals(3, masked.get(1, 1), 0.01);
        assertEquals(4, masked.get(2, 2), 0.01);
    }

    @Test public void testGetWithGaps() throws Exception {
        Matrix baseMatrix = new ArrayMatrix(VALUES);
        int[] toKeep = new int[] {0, 2, 4};
        ColumnMaskedMatrix masked = new ColumnMaskedMatrix(baseMatrix, toKeep);
        assertEquals(1, masked.get(0, 0), 0.01);
        assertEquals(3, masked.get(1, 1), 0.01);
        assertEquals(5, masked.get(2, 2), 0.01);
    }
}
