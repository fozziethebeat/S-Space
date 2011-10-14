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
 * A collection of unit tests for {@link HashMultiMap} 
 */
public class HashMultiMapTests {

    
    @Test public void testConstructor() {
         HashMultiMap<String,String> m = new HashMultiMap<String,String>();
    }

    @Test public void testMapConstructor() {

        Map<String,String> control = new HashMap<String,String>();
        
        for (int i = 0; i < 512; ++i) {
            String s = Integer.toHexString(i);
            control.put(s, s);
        }

        HashMultiMap<String,String> test = new HashMultiMap<String,String>(control);

        assertEquals(control.size(), test.size());
        assertTrue(control.keySet().containsAll(test.keySet()));
        assertTrue(control.values().containsAll(test.values()));
    }    

    @Test(expected=NullPointerException.class) 
    public void testMapConstructorNull() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>(null);
    }

    @Test public void testPut() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("a", "1");
        Set<String> s = m.get("a");
        assertTrue(s.contains("1"));
    }

    @Test public void testPutMulti() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        Set<String> vals = new HashSet<String>();
        vals.add("1");
        vals.add("2");
        m.putMulti("a", vals);
        Set<String> s = m.get("a");
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
        assertEquals(2, s.size());
        assertEquals(2, m.range());
        assertEquals(1, m.size());

        // empty values
        m.putMulti("b", new HashSet<String>());
        assertFalse(m.containsKey("b"));
    }

    @Test public void testPutEmptyString() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("", "empty");
        assertTrue(m.get("").contains("empty"));
    }

    @Test public void testPutDifferentFirstChars() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("a", "1");
        m.put("ac", "2");
        m.put("b", "3");
        m.put("bd", "4");

        assertTrue(m.get("a").contains("1"));
        assertTrue(m.get("ac").contains("2"));
        assertTrue(m.get("b").contains("3"));
        assertTrue(m.get("bd").contains("4"));
    }

    @Test public void testPutConflict() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("a", "1");
        m.put("a", "2");
        Set<String> s = m.get("a");
        assertTrue(s.contains("2"));
    }

    @Test public void testPutSubstringOfKey() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("catapult", "1");
        m.put("cat", "2");

        assertTrue(m.get("catapult").contains("1"));
        assertTrue(m.get("cat").contains("2"));
    }

    @Test public void testPutKeyIsLonger() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "1");
        m.put("catapult", "2");

        assertTrue(m.get("catapult").contains("2"));
        assertTrue(m.get("cat").contains("1"));
    }
    
    @Test public void testPutKeyIsLongerByOne() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "1");
        m.put("cats", "2");

        assertTrue(m.get("cats").contains("2"));
        assertTrue(m.get("cat").contains("1"));
    }

    @Test public void testPutKeysLongerByOne() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "1");
        assertTrue(m.get("cat").contains("1"));
        m.put("catx", "2");
        assertTrue(m.get("catx").contains("2"));
        m.put("catxy", "3");
        assertTrue(m.get("catxy").contains("3"));
        m.put("catxyz", "4");
        assertTrue(m.get("catxyz").contains("4"));
    }


    @Test public void testPutLotsOfRandomPrefixes() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertEquals(0, m.size());
        int size = 10240;
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
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
    

    @Test public void testContainsKey() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("catapult", "1");
        m.put("cat", "2");

        assertTrue(m.containsKey("catapult"));
        assertTrue(m.containsKey("cat"));
    }

    @Test public void testContainsKeyFalse() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("catapult", "1");
        m.put("cat", "2");

        assertFalse(m.containsKey("dog"));
        assertFalse(m.containsKey("c"));
        assertFalse(m.containsKey("ca"));
        assertFalse(m.containsKey("cats"));
        assertFalse(m.containsKey("catapul"));
    }

    @Test public void testContainsValue() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("catapult", "1");
        m.put("cat", "2");

        assertTrue(m.containsValue("1"));
        assertTrue(m.containsValue("2"));
    }

    @Test public void testContainsValueFalse() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("catapult", "1");
        m.put("cat", "2");

        assertFalse(m.containsValue("catapult"));
        assertFalse(m.containsValue("c"));
        assertFalse(m.containsValue("cat"));
    }

    @Test public void testKeySet() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();

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
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");
        m.put("category", "1");
        m.put("catamaran", "2");
        m.put("apple", "3");
        m.put("banana", "4");

        Set<Map.Entry<String,String>> test = m.entrySet();

        System.out.println(m);
        System.out.println(m.entrySet());
        for (Map.Entry<String,String> e : m.entrySet()) {
            String key = e.getKey().toString();
            System.out.println(key);
            e.setValue(key);
        }
        
        System.out.println(m);

        assertTrue(m.get("cat").contains("cat"));
        assertEquals(1, m.get("cat").size());

        assertTrue(m.get("category").contains("category"));
        assertEquals(1, m.get("category").size());

        assertTrue(m.get("catamaran").contains("catamaran"));
        assertEquals(1, m.get("catamaran").size());

        assertTrue(m.get("apple").contains("apple"));
        assertEquals(1, m.get("apple").size());

        assertTrue(m.get("banana").contains("banana"));
        assertEquals(1, m.get("banana").size());
    }

    @Test public void testKeyIterator() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
        
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
        
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");

        Iterator<String> it = m.keySet().iterator();
        it.next();
        it.next(); // error
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyIteratorNextError() {
        
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();

        Iterator<String> it = m.keySet().iterator();
        it.next(); // error
    }

    @Test public void testIteratorRemove() {
        
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");
        assertTrue(m.containsKey("cat"));

        Iterator<String> it = m.keySet().iterator();
        it.next();
        it.remove();
        assertFalse(m.containsKey("cat"));
    }

    @Test(expected=IllegalStateException.class) 
    public void testIteratorRemoveTwice() {
        
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");
        assertTrue(m.containsKey("cat"));

        Iterator<String> it = m.keySet().iterator();
        it.next();
        it.remove();
        it.remove();
    }

    @Test public void testRemove() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");
        assertTrue(m.containsKey("cat"));

        m.remove("cat");
        assertFalse(m.containsKey("cat"));
        assertEquals(0, m.size());
    }

    @Test public void testRemoveEmptyString() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("", "empty");
        assertTrue(m.get("").contains("empty"));
        m.remove("");
        assertFalse(m.containsKey(""));
    }

    @Test public void testRemoveIntermediate() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "cat");
        m.put("cats", "cats");
        assertTrue(m.containsKey("cat"));

        m.remove("cat");
        assertFalse(m.containsKey("cat"));
        assertTrue(m.containsKey("cats"));
        assertEquals(1, m.size());
    }

    @Test public void testRemoveTerminal() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "cat");
        m.put("cats", "cats");
        assertTrue(m.containsKey("cats"));

        m.remove("cats");
        assertFalse(m.containsKey("cats"));
        assertTrue(m.containsKey("cat"));
        assertEquals(1, m.size());
    }

    @Test public void testMultipleRemoves() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertEquals(0, m.size());
        m.put("cat", "0");
        assertEquals(1, m.size());
        m.put("category", "1");
        assertEquals(2, m.size());
        m.put("catamaran", "2");
        assertEquals(3, m.size());
    }

    @Test public void testSizeLotsOfPrefixes() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertEquals(0, m.size());
        for (int i = 0; i < 10240; ++i) {
            String s = String.valueOf(i);
            m.put(s,s);
            assertEquals(i+1, m.size());
        }
    }

    @Test public void testSizeLotsOfRandomPrefixes() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
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

    @Test public void testRange() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertEquals(0, m.range());
        m.put("1","1");
        assertEquals(1, m.range());
    }

    @Test public void testRangeIncreasing() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();

        for (int i = 0; i < 10240; ++i) {
            String s = String.valueOf(i);
            m.put("1",s);
             assertEquals(i+1, m.range());
            assertEquals(1, m.size());
        }
    }

    

    @Test public void testIsEmpty() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertTrue(m.isEmpty());

        m.put("cat", "0");
        assertFalse(m.isEmpty());
        
        m.clear();
        assertTrue(m.isEmpty());
    }

    @Test public void testEmptyEntrySet() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        assertTrue(m.isEmpty());
        Set<Map.Entry<String, String>> e = m.entrySet();
        assertEquals(0, e.size());
        Iterator<Map.Entry<String, String>> i = e.iterator();
        assertFalse(i.hasNext());
    }

    @Test public void testClear() {
        HashMultiMap<String,String> m = new HashMultiMap<String,String>();
        m.put("cat", "0");
        m.put("category", "1");
        m.put("catamaran", "2");

        m.clear();
        assertEquals(0, m.size());
        assertEquals(0, m.range());
        assertFalse(m.containsKey("cat"));
        assertFalse(m.containsKey("category"));
        assertFalse(m.containsKey("catamaran"));
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(HashMultiMapTests.class.getName());
    }

}
