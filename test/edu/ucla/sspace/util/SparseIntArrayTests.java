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
 * A collection of unit tests for {@link IntegerMap} 
 */
public class SparseIntArrayTests {

    @Test public void testConstructor() {
	new SparseIntArray();
    }

    @Test public void testArgConstructor() {
	new SparseIntArray(100);
	new SparseIntArray(0);
	new SparseIntArray(1);
    }

    @Test(expected=IllegalArgumentException.class) 
    public void testIllegalConstructor() {
	new SparseIntArray(-1);
    }

    @Test public void testGet() {
	int size = 1024;
	int[] control = new int[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = (int)(Math.random() * Integer.MAX_VALUE);	    
	}

	SparseIntArray arr = new SparseIntArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(control[i], i);
	}

	for (int i = 0; i < size; ++i) {
	    int v = arr.get(control[i]);
	    assertEquals(i, v);
	}
    }

    @Test public void testGetWithReZero() {
	int size = 1033;

	SparseIntArray arr = new SparseIntArray();
	for (int i = 1; i < size; ++i) {
	    arr.set(i, i);
	    assertEquals(i, arr.cardinality());
	}

	int original = arr.cardinality();

	for (int i = 1; i < size; i+=2) {
	    arr.set(i, 0);
	    assertEquals(--original, arr.cardinality());
	    assertEquals(0, arr.getPrimitive(i));
	}	
    }

    @Test public void toArray() {
	int size = 1024;
	Integer[] control = new Integer[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = i;
	}

	SparseIntArray arr = new SparseIntArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(control[i], i);
	}
	
	Integer[] test = new Integer[size];
	arr.toArray(test);
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void toArrayWithZeros() {
	int size = 10;
	Integer[] control = new Integer[size];
	for (int i = 0; i < size; i++) {
	    control[i] = (i % 2 == 0) ? i : 0;
	}

	SparseIntArray arr = new SparseIntArray();
	for (int i = 0; i < size; i += 2) {
	    arr.set(control[i], i);
	}
	
	Integer[] test = new Integer[size];
	arr.toArray(test);
	System.out.println(Arrays.toString(control));
	System.out.println(Arrays.toString(test));
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void toArraySubclass() {
	int size = 1024;
	Integer[] control = new Integer[size];
	for (int i = 0; i < size; ++i) {
	    control[i] = i;
	}

	SparseIntArray arr = new SparseIntArray();
	for (int i = 0; i < size; ++i) {
	    arr.set(control[i], i);
	}
	
	Number[] test = new Number[size];
	arr.toArray(test);
	assertTrue(Arrays.equals(control, test));
    }

    @Test public void testCardinality() {
	int size = 1033;

	SparseIntArray arr = new SparseIntArray();
	for (int i = 1; i < size; ++i) {
	    arr.set(i, i);
	    assertEquals(i, arr.cardinality());
	}
    }
    
}