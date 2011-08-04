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

package edu.ucla.sspace.hal;

import edu.ucla.sspace.vector.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link ConcatenatedSparseDoubleVectorTests}
 * class
 */
public class ConcatenatedSparseDoubleVectorTests {

    @Test public void testSparseIndices() {
        SparseDoubleVector v1 = new SparseHashDoubleVector(100);
        SparseDoubleVector v2 = new SparseHashDoubleVector(100);
        v1.set(0, 10);
        v1.set(50, 10);
        v2.set(0, 10);
        v2.set(50, 10);

        SparseDoubleVector concat = new ConcatenatedSparseDoubleVector(v1, v2);
        int[] indices = concat.getNonZeroIndices();
        assertEquals(4, indices.length);
        assertEquals(0, indices[0]);
        assertEquals(50, indices[1]);
        assertEquals(100, indices[2]);
        assertEquals(150, indices[3]);
    }
    
    @Test public void testGet() {
        SparseDoubleVector v1 = new SparseHashDoubleVector(100);
        SparseDoubleVector v2 = new SparseHashDoubleVector(100);
        v1.set(0, 10);
        v1.set(50, 10);
        v2.set(0, 10);
        v2.set(50, 10);

        SparseDoubleVector concat = new ConcatenatedSparseDoubleVector(v1, v2);
        assertEquals(10, concat.get(0), 0.1d);
        assertEquals(10, concat.get(50), 0.1d);
        assertEquals(10, concat.get(100), 0.1d);
        assertEquals(10, concat.get(150), 0.1d);
        assertEquals(0, concat.get(101), 0.1d);
        assertEquals(0, concat.get(151), 0.1d);
        assertEquals(0, concat.get(99), 0.1d);
        assertEquals(0, concat.get(149), 0.1d);
    }
}