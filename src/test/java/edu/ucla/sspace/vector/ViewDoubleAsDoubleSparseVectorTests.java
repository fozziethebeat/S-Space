/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.vector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * Tests for the {@link ViewDoubleAsDoubleSparseVector} class.
 */
public class ViewDoubleAsDoubleSparseVectorTests {

    @Test public void testMagnitude() {
        SparseDoubleVector v = new CompactSparseVector(100);
        v.set(1, 1);
        v.set(3, 1);
        v.set(5, 1);
        v.set(7, 1);
        v.set(9, 1);
        SparseDoubleVector view = Vectors.subview(v, 0, 100);
        assertEquals(v.magnitude(), view.magnitude(), .0001);

        view = Vectors.subview(v, 2, 98);
        assertEquals(Math.sqrt(4), view.magnitude(), .0001);

        view = Vectors.subview(v, 0, 8);
        assertEquals(Math.sqrt(4), view.magnitude(), .0001);
    }

    @Test public void testLength() {
        SparseDoubleVector v = new CompactSparseVector(100);
        v.set(1, 1);
        v.set(3, 1);
        v.set(5, 1);
        v.set(7, 1);
        v.set(9, 1);
        SparseDoubleVector view = Vectors.subview(v, 0, 100);
        assertEquals(v.length(), view.length());

        view = Vectors.subview(v, 2, 98);
        assertEquals(98, view.length());

        view = Vectors.subview(v, 0, 8);
        assertEquals(8, view.length(), 8);
    }

    @Test public void testNonZero() {
        SparseDoubleVector v = new CompactSparseVector(100);
        v.set(1, 1);
        v.set(3, 1);
        v.set(5, 1);
        v.set(7, 1);
        v.set(9, 1);
        SparseDoubleVector view = Vectors.subview(v, 0, 100);
        int[] a1 = v.getNonZeroIndices();
        Arrays.sort(a1);
        int[] a2 = view.getNonZeroIndices();
        Arrays.sort(a2);        
        assertTrue(Arrays.equals(a1, a2));

        a1 = new int[] { 3, 5, 7, 9};        
        view = Vectors.subview(v, 2, 98);
        a2 = view.getNonZeroIndices();
        Arrays.sort(a2);        
        assertTrue(Arrays.equals(a1, a2));


        a1 = new int[] { 1, 3, 5, 7 };        
        view = Vectors.subview(v, 0, 8);
        a2 = view.getNonZeroIndices();        
        Arrays.sort(a2);        
        System.out.printf("a1 = %s, a2 = %s%n", Arrays.toString(a1), Arrays.toString(a2));
        assertTrue(Arrays.equals(a1, a2));
    }
}
