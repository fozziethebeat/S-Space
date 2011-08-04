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
 * A collection of unit tests for {@link TrieMap} 
 */
public class TrieMapTests {

    
    @Test public void testConstructor() {
 	TrieMap<String> m = new TrieMap<String>();
    }

    @Test public void testMapConstructor() {

	Map<String,String> control = new HashMap<String,String>();
	
	for (int i = 0; i < 512; ++i) {
	    String s = Integer.toHexString(i);
	    control.put(s, s);
	}

	TrieMap<String> test = new TrieMap<String>(control);

	assertEquals(control.size(), test.size());
	assertTrue(control.keySet().containsAll(test.keySet()));
	assertTrue(control.values().containsAll(test.values()));
    }    

    @Test(expected=NullPointerException.class) 
    public void testMapConstructorNull() {
	TrieMap<String> m = new TrieMap<String>(null);
    }

    @Test public void testPut() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "1");
	String s = m.get("a");
	assertEquals("1", s);
    }

    @Test(expected=NullPointerException.class) 
    public void testPutNullKey() {
	TrieMap<String> m = new TrieMap<String>();
	m.put(null, "value");
    }

    @Test(expected=NullPointerException.class) 
    public void testPutNullValue() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("key", null);
    }

    @Test public void testPutEmptyString() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("", "empty");
	assertEquals("empty", m.get(""));
    }

    @Test public void testPutDifferentFirstChars() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "1");
	m.put("ac", "2");
	m.put("b", "3");
	m.put("bd", "4");

	assertEquals("1", m.get("a"));
	assertEquals("2", m.get("ac"));
	assertEquals("3", m.get("b"));
	assertEquals("4", m.get("bd"));
    }

    @Test public void testPutConflict() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "1");
	m.put("a", "2");
	String s = m.get("a");
	assertEquals("2", s);
    }

    @Test public void testPutSubstringOfKey() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertEquals("1", m.get("catapult"));
	assertEquals("2", m.get("cat"));
    }

    @Test public void testPutKeyIsLonger() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	m.put("catapult", "2");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("catapult"));
    }
    
    @Test public void testPutKeyIsLongerByOne() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	m.put("cats", "2");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("cats"));
    }

    @Test public void testPutKeysLongerByOne() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "1");
	assertEquals("1", m.get("cat"));
	m.put("catx", "2");
	assertEquals("2", m.get("catx"));
	m.put("catxy", "3");
	assertEquals("3", m.get("catxy"));
	m.put("catxyz", "4");

	assertEquals("1", m.get("cat"));
	assertEquals("2", m.get("catx"));
	assertEquals("3", m.get("catxy"));
	assertEquals("4", m.get("catxyz"));
    }


    @Test public void testPutKeysShorterByOne() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catxyz", "4");
	assertEquals("4", m.get("catxyz"));
	m.put("catxy", "3");
	assertEquals("3", m.get("catxy"));
	m.put("catx", "2");
	assertEquals("2", m.get("catx"));
	m.put("cat", "1");
	assertEquals("1", m.get("cat"));
	
	assertEquals("2", m.get("catx"));
	assertEquals("3", m.get("catxy"));
	assertEquals("4", m.get("catxyz"));
    }

    @Test public void testPutKeysDiverge() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("category", "1");
	m.put("catamaran", "2");

	assertEquals("1", m.get("category"));
	assertEquals("2", m.get("catamaran"));
    }

    @Test public void testPutKeysConflictAndDiverge() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	assertEquals("0", m.get("cat"));
	assertEquals("1", m.get("category"));
	assertEquals("2", m.get("catamaran"));
    }


    @Test public void testPutWithSubstringAndDiverge() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	m.put("cat", "cat");
	m.put("category", "category");
	m.put("catamaran", "catamaran");
	
	assertEquals("cat", m.get("cat"));
	assertEquals("category", m.get("category"));
	assertEquals("catamaran", m.get("catamaran"));
    }


    @Test public void testPutKeysReverseOrdering() {
	TrieMap<String> m = new TrieMap<String>();

	m.put("coconut", "2");
	m.put("banana", "1");
	m.put("apple", "0");

	assertEquals(3, m.size());
    }

    @Test public void testPutStringKeysRandomOrder() {
	TrieMap<String> m = new TrieMap<String>();

	String[] arr = new String[] {"apple", "banana", "coconut", "daffodol",
				     "edamame", "fig", "hummus", "grapes" };
	Collections.shuffle(Arrays.asList(arr));

	for (String s : arr) {
	    m.put(s, s);
	}
	    	
	assertEquals(arr.length, m.size());
    }

    @Test public void testPutLotsOfRandomPrefixes() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());
	int size = 10240;
	//int[] a = new int[size];
	Set<Integer> s = new HashSet<Integer>();
	while (s.size() < size) {
	    s.add((int)(Math.random() * Integer.MAX_VALUE));
	}
	
	int c = 1;
	for (Integer i : s) {
	    String w = i.toString();
	    m.put(w,w);
	    assertEquals(c, m.size());
	    c++;
	}

	for (Map.Entry<String,String> e : m.entrySet()) {
	    assertEquals(Integer.parseInt(e.getKey().toString()),
			 Integer.parseInt(e.getValue()));
	}
    }

    @Test public void testPutLotsOfOrderedPrefixes() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());

	for (int i = 0; i < 10240; ++i) {
	    String s = String.valueOf(i);
	    m.put(s,s);
	    assertEquals(i+1, m.size());
	}

	for (Map.Entry<String,String> e : m.entrySet()) {
	    assertEquals(Integer.parseInt(e.getKey().toString()),
			 Integer.parseInt(e.getValue()));
	}
    }

    @Test public void testPutNumberKeysRandomOrder() {
	TrieMap<String> m = new TrieMap<String>();
	Set<String> control = new HashSet<String>();
	
	ArrayList<String> list = new ArrayList<String>();
	for (int i = 0; i < 512; ++i) {
	    String s = Integer.toHexString(i);
	    list.add(s);
	}
	Collections.shuffle(list);

	for (String s : list) {
	    m.put(s, s);
	    control.add(s);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));
    }    

    @Test public void testUnknown() {
	TrieMap<String> m = new TrieMap<String>();
	String[] arr = new String[] { "1cd", "1c", "1" };	
	Set<String> control = new HashSet<String>();
	for (String s : arr) {
	    m.put(s, s);
	    control.add(s);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));
    }
    

    @Test public void testContainsKey() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertTrue(m.containsKey("catapult"));
	assertTrue(m.containsKey("cat"));
    }

    @Test public void testContainsKeyFalse() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertFalse(m.containsKey("dog"));
	assertFalse(m.containsKey("c"));
	assertFalse(m.containsKey("ca"));
	assertFalse(m.containsKey("cats"));
	assertFalse(m.containsKey("catapul"));
    }

    @Test public void testContainsValue() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertTrue(m.containsValue("1"));
	assertTrue(m.containsValue("2"));
    }

    @Test public void testContainsValueFalse() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("catapult", "1");
	m.put("cat", "2");

	assertFalse(m.containsValue("catapult"));
	assertFalse(m.containsValue("c"));
	assertFalse(m.containsValue("cat"));
    }

    @Test public void testKeySet() {
	TrieMap<String> m = new TrieMap<String>();

	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");
	m.put("apple", "3");
	m.put("banana", "4");

	Set<String> test = m.keySet();
	
	assertTrue(test.contains("apple"));
	assertTrue(test.contains("banana"));
	assertTrue(test.contains("cat"));
	assertTrue(test.contains("category"));
	assertTrue(test.contains("catamaran"));
    }

    @Test public void testEntrySetValue() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");
	m.put("apple", "3");
	m.put("banana", "4");

	Set<Map.Entry<String,String>> test = m.entrySet();

	for (Map.Entry<String,String> e : m.entrySet()) {
	    e.setValue(e.getKey().toString());
	}
	
	assertEquals("cat", m.get("cat"));
	assertEquals("category", m.get("category"));
	assertEquals("catamaran", m.get("catamaran"));
	assertEquals("apple", m.get("apple"));
	assertEquals("banana", m.get("banana"));
    }

    @Test(expected=NullPointerException.class) 
    public void testEntrySetNullValue() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("key", "value");
	Set<Map.Entry<String,String>> test = m.entrySet();
	for (Map.Entry<String,String> e : test) {
	    e.setValue(null);
	}
	
    }

    @Test public void testKeyIterator() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	Set<String> control = new HashSet<String>();
	control.add("cat");
	control.add("category");
	control.add("catamaran");

	Set<String> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test public void testValueIterator() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

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
	
	TrieMap<String> m = new TrieMap<String>();
	Iterator<String> it = m.keySet().iterator();
	assertFalse(it.hasNext());

	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	it = m.keySet().iterator();

	Set<String> control = new HashSet<String>();

	while (it.hasNext()) {
	    control.add(it.next());
	}

	Set<String> test = m.keySet();

	assertTrue(test.containsAll(control));
	assertTrue(control.containsAll(test));
    }

    @Test(expected=NoSuchElementException.class)
    public void testIteratorNextError() {
	
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");

	Iterator<String> it = m.keySet().iterator();
	it.next();
	it.next(); // error
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyTrieIteratorNextError() {
	
	TrieMap<String> m = new TrieMap<String>();

	Iterator<String> it = m.keySet().iterator();
	it.next(); // error
    }

    @Test public void testIteratorRemove() {
	
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	assertTrue(m.containsKey("cat"));

	Iterator<String> it = m.keySet().iterator();
	it.next();
	it.remove();
	assertFalse(m.containsKey("cat"));
    }

    @Test(expected=IllegalStateException.class) 
    public void testIteratorRemoveTwice() {
	
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	assertTrue(m.containsKey("cat"));

	Iterator<String> it = m.keySet().iterator();
	it.next();
	it.remove();
	it.remove();
    }

    @Test public void testRemove() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	assertTrue(m.containsKey("cat"));

	m.remove("cat");
	assertFalse(m.containsKey("cat"));
	assertEquals(0, m.size());
    }

    @Test public void testRemoveEmptyString() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("", "empty");
	assertEquals("empty", m.get(""));
	m.remove("");
	assertFalse(m.containsKey(""));
    }

    @Test public void testRemoveIntermediate() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "cat");
	m.put("cats", "cats");
	assertTrue(m.containsKey("cat"));

	m.remove("cat");
	assertFalse(m.containsKey("cat"));
	assertTrue(m.containsKey("cats"));
	assertEquals(1, m.size());
    }

    @Test public void testRemoveTerminal() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "cat");
	m.put("cats", "cats");
	assertTrue(m.containsKey("cats"));

	m.remove("cats");
	assertFalse(m.containsKey("cats"));
	assertTrue(m.containsKey("cat"));
	assertEquals(1, m.size());
    }

    @Test public void testMultipleRemoves() {
	TrieMap<String> m = new TrieMap<String>();
	Set<String> control = new HashSet<String>();
	
	LinkedList<String> list = new LinkedList<String>();
	for (int i = 0; i < 512; ++i) {
	    String s = Integer.toHexString(i);
	    list.add(s);
	}

	for (String s : list) {
	    m.put(s, s);
	    control.add(s);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));

	// remove half
	int half = control.size();
	for (int i = 0; i < half; ++i) {
	    String s = list.poll();
	    control.remove(s);
	    m.remove(s);
	}

	assertEquals(control.size(), m.size());
	assertTrue(control.containsAll(m.keySet()));	
    }    

    @Test public void testSize() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());
	m.put("cat", "0");
	assertEquals(1, m.size());
	m.put("category", "1");
	assertEquals(2, m.size());
	m.put("catamaran", "2");
	assertEquals(3, m.size());
    }

    @Test public void testSizeLotsOfPrefixes() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());
	for (int i = 0; i < 10240; ++i) {
	    String s = String.valueOf(i);
	    m.put(s,s);
	    assertEquals(i+1, m.size());
	}
    }

    @Test public void testSizeLotsOfRandomPrefixes() {
	TrieMap<String> m = new TrieMap<String>();
	assertEquals(0, m.size());
	int size = 10240;
	//int[] a = new int[size];
	Set<Integer> s = new HashSet<Integer>();
	while (s.size() < size) {
	    s.add((int)(Math.random() * Integer.MAX_VALUE));
	}
	
	int c = 1;
	for (Integer i : s) {
	    String w = i.toString();
	    m.put(w,w);
	    assertEquals(c, m.size());
	    c++;
	}
    }

    @Test public void testIsEmpty() {
	TrieMap<String> m = new TrieMap<String>();
	assertTrue(m.isEmpty());

	m.put("cat", "0");
	assertFalse(m.isEmpty());
	
	m.clear();
	assertTrue(m.isEmpty());
    }

    @Test public void testClear() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("cat", "0");
	m.put("category", "1");
	m.put("catamaran", "2");

	m.clear();
	assertEquals(0, m.size());
	assertFalse(m.containsKey("cat"));
	assertFalse(m.containsKey("category"));
	assertFalse(m.containsKey("catamaran"));
    }

    @Test public void testOrder() {
	TrieMap<String> m = new TrieMap<String>();
	m.put("a", "0");
	m.put("apple", "1");
	m.put("be", "2");
	m.put("bee", "3");
	m.put("boy", "4");
	m.put("cat", "5");
	
	SortedSet<String> control = 
	    new TreeSet<String>(m.keySet());
	
	Iterator<String> it1 = m.keySet().iterator();
	Iterator<String> it2 = control.iterator();

	while (it1.hasNext() && it2.hasNext()) {
	    assertEquals(it2.next(), it1.next());
	}
	
	assertEquals(it2.hasNext(), it1.hasNext());
    }

    public static void main(String args[]) {
	org.junit.runner.JUnitCore.main(TrieMapTests.class.getName());
    }

}