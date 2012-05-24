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

package edu.ucla.sspace.util.primitive;

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
 * A collection of unit tests for {@link CompactIntSet} 
 */
public class IntIntHashMultiMapTest {

    @Test public void testAsMap() {
        IntIntMultiMap m = new IntIntHashMultiMap();
        for (int i = 0; i < 10; ++i) {
            m.put(i, i);
            m.put(i, i + 1);
        }
        assertEquals(10, m.size());
        assertEquals(20, m.range());

        Map<Integer,Set<Integer>> map = m.asMap();
        assertEquals(10, map.size());
        for (Set<Integer> s : map.values())
            assertEquals(2, s.size());
    }

    @Test public void testPut() {
        IntIntMultiMap m = new IntIntHashMultiMap();
        m.put(1, 1);
        IntSet s = m.get(1);
        assertEquals(1, m.size());
        assertEquals(1, m.range());
        assertEquals(1, s.size());
        assertTrue(s.contains(1));

        m.put(1, 2);
        s = m.get(1);
        assertEquals(1, m.size());
        assertEquals(2, m.range());
        assertEquals(2, s.size());
        assertTrue(s.contains(1));
        assertTrue(s.contains(2));

        m.put(2, 2);
        s = m.get(2);
        assertEquals(2, m.size());
        assertEquals(3, m.range());
        assertEquals(1, s.size());
        assertFalse(s.contains(1));
        assertTrue(s.contains(2));
    }

    @Test public void testPutMany() {
        IntIntMultiMap m = new IntIntHashMultiMap();
        m.put(1, 1);
        IntSet s = m.get(1);
        assertEquals(1, m.size());
        assertEquals(1, m.range());
        assertEquals(1, s.size());
        assertTrue(s.contains(1));

        s = new TroveIntSet();
        for (int i = 2; i <= 10; ++i)
            s.add(i);
        m.putMany(1, s);
        s = m.get(1);
        assertEquals(1, m.size());
        assertEquals(10, m.range());
        assertEquals(10, s.size());
        for (int i = 1; i <= 10; ++i)
            assertTrue(s.contains(i));
    }

}
