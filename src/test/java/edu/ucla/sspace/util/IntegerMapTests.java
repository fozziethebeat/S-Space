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
public class IntegerMapTests {

    
    @Test public void testConstructor() {
 	IntegerMap<String> m = new IntegerMap<String>();
    }

//     @Test public void testMapConstructor() {

//     }    

//     @Test(expected=NullPointerException.class) 
//     public void testMapConstructorNull() {
// 	IntegerMap<String> m = new IntegerMap<String>(null);
//     }

    @Test public void testPut() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	String s = m.get(1);
	assertEquals("1", s);
    }

    @Test(expected=NullPointerException.class) 
    public void testPutNullKey() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(null, "value");
    }

    public void testPutNullValue() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, null);
	String s = m.get(0);
	assertEquals(null, s);
    }

    @Test public void testPutConflict() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	m.put(1, "2");
	String s = m.get(1);
	assertEquals("2", s);
    }

    @Test public void testPutNegative() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(-1, "-1");
	m.put(1, "1");

	assertEquals("-1", m.get(-1));
	assertEquals("1", m.get(1));
    }

    @Test public void testPutLotsOfRandomInts() {
	IntegerMap<String> m = new IntegerMap<String>();
	assertEquals(0, m.size());
	int size = 10240;

	Set<Integer> s = new HashSet<Integer>();
	while (s.size() < size) {
	    s.add((int)(Math.random() * Integer.MAX_VALUE));
	}
	
	int c = 1;
	for (Integer i : s) {
	    String w = i.toString();
	    m.put(i, w);
	    assertEquals(c, m.size());
	    c++;
	}
	
	Set<Integer> keySet = m.keySet();
	Collection<String> values = m.values();

	assertEquals(s.size(), keySet.size());
	assertTrue(s.containsAll(keySet));
	assertTrue(keySet.containsAll(s));

	// check mapping
	for (Map.Entry<Integer,String> e : m.entrySet()) {
	    int i = e.getKey().intValue();
	    String str = e.getValue();
	    int j = Integer.parseInt(str);
	    assertEquals(i, j);
	}
    }

    @Test public void testProgressive() {

	IntegerMap<String> m = new IntegerMap<String>();
	assertEquals(0, m.size());
	int size = 1024;

	for (int i = 0; i < size; ++i) {
	    
	    int k = (int)(Integer.MAX_VALUE * Math.random());

	    m.put(k, Integer.valueOf(k).toString());
	    
	    // check mapping
	    for (Map.Entry<Integer,String> e : m.entrySet()) {
		int p = e.getKey().intValue();
		String s = e.getValue();
		int q = Integer.parseInt(s);
		assertEquals(p, q);
	    }
	}
    }

    @Test public void testContainsKey() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	m.put(2, "2");

	assertTrue(m.containsKey(1));
	assertTrue(m.containsKey(2));
    }

    @Test public void testContainsKeyFalse() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	m.put(2, "2");

	assertFalse(m.containsKey(3));
	assertFalse(m.containsKey(4));
	assertFalse(m.containsKey(-1));
	assertFalse(m.containsKey(0));
	assertFalse(m.containsKey(-2));
    }

    @Test public void testContainsValue() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	m.put(2, "2");

	assertTrue(m.containsValue("1"));
	assertTrue(m.containsValue("2"));
    }

    @Test public void testContainsValueFalse() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(1, "1");
	m.put(2, "2");

	assertFalse(m.containsValue("3"));
	assertFalse(m.containsValue("4"));
	assertFalse(m.containsValue("5"));
    }

    @Test public void testKeySet() {
	IntegerMap<String> m = new IntegerMap<String>();

	m.put(1, "0");
	m.put(2, "1");
	m.put(3, "2");
	m.put(4, "3");
	m.put(5, "4");

	Set<Integer> test = m.keySet();
	
	assertTrue(test.contains(1));
	assertTrue(test.contains(2));
	assertTrue(test.contains(3));
	assertTrue(test.contains(4));
	assertTrue(test.contains(5));
	assertFalse(test.contains(0));
	assertFalse(test.contains(6));
	assertFalse(test.contains(-1));
    }

