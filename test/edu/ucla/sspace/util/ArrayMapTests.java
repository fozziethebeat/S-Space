/*
 * Copyright 2011 David Jurgens
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

import java.util.*;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link ArrayMap} 
 */
public class ArrayMapTests {
   
    @Test public void testCreate() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
    }

    @Test(expected=NullPointerException.class) public void testCreateNull() {
        Map<Integer,String> map = new ArrayMap<String>(null);
    }

    @Test public void testGet() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        assertEquals("foo", map.get(0));
        assertEquals("bar", map.get(1));
        assertEquals("baz", map.get(2));
        assertEquals(null, map.get(-1));
        assertEquals(null, map.get(3));
    }

    @Test public void testPut() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        map.put(0, "quux");
        assertEquals("quux", map.get(0));
        assertEquals("bar", map.get(1));
        assertEquals("baz", map.get(2));
    }

    @Test(expected=IllegalArgumentException.class) public void testPutLessThanZero() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        map.put(-1, "quux");
    }

    @Test(expected=IllegalArgumentException.class) public void testPutOutOfBounds() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        map.put(3, "quux");
    }

    @Test public void testSize() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        assertEquals(3, map.size());
        assertTrue(map.remove(2) != null);
        assertEquals(2, map.size());
        map.put(2, "baz");
        assertEquals(3, map.size());

        map = new ArrayMap<String>(new String[3]);
        assertEquals(0, map.size());
    }

    @Test public void testKeySetIterator() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        
        Iterator<Integer> it = map.keySet().iterator();
        Set<Integer> control = new HashSet<Integer>();
        int i = 0;
        while (it.hasNext()) {
            control.add(it.next());
            i++;
        }
        assertEquals(3, i);
        assertEquals(3, control.size());
    }

    @Test public void testValuesIterator() {
        Map<Integer,String> map = new ArrayMap<String>(new String[] { "foo", "bar", "baz" });
        
        Iterator<String> it = map.values().iterator();
        Set<String> control = new HashSet<String>();
        int i = 0;
        while (it.hasNext()) {
            control.add(it.next());
            i++;
        }
        assertEquals(3, i);
        assertEquals(3, control.size());
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(IntegerMapTests.class.getName());
    }

}