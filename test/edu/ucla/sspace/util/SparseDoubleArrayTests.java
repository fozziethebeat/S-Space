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

package edu.ucla.sspace.util;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link SparseDoubleArray} 
 */
public class SparseDoubleArrayTests {

    @Test public void testConstructor() {
	new SparseDoubleArray();
    }

    @Test public void testArgConstructor() {
	new SparseDoubleArray(100);
	new SparseDoubleArray(0);
	new SparseDoubleArray(1);
    }

    @Test(expected=IllegalArgumentException.class) 
    public void testIllegalConstructor() {
	new SparseDoubleArray(-1);
    }

    @Test public void testAdd() {
	SparseDoubleArray arr = new SparseDoubleArray(100);
        arr.addPrimitive(5, 5);
        assertEquals(5, arr.get(5), 0.01d);
        arr.addPrimitive(5, 5);
        assertEquals(10, arr.get(5), 0.01d);
        arr.addPrimitive(5, -5);
        assertEquals(5, arr.get(5), 0.01d);
        arr.addPrimitive(5, -5);
        assertEquals(0, arr.get(5), 0.01d);
        arr.addPrimitive(5, -5);
        assertEquals(-5, arr.get(5), 0.01d);
    }

    @Test public void testGet() {
	int size = 1024;
	double[] control = new double[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = Math.random() * Integer.MAX_VALUE;
	}

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(i * 100, control[i]);
	}

	for (int i = 0; i < size; ++i) {
	    double v = arr.get(i * 100);
	    assertEquals(control[i], v, 0.001d);
	}
    }

    @Test public void testGetWithReZero() {
	int size = 1033;

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 1; i < size; ++i) {
	    arr.set(i, Double.valueOf(i));
	    assertEquals(i, arr.cardinality());
	}

	int original = arr.cardinality();

	for (int i = 1; i < size; i+=2) {
	    arr.set(i, 0d);
	    assertEquals(--original, arr.cardinality());
	    assertEquals(0d, arr.getPrimitive(i), 0.00d);
	}	
    }

    @Test public void toArray() {
	int size = 1024;
	Double[] control = new Double[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = Double.valueOf(i);
	}

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(i, control[i]);
	}
	
	Double[] test = new Double[size];
	arr.toArray(test);
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void toArrayWithZeros() {
	int size = 10;
	Double[] control = new Double[size];
	for (int i = 0; i < size; i++) {
	    control[i] = (i % 2 == 0) ? (double)i : 0d;
	}

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 0; i < size; i += 2) {
	    arr.set(i, (double)i);
	}
	
	Double[] test = new Double[size];
	arr.toArray(test);
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void toArraySubclass() {
	int size = 1024;
	Double[] control = new Double[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = Double.valueOf(i);
	}

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(i, control[i]);
	}
	
	Number[] test = new Number[size];
	arr.toArray(test);
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void testCardinality() {
	int size = 1033;

	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 1; i < size; ++i) {
	    arr.set(i, Double.valueOf(i));
	    assertEquals(i, arr.cardinality());
	}
    }

    @Test public void testIterator() {
        int size = 100;
	SparseDoubleArray arr = new SparseDoubleArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(i, (double)i);
	}
        
        // NOTE: start at 1 since the zero'th position has a 0 value
        int i = 1;
        for (DoubleEntry e : arr) {            
            assertEquals(i, e.index());
            assertEquals(i, e.value(), 0.001);
            i++;
        }
        assertEquals(i, size);
    }
}