//     @Test public void testEntrySetValue() {
// 	IntegerMap<String> m = new IntegerMap<String>();

//     }


    @Test public void testKeyIterator() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, "0");
	m.put(1, "1");
	m.put(2, "2");

	Set<Integer> control = new HashSet<Integer>();
	control.add(0);
	control.add(1);
	control.add(2);

	Set<Integer> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test public void testValueIterator() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, "0");
	m.put(1, "1");
	m.put(2, "2");

	Set<String> control = new HashSet<String>();
	control.add("0");
	control.add("1");
	control.add("2");

	Collection<String> test = m.values();
	
	assertEquals(control.size(), test.size());
	for (String s : test) {	    
	    assertTrue(control.contains(s));
	}
    }

    @Test public void testIteratorHasNext() {
	
	IntegerMap<String> m = new IntegerMap<String>();
	Iterator<Integer> it = m.keySet().iterator();
	assertFalse(it.hasNext());

	m.put(0, "0");
	m.put(1, "1");
	m.put(2, "2");

	it = m.keySet().iterator();

	Set<Integer> control = new HashSet<Integer>();

	while (it.hasNext()) {
	    control.add(it.next());
	}

	Set<Integer> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test(expected=NoSuchElementException.class)
    public void testIteratorNextError() {
	
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, "0");

	Iterator<Integer> it = m.keySet().iterator();
	it.next();
	it.next(); // error
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyIntIteratorNextError() {
	
	IntegerMap<String> m = new IntegerMap<String>();

	Iterator<Integer> it = m.keySet().iterator();
	it.next(); // error
    }

//     @Test public void testIteratorRemove() {
	
// 	IntegerMap<String> m = new IntegerMap<String>();
//     }

//     @Test(expected=IllegalStateException.class) 
//     public void testIteratorRemoveTwice() {
	
// 	IntegerMap<String> m = new IntegerMap<String>();
// 	m.put(0, "0");
// 	assertTrue(m.containsKey(0));

// 	Iterator<Integer> it = m.keySet().iterator();
// 	it.next();
// 	it.remove();
// 	it.remove();
//     }

    @Test public void testRemove() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, "0");
	assertTrue(m.containsKey(0));

	m.remove(0);
	assertFalse(m.containsKey(0));
	assertEquals(0, m.size());
    }

    @Test public void testMultipleRemoves() {
	IntegerMap<String> m = new IntegerMap<String>();
	Set<Integer> control = new HashSet<Integer>();
	
	LinkedList<Integer> list = new LinkedList<Integer>();
	for (int i = 0; i < 512; ++i) {
	    list.add(i);
	}

	for (Integer i : list) {
	    m.put(i, i.toString());
	    control.add(i);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));

	// remove half
	int half = control.size();
	for (int i = 0; i < half; ++i) {
	    Integer j = list.poll();
	    control.remove(j);
	    m.remove(j);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));	
    }    

    @Test public void testSize() {
	IntegerMap<String> m = new IntegerMap<String>();
	assertEquals(0, m.size());
	m.put(0, "0");
	assertEquals(1, m.size());
	m.put(1, "1");
	assertEquals(2, m.size());
	m.put(2, "2");
	assertEquals(3, m.size());
    }

    @Test public void testIsEmpty() {
	IntegerMap<String> m = new IntegerMap<String>();
	assertTrue(m.isEmpty());

	m.put(0, "0");
	assertFalse(m.isEmpty());
	
	m.clear();
	assertTrue(m.isEmpty());
    }

    @Test public void testClear() {
	IntegerMap<String> m = new IntegerMap<String>();
	m.put(0, "0");
	m.put(1, "1");
	m.put(2, "2");

	m.clear();
	assertEquals(0, m.size());
	assertFalse(m.containsKey(0));
	assertFalse(m.containsKey(1));
	assertFalse(m.containsKey(2));
    }

    public static void main(String args[]) {
	org.junit.runner.JUnitCore.main(IntegerMapTests.class.getName());
    }

}