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

package edu.ucla.sspace.vector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class ScaledSparseDoubleVectorTest {

    @Test public void testLength() {
        SparseDoubleVector v = 
            new CompactSparseVector(new double[] {4, 4, 4, 4});
        v = new ScaledSparseDoubleVector(v, 5);
        assertEquals(4, v.length());
    }

    @Test public void testAdd() {
        SparseDoubleVector v =
            new CompactSparseVector(new double[] {4, 4, 4, 4});
        v = new ScaledSparseDoubleVector(v, 5);

        v.add(0, 1);
        assertEquals(21, v.get(0), .0001);

        v.add(2, 5);
        assertEquals(25, v.get(2), .0001);

        v.add(2, -4);
        assertEquals(21, v.get(2), .0001);

        v.add(3, 2);
        assertEquals(22, v.get(3), .0001);
    }

    @Test public void testSetAndGet() {
        SparseDoubleVector v =
            new CompactSparseVector(new double[] {4, 4, 4, 4});
        v = new ScaledSparseDoubleVector(v, 5);

        v.add(0, 1);
        assertEquals(21, v.get(0), .0001);

        v.add(2, 5);
        assertEquals(25, v.get(2), .0001);

        v.add(2, -4);
        assertEquals(21, v.get(2), .0001);

        v.add(3, 2);
        assertEquals(22, v.get(3), .0001);
    }

    @Test public void testDoubleScale() {
        SparseDoubleVector b = new CompactSparseVector(
                new double[] {4, 4, 4, 4});
        ScaledSparseDoubleVector v = new ScaledSparseDoubleVector(b, 5);
        v = new ScaledSparseDoubleVector(v, 6);

        assertEquals(b, v.getBackingVector());
        assertEquals(30, v.getScalar(), .0001);

        assertEquals(4*30, v.get(0), .001);
        assertEquals(4*30, v.get(1), .001);
        assertEquals(4*30, v.get(2), .001);
        assertEquals(4*30, v.get(3), .001);
    }

    @Test public void testMagnitude() {
        SparseDoubleVector v = 
            new CompactSparseVector(new double[] {1, 2, 3, 1});
        v = new ScaledSparseDoubleVector(v, 5);
        assertEquals(Math.sqrt(35), v.magnitude(), .0001);
    }
}
