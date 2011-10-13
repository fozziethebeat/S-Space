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
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link OpenIntSet} 
 */
public class OpenIntSetTests {
   
    @Test public void testAdd() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(1));
        assertEquals(1, test.size());
    }

    @Test public void testAddNegative() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(-1));
        assertEquals(1, test.size());
    }

    @Test public void testAddPositiveAndNegative() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 1; i <= 10; ++i) {
            assertTrue(test.add(i));
            assertTrue(test.add(-i));
        }
        assertEquals(20, test.size());
    }

    @Test public void testEmptyMarker() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(0));
        assertEquals(1, test.size());
        assertTrue(test.contains(0));
    }

    @Test public void testDeletedMarker() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(Integer.MAX_VALUE));
        assertEquals(1, test.size());
        assertTrue(test.contains(Integer.MAX_VALUE));
    }

    @Test public void testAddTwice() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(1));
        assertEquals(1, test.size());
        assertFalse(test.add(1));
        assertEquals(1, test.size());
    }

    @Test public void testZeroTwice() {
        Set<Integer> test = new OpenIntSet();
        assertTrue(test.add(0));
        assertEquals(1, test.size());
        assertFalse(test.add(0));
        assertEquals(1, test.size());
    }

    @Test public void testContains() {
        Set<Integer> test = new OpenIntSet();
        test.add(1);
        assertTrue(test.contains(1));
    }

    @Test public void testContainsNegative() {
        Set<Integer> test = new OpenIntSet();
        test.add(-1);
        assertTrue(test.contains(-1));
    }

    @Test public void testContainsPositiveAndNegative() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 1; i <= 10; ++i) {
            assertTrue(test.add(i));
            assertTrue(test.add(-i));
        }
        for (int i = 1; i <= 10; ++i) {
            assertTrue(test.contains(i));
            assertTrue(test.contains(-i));
        }
    }

    @Test public void testContainsAfterRemove() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 1; i < 9; ++i) {
            test.add(i);
            assertEquals(i, test.size());
        }

        for (int i = 1; i < 9; ++i) {
            if (i % 3 ==0)
                assertTrue(test.contains(i));
            else
                assertTrue(test.remove(i));
        }

        for (int i = 1; i < 9; ++i) {
            if (i % 3 ==0)
                assertTrue(test.contains(i));
        }
    }

   
    @Test public void addRandom() {
        Random r = new Random();
        long seed = System.currentTimeMillis();
        r.setSeed(seed);
        System.out.println("addRandom() seed: " + seed);
        Set<Integer> test = new OpenIntSet();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            int j = r.nextInt(Integer.MAX_VALUE);
            test.add(j);
            control.add(j);
        }
        assertEquals(control.size(), test.size());
        for (Integer i : test)
            assertTrue(control.contains(i));
    }
    
    @Test public void testRemove() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 1; i < 9; ++i) {
            test.add(i);
            assertEquals(i, test.size());
        }

         for (int i = 9; i >= 1; --i) {
             System.out.printf("i: %d, size(): %d%n", i, test.size());
             test.remove(i);
             assertEquals(i-1, test.size());
         }
    }

    @Test public void testRemoveWithZero() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 0; i < 10; ++i) {
            test.add(i);
            assertEquals(i+1, test.size());
        }

        for (int i = 9; i >= 0; --i) {
            test.remove(i);
            assertEquals(i, test.size());
        }
    }

    @Test public void testSize() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 0; i < 100; ++i) {
            test.add(i);
            assertEquals(i+1, test.size());
        }
    }

    @Test public void testIterator() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 0; i < 10; ++i) {
            test.add(i);
        }

        Set<Integer> control = new HashSet<Integer>();
        for (Integer i : test)
            control.add(i);

        assertEquals(10, control.size());        
    }

    @Test public void testEmptyIterator() {
        Set<Integer> test = new OpenIntSet();
        Iterator<Integer> it = test.iterator();
        assertFalse(it.hasNext());
    }

    @Test(expected=NoSuchElementException.class)  public void testEmptyIteratorNext() {
        Set<Integer> test = new OpenIntSet();
        Iterator<Integer> it = test.iterator();
        it.next();
    }

    @Test(expected=IllegalStateException.class)  public void testEmptyIteratorRemove() {
        Set<Integer> test = new OpenIntSet();
        Iterator<Integer> it = test.iterator();
        it.remove();
    }

    @Test public void testIteratorRemove() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 0; i < 10; ++i) {
            test.add(i);
        }

        Iterator<Integer> it = test.iterator();
        Integer i = it.next();
        assertEquals(10, test.size());
        it.remove();
        assertEquals(9, test.size());
        assertFalse(test.contains(i));
    }

    @Test(expected=IllegalStateException.class)  public void testEmptyIteratorRemoveTwice() {
        Set<Integer> test = new OpenIntSet();
        for (int i = 0; i < 10; ++i) {
            test.add(i);
        }
        Iterator<Integer> it = test.iterator();
        it.remove();
        it.remove();
    }


    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(IntegerMapTests.class.getName());
    }

}