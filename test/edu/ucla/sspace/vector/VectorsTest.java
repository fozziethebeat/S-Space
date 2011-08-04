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

package edu.ucla.sspace.vector;


import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link Vectors} class
 */
public class VectorsTest {

    @Test public void testInstanceOf() {
        CompactSparseIntegerVector siv = new CompactSparseIntegerVector(100);
        CompactSparseIntegerVector sivCopy = Vectors.instanceOf(siv);
        assertEquals(siv.length(), sivCopy.length());

        CompactSparseVector csv = new CompactSparseVector(100);
        CompactSparseVector csvCopy = Vectors.instanceOf(csv);
        assertEquals(csv.length(), csvCopy.length());
    }

    @Test public void testIntegerCopyOf() {
        IntegerVector a = new CompactSparseIntegerVector(5);
        a.set(1, -123);
        IntegerVector b = Vectors.copyOf(a);
        assertTrue(b instanceof SparseIntegerVector);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.get(1));
        assertNotSame(a, b);

        a = new DenseIntVector(new int[] {1, 2, 4, 5});
        b = Vectors.copyOf(a);
        assertTrue(b instanceof DenseIntVector);
        assertEquals(a.length(), b.length());
        for (int i = 0; i < b.length(); ++i)
            assertEquals(a.get(i), b.get(i));
        assertNotSame(a, b);

    }

    @Test public void testDoubleCopyOf() {
        DoubleVector a = new CompactSparseVector(5);
        a.set(1, -123);
        DoubleVector b = Vectors.copyOf(a);
        assertTrue(b instanceof CompactSparseVector);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.get(1), 0);
        assertNotSame(a, b);

        a = new DenseVector(new double[] {1, 2, 4, 5});
        b = Vectors.copyOf(a);
        assertTrue(b instanceof DenseVector);
        assertEquals(a.length(), b.length());
        for (int i = 0; i < b.length(); ++i)
            assertEquals(a.get(i), b.get(i), 0);
        assertNotSame(a, b);
    }

    @Test public void testGenericCopyOf() {
        Vector a = new CompactSparseVector(5);
        a.set(1, -123);
        Vector b = Vectors.copyOf(a);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.getValue(1).doubleValue(), 0);
        assertNotSame(a, b);

        a = new CompactSparseIntegerVector(5);
        a.set(1, -123);
        b = Vectors.copyOf(a);
        assertEquals(a.length(), b.length());
        assertEquals(-123, b.getValue(1).doubleValue(), 0);
        assertNotSame(a, b);
    }

    @Test public void testIntegerEquals() {
        IntegerVector v1 = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        IntegerVector v2 = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        IntegerVector v3 = new DenseIntVector(new int[] {1, 1, 2, 3, 4 });

        assertTrue(Vectors.equals(v1, v2));
        assertFalse(Vectors.equals(v1, v3));
    }

    @Test public void testDoubleEquals() {
        DoubleVector v1 = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        DoubleVector v2 = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        DoubleVector v3 = new DenseVector(new double[] {1, 1, 2, 3, 4 });
        IntegerVector iv1 = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        DoubleVector v4 = Vectors.asDouble(iv1);

        assertTrue(Vectors.equals(v1, v2));
        assertFalse(Vectors.equals(v1, v3));
        assertTrue(Vectors.equals(v1, v4));
    }

    @Test public void testIntegerSubview() {
        IntegerVector v = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        IntegerVector sub = Vectors.subview(v, 1, 3);
        assertEquals(1, sub.get(0));
        assertEquals(2, sub.get(1));
        assertEquals(3, sub.get(2));
        assertEquals(3, sub.length());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testDoubleVectorImmutable() {
        DoubleVector v = new DenseVector(new double[] {1, 2, 3, 4, 5});
        DoubleVector r = Vectors.immutable(v);
        r.set(1, 10);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testIntegerVectorImmutable() {
        IntegerVector v = new DenseIntVector(new int[] {1, 2, 3, 4, 5});
        IntegerVector r = Vectors.immutable(v);
        r.set(1, 10);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSparseDoubleVectorImmutable() {
        SparseDoubleVector v = new CompactSparseVector(10);
        v.set(8, 10);
        SparseDoubleVector r = Vectors.immutable(v);
        r.set(1, 10);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testSparseIntegerVectorImmutable() {
        SparseIntegerVector v = new CompactSparseIntegerVector(10);
        v.set(8, 10);
        SparseIntegerVector r = Vectors.immutable(v);
        r.set(1, 10);
    }

    @Test(expected=IllegalArgumentException.class)
        public void testIntegerSubviewLength() {
        IntegerVector v = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, 0, 10);
    }

    @Test(expected=IllegalArgumentException.class)
        public void testIntegerSubviewOffset() {
        IntegerVector v = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, -10, 10);
    }

    @Test(expected=IllegalArgumentException.class)
        public void testIntegerSubviewOffsetAndLength() {
        IntegerVector v = new DenseIntVector(new int[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, 3, 3);
    }

    @Test public void testDoubleSubview() {
        DoubleVector v = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        DoubleVector sub = Vectors.subview(v, 1, 3);
        assertEquals(1, sub.get(0), .1);
        assertEquals(2, sub.get(1), .1);
        assertEquals(3, sub.get(2), .1);
        assertEquals(3, sub.length());
    }

    @Test(expected=IllegalArgumentException.class)
        public void testDoubleSubviewLength() {
        DoubleVector v = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, 0, 10);
    }

    @Test(expected=IllegalArgumentException.class)
        public void testDoubleSubviewOffset() {
        DoubleVector v = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, -10, 10);
    }

    @Test(expected=IllegalArgumentException.class)
        public void testDoubleSubviewOffsetAndLength() {
        DoubleVector v = new DenseVector(new double[] {0, 1, 2, 3, 4 });
        Vectors.subview(v, 3, 3);
    }
    
}
