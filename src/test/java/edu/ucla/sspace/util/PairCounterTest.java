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
 * A collection of unit tests for {@link PairCounter} 
 */
public class PairCounterTest {
   
    @Test public void testCount() {
        PairCounter<Integer> c = new PairCounter<Integer>();
        c.count(new Pair<Integer>(1,2));
        assertEquals(1, c.sum());
        assertEquals(1, c.items().size());
        assertEquals(1, c.getCount(new Pair<Integer>(1,2)));

        c.count(new Pair<Integer>(1,2));
        assertEquals(2, c.sum());
        assertEquals(1, c.items().size());
        assertEquals(2, c.getCount(new Pair<Integer>(1,2)));

        c.count(new Pair<Integer>(2,1));
        assertEquals(3, c.sum());
        assertEquals(2, c.items().size());
        assertEquals(1, c.getCount(new Pair<Integer>(2,1)));
    }

    @Test public void testItems() {
        PairCounter<Integer> c = new PairCounter<Integer>();
        c.count(new Pair<Integer>(1,2));
        c.count(new Pair<Integer>(1,2));
        c.count(new Pair<Integer>(2,1));
        Set<Pair<Integer>> s = c.items();
        assertEquals(2, s.size());
        assertTrue(s.contains(new Pair<Integer>(1,2)));
        assertTrue(s.contains(new Pair<Integer>(2,1)));
        assertFalse(s.contains(new Pair<Integer>(1,1)));
        assertFalse(s.contains(new Pair<Integer>(3,3)));

        Set<Pair<Integer>> control = new HashSet<Pair<Integer>>();
        control.add(new Pair<Integer>(1,2));
        control.add(new Pair<Integer>(2,1));
        Set<Pair<Integer>> test = new HashSet<Pair<Integer>>();
        
        Iterator<Pair<Integer>> it = s.iterator();
        int seen = 0;
        while (it.hasNext()) {
            test.add(it.next());
            seen++;
        }
        assertEquals(2, seen);
        assertEquals(control, test);        
    }

    @Test public void testIterator() {
        PairCounter<Integer> c = new PairCounter<Integer>();
        c.count(new Pair<Integer>(1,2));
        c.count(new Pair<Integer>(1,2));
        c.count(new Pair<Integer>(2,1));

        Map<Pair<Integer>,Integer> m = new HashMap<Pair<Integer>,Integer>();
        m.put(new Pair<Integer>(1,2), 2);
        m.put(new Pair<Integer>(2,1), 1);


        Iterator<Map.Entry<Pair<Integer>,Integer>> it = c.iterator();
        int seen = 0;
        while (it.hasNext()) {
            Map.Entry<Pair<Integer>,Integer> e = it.next();
            Pair<Integer> p = e.getKey();
            assertEquals(m.get(p), e.getValue());
            seen++;
        }
        assertEquals(2, seen);
    }
}