/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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
public class DenseDynamicMagnitudeVectorTest {

    @Test public void testLength() {
        DoubleVector v = new DenseDynamicMagnitudeVector(4);
        assertEquals(4, v.length());

        v = new DenseDynamicMagnitudeVector(new double[] {4, 4, 4, 4});
        assertEquals(4, v.length());

        v = new DenseVector(new double[] {4, 4, 4, 4});
        v = new DenseDynamicMagnitudeVector(v);
        assertEquals(4, v.length());
    }

    @Test public void testAdd() {
        DoubleVector v = new DenseDynamicMagnitudeVector(4);

        assertEquals(0, v.magnitude(), .0001);

        v.add(0, 1);
        assertEquals(1, v.get(0), .0001);
        assertEquals(1, v.magnitude(), .0001);

        v.add(2, 5);
        assertEquals(5, v.get(2), .0001);
        assertEquals(Math.sqrt(26), v.magnitude(), .0001);

        v.add(2, -4);
        assertEquals(1, v.get(2), .0001);
        assertEquals(Math.sqrt(2), v.magnitude(), .0001);

        v.add(3, 2);
        assertEquals(2, v.get(3), .0001);
        assertEquals(Math.sqrt(6), v.magnitude(), .0001);
    }

    @Test public void testSetAndGet() {
        DoubleVector v = new DenseDynamicMagnitudeVector(4);

        assertEquals(0, v.magnitude(), .0001);

        v.set(0, 1);
        assertEquals(1, v.get(0), .0001);
        assertEquals(1, v.magnitude(), .0001);

        v.set(2, 5);
        assertEquals(5, v.get(2), .0001);
        assertEquals(Math.sqrt(26), v.magnitude(), .0001);

        v.set(2, 1);
        assertEquals(1, v.get(2), .0001);
        assertEquals(Math.sqrt(2), v.magnitude(), .0001);

        v.set(3, 2);
        assertEquals(2, v.get(3), .0001);
        assertEquals(Math.sqrt(6), v.magnitude(), .0001);
    }
}
