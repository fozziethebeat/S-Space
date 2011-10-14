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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DefaultPermutationFunctionTest {

    @Test public void testPermutation() {
        IntegerVector v = new DenseIntVector(new int[] {1, 2, 3, 4, 5, 6});
        Set<Integer> vValues = new HashSet<Integer>();
        for (int i = 0; i < v.length(); ++i)
            vValues.add(v.get(i));

        DefaultPermutationFunction func = new DefaultPermutationFunction();
        Vector permuted = func.permute(v, 1);
        assertNotEquals(v, permuted);
        assertEquals(v.length(), permuted.length());
        assertTrue(permuted instanceof IntegerVector);
        for (int i = 0; i < permuted.length(); ++i)
            assertTrue(vValues.contains(permuted.getValue(i)));
    }

    @Test public void testNegativePermutation() {
        IntegerVector v = new DenseIntVector(new int[] {1, 2, 3, 4, 5, 6});
        Set<Integer> vValues = new HashSet<Integer>();
        for (int i = 0; i < v.length(); ++i)
            vValues.add(v.get(i));

        DefaultPermutationFunction func = new DefaultPermutationFunction();
        Vector permuted = func.permute(v, -1);
        assertNotEquals(v, permuted);
        assertEquals(v.length(), permuted.length());
        assertTrue(permuted instanceof IntegerVector);
        for (int i = 0; i < permuted.length(); ++i)
            assertTrue(vValues.contains(permuted.getValue(i)));
    }

    @Test public void testInversePermutation() {
        IntegerVector v = new DenseIntVector(new int[] {1, 2, 3, 4, 5, 6});
        DefaultPermutationFunction func = new DefaultPermutationFunction();
        Vector permuted = func.permute(v, 1);
        Vector invPermuted = func.permute(permuted, -1);
        assertNotEquals(v, permuted);
        assertNotEquals(permuted, invPermuted);
        assertTrue(permuted instanceof IntegerVector);
        assertTrue(invPermuted instanceof IntegerVector);
        for (int i = 0; i < v.length(); ++i)
            assertEquals(v.get(i), invPermuted.getValue(i).intValue());
    }

    @Test public void testInverseNegativePermutation() {
        IntegerVector v = new DenseIntVector(new int[] {1, 2, 3, 4, 5, 6});
        DefaultPermutationFunction func = new DefaultPermutationFunction();
        Vector permuted = func.permute(v, -1);
        Vector invPermuted = func.permute(permuted, 1);
        assertNotEquals(v, permuted);
        assertNotEquals(permuted, invPermuted);
        assertTrue(permuted instanceof IntegerVector);
        assertTrue(invPermuted instanceof IntegerVector);
        for (int i = 0; i < v.length(); ++i)
            assertEquals(v.get(i), invPermuted.getValue(i).intValue());
    }

    private void assertNotEquals(Object o1, Object o2) {
        assertFalse(o1.equals(o2));
    }
}
