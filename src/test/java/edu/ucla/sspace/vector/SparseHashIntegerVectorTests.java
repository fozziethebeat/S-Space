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

import edu.ucla.sspace.util.*;

import java.util.*;



/**
 * Tests for the {@link SparseHashIntegerVector} class.
 */
public class SparseHashIntegerVectorTests {

    @Test public void testSetZero() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        v.set(0, 0);
        assertEquals(0, v.getNonZeroIndices().length);
    }

    @Test public void testSetAddSumIsZero() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        assertEquals(0, v.getNonZeroIndices().length);
        v.add(0, 1);
        assertEquals(1, v.getNonZeroIndices().length);
        v.add(0, -1);
        assertEquals(0, v.getNonZeroIndices().length);
    }


    @Test public void testGetAndSet() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        v.set(1, 1);
        assertEquals(1, v.get(1));

        v.set(1, 2);
        assertEquals(2, v.get(1));

        v.set(2, 3);
        assertEquals(3, v.get(2));
    }

    @Test public void testGetAndSetZero() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        v.set(1, 1);
        assertEquals(1, v.get(1));

        v.set(1, 0);
        assertEquals(0, v.get(1));

        v.set(1, 2);
        assertEquals(2, v.get(1));
    }       

    @Test public void testNonZero() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        v.set(1, 1);
        int[] nz = v.getNonZeroIndices();
        assertEquals(1, nz.length);
        assertEquals(1, nz[0]);

        v.set(1, 0);
        nz = v.getNonZeroIndices();
        assertEquals(0, nz.length);

        v.set(1, 2);
        v.set(2, 2);
        nz = v.getNonZeroIndices();
        assertEquals(2, nz.length);
    }       

    @Test public void testIterator() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        Map<Integer,Integer> control = new HashMap<Integer,Integer>();
        Map<Integer,Integer> test = new HashMap<Integer,Integer>();
        for (int i = 20; i < 30; ++i) {
            v.set(i, i);
            control.put(i, i);
        }
        
        Iterator<IntegerEntry> iter = v.iterator();
        while (iter.hasNext()) {
            IntegerEntry e = iter.next();
            test.put(e.index(), e.value());
        }
        
        assertEquals(control, test);
    }       

    @Test public void testMagnitude() {
        SparseHashIntegerVector v = new SparseHashIntegerVector(100);
        assertEquals(0, v.magnitude(), .0001);

        v.set(1, 1);
        assertEquals(1, v.magnitude(), .0001);

        v.set(1, 3);
        v.set(2, 4);
        assertEquals(5, v.magnitude(), .0001);
    }
}