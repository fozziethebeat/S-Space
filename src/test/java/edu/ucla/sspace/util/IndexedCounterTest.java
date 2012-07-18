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

import java.io.*;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link HashIndexer} 
 */
public class IndexedCounterTest {

    @Test public void testCount() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        IndexedCounter<Integer> c = new IndexedCounter<Integer>(h);
        assertEquals(0, c.size());
        assertEquals(0, c.sum());
        for (int i = 0; i < 10; ++i) {
            c.count(i);
            assertEquals(i + 1, c.size());
            assertEquals(i + 1, c.sum());
            assertEquals(1, c.getCount(i));
        }
    }

    @Test public void testSum() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        IndexedCounter<Integer> c = new IndexedCounter<Integer>(h);
        assertEquals(0, c.size());
        assertEquals(0, c.sum());
        for (int i = 0; i < 10; ++i) {
            c.count(i);
            assertEquals(i + 1, c.size());
            assertEquals(i + 1, c.sum());
            assertEquals(1, c.getCount(i));
        }
        for (int i = 0; i < 10; ++i) {
            c.count(i);
            assertEquals(10, c.size());
            assertEquals(i + 11, c.sum());
            assertEquals(2, c.getCount(i));
        }
    }


    @Test public void testIterator() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        IndexedCounter<Integer> c = new IndexedCounter<Integer>(h);
        assertEquals(0, c.size());
        assertEquals(0, c.sum());
        for (int i = 0; i < 10; ++i) {
            c.count(i);
            assertEquals(i + 1, c.size());
            assertEquals(i + 1, c.sum());
            assertEquals(1, c.getCount(i));
        }

        Set<Map.Entry<Integer,Integer>> control = 
            new HashSet<Map.Entry<Integer,Integer>>();
        for (int i = 0; i < 10; ++i) {
            Map.Entry<Integer,Integer> e = 
                new AbstractMap.SimpleImmutableEntry<Integer,Integer>(i, 1);
            control.add(e);
        }

        Set<Map.Entry<Integer,Integer>> test = 
            new HashSet<Map.Entry<Integer,Integer>>();
        for (Map.Entry<Integer,Integer> e : c)
            test.add(e);
        assertEquals(control, test);
    }

}