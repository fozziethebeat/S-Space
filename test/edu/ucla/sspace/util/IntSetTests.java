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
 * A collection of unit tests for {@link IntSet} 
 */
public class IntSetTests {
   
    @Test public void testAdd() {
        Set<Integer> test = new IntSet();
        assertTrue(test.add(1));
        assertEquals(1, test.size());
    }

    @Test(expected=IllegalArgumentException.class) public void testAddNegative() {
        Set<Integer> test = new IntSet();
        assertTrue(test.add(-1));
        assertEquals(1, test.size());
    }

    @Test public void testAddTwice() {
        Set<Integer> test = new IntSet();
        assertTrue(test.add(1));
        assertEquals(1, test.size());
        assertFalse(test.add(1));
        assertEquals(1, test.size());
    }

    @Test public void testZeroTwice() {
        Set<Integer> test = new IntSet();
        assertTrue(test.add(0));
        assertEquals(1, test.size());
        assertFalse(test.add(0));
        assertEquals(1, test.size());
    }

    @Test public void testContains() {
        Set<Integer> test = new IntSet();
        test.add(1);
        assertTrue(test.contains(1));
    }

    @Test public void testContainsNegative() {
        Set<Integer> test = new IntSet();
        assertFalse(test.contains(-1));
    }
   
    @Test public void addRandom() {
        Random r = new Random();
        long seed = System.currentTimeMillis();
        r.setSeed(seed);
        System.out.println("addRandom() seed: " + seed);
        Set<Integer> test = new IntSet();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            int j = r.nextInt(1024);
            test.add(j);
            control.add(j);
        }
        assertEquals(control.size(), test.size());
        for (Integer i : test)
            assertTrue(control.contains(i));
    }
    
    @Test public void testRemove() {
        Set<Integer> test = new IntSet();
        for (int i = 1; i < 9; ++i) {
            test.add(i);
            assertEquals(i, test.size());
        }

         for (int i = 9; i >= 1; --i) {
             test.remove(i);
             assertEquals(i-1, test.size());
         }
    }

    @Test public void testRemoveWithZero() {
        Set<Integer> test = new IntSet();
        for (int i = 0; i < 10; ++i) {
            assertTrue(test.add(i));
            assertEquals(i+1, test.size());
        }

        System.out.println("test: " + test);

        for (int i = 9; i >= 0; --i) {
            System.out.printf("i: %d, size(): %d%n", i, test.size());
            assertTrue(test.remove(i));
            assertEquals(i, test.size());
        }
    }

    @Test public void testRemoveWithPrecedingDeletedValues() {
        Set<Integer> test = new IntSet();
        assertTrue(test.add(0));
        assertTrue(test.add(4));
        assertTrue(test.add(8));
        assertTrue(test.add(12));

        assertTrue(test.remove(0));
        assertTrue(test.remove(4));
        assertTrue(test.remove(8));
        assertTrue(test.remove(12));

    }

    @Test public void testRemoveNegative() {
        Set<Integer> test = new IntSet();
        assertFalse(test.remove(-1));
    }

    @Test public void testSize() {
        Set<Integer> test = new IntSet();
        for (int i = 0; i < 100; ++i) {
            test.add(i);
            assertEquals(i+1, test.size());
        }
    }

    @Test public void testIterator() {
        Set<Integer> test = new IntSet();
        for (int i = 0; i < 10; ++i) {
            test.add(i);
        }

        Set<Integer> control = new HashSet<Integer>();
        for (Integer i : test)
            control.add(i);

        assertEquals(10, control.size());        
    }

    @Test public void testEmptyIterator() {
        Set<Integer> test = new IntSet();
        Iterator<Integer> it = test.iterator();
        assertFalse(it.hasNext());
    }

    @Test(expected=NoSuchElementException.class)  public void testEmptyIteratorNext() {
        Set<Integer> test = new IntSet();
        Iterator<Integer> it = test.iterator();
        it.next();
    }

    @Test(expected=IllegalStateException.class)  public void testEmptyIteratorRemove() {
        Set<Integer> test = new IntSet();
        Iterator<Integer> it = test.iterator();
        it.remove();
    }

    @Test public void testIteratorRemove() {
        Set<Integer> test = new IntSet();
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
        Set<Integer> test = new IntSet();
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