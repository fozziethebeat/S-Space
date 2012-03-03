/*
 * Copyright 2009 Keith Stevens 
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

import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link VectorMath} class
 */
public class VectorMathTest {

    @Test public void testIntegerVectorAdd() {
        IntegerVector a = new CompactSparseIntegerVector(100);
        a.set(1, 20);
        a.set(5, -1);
        IntegerVector b = new CompactSparseIntegerVector(100);
        b.set(2, 20);
        b.set(5, 1);
        IntegerVector c = VectorMath.add(a, b);
        assertEquals(20, c.get(1));
        assertEquals(20, c.get(2));
        assertEquals(0, c.get(5));
        assertSame(a, c);
    }

    @Test public void testTernaryVectorAdd() {
        IntegerVector a = new CompactSparseIntegerVector(100);
        a.set(1, 20);
        a.set(5, -1);
        IntegerVector b =
            new TernaryVector(100, new int[] {1, 2, 3, 4}, new int[] {7, 8, 9});
        IntegerVector c = VectorMath.add(a, b);
        assertEquals(21, c.get(1));
        assertEquals(-1, c.get(7));
        assertEquals(-1, c.get(5));
        assertSame(a, c);
    }

    @Test public void testDoubleVectorAdd() {
        DoubleVector a = new CompactSparseVector(100);
        a.set(1, 20);
        a.set(5, -1);
        DoubleVector b = new CompactSparseVector(100);
        b.set(2, 20);
        b.set(5, 1);
        DoubleVector c = VectorMath.add(a, b);
        assertEquals(20, c.get(1), 0);
        assertEquals(20, c.get(2), 0);
        assertEquals(0, c.get(5), 0);
        assertSame(a, c);
    }

    @Test public void testIntegerVectorAddUnmodified() {
        IntegerVector a = new CompactSparseIntegerVector(100);
        a.set(1, 20);
        a.set(5, -1);
        IntegerVector b = new CompactSparseIntegerVector(100);
        b.set(2, 20);
        b.set(5, 1);
        IntegerVector c = VectorMath.addUnmodified(a, b);
        assertEquals(20, c.get(1));
        assertEquals(20, c.get(2));
        assertEquals(0, c.get(5));
        assertNotSame(a, c);
    }

    @Test public void testDoubleVectorAddUnmodified() {
        DoubleVector a = new CompactSparseVector(100);
        a.set(1, 20);
        a.set(5, -1);
        DoubleVector b = new CompactSparseVector(100);
        b.set(2, 20);
        b.set(5, 1);
        DoubleVector c = VectorMath.addUnmodified(a, b);
        assertEquals(20, c.get(1), 0);
        assertEquals(20, c.get(2), 0);
        assertEquals(0, c.get(5), 0);
        assertNotSame(a, c);
    }

    @Test public void testCompactAdd() {
        DoubleVector a = new CompactSparseVector(10000);
        SparseDoubleVector b = new CompactSparseVector(10000);
        Random r = new Random(5);
        for (int i = 0; i < 200; ++i)
            b.set(r.nextInt(10000), 1);
        VectorMath.add(a, b);
    }

    @Test public void testCompactAdd2() {
        DoubleVector a = new CompactSparseVector(10000);
        SparseDoubleVector b = new CompactSparseVector(10000);
        Random r = new Random(5);
        for (int i = 0; i < 200; ++i)
            b.set(r.nextInt(10000), 1);
        int[] nonZeros = b.getNonZeroIndices();
        for (int i : nonZeros)
            a.add(i, b.get(i));
    }

    @Test public void testMultiplyUnmodified() {
        SparseDoubleVector v = new CompactSparseVector();
        v.set(1, 10);
        v.set(4, 10);
        v.set(10, 10);
        SparseDoubleVector w = new CompactSparseVector();
        w.set(1, 2);
        w.set(10, 3);
        w.set(5, 10);

        SparseDoubleVector u = VectorMath.multiplyUnmodified(v, w);
        assertEquals(3, v.getNonZeroIndices().length);
        assertEquals(10, v.get(1), .00001);
        assertEquals(10, v.get(4), .00001);
        assertEquals(10, v.get(10), .00001);
        assertEquals(3, w.getNonZeroIndices().length);
        assertEquals(2, w.get(1), .00001);
        assertEquals(3, w.get(10), .00001);
        assertEquals(10, w.get(5), .00001);
        assertEquals(2, u.getNonZeroIndices().length);
        assertEquals(20, u.get(1), .00001);
        assertEquals(30, u.get(10), .00001);
    }
}
