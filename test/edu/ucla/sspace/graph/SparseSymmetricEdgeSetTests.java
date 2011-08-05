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

package edu.ucla.sspace.graph;

import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 */
public class SparseSymmetricEdgeSetTests { 

    @Test public void testAdd() {
        EdgeSet<Edge> e = new SparseSymmetricEdgeSet(0);
        assertEquals(0, e.size());
        e.add(new SimpleEdge(0, 1));
        assertEquals(1, e.size());
        assertTrue(e.contains(new SimpleEdge(0, 1)));
        e.add(new SimpleEdge(0, 2));
        assertEquals(2, e.size());
        assertTrue(e.contains(new SimpleEdge(0, 2)));
    }

    @Test public void testAddWrongOrder() {
        EdgeSet<Edge> e = new SparseSymmetricEdgeSet(0);
        assertEquals(0, e.size());
        e.add(new SimpleEdge(0, 1));
        assertEquals(1, e.size());
        assertTrue(e.contains(new SimpleEdge(0, 1)));
        e.add(new SimpleEdge(1, 0));
        assertEquals(1, e.size());
        assertTrue(e.contains(new SimpleEdge(1, 0)));
    }

    @Test public void testSize() {
        EdgeSet<Edge> e = new SparseSymmetricEdgeSet(0);
        assertEquals(0, e.size());
        for (int i = 1; i <= 100; ++i) {
            e.add(new SimpleEdge(0, i));
            assertEquals(i, e.size());
        }

        e.remove(new SimpleEdge(0, 1));
        assertEquals(99, e.size());

        e.clear();
        assertEquals(0, e.size());
    }

    @Test public void testRemove() {
        EdgeSet<Edge> e = new SparseSymmetricEdgeSet(0);
        assertEquals(0, e.size());
        e.add(new SimpleEdge(0, 1));
        assertEquals(1, e.size());
        assertTrue(e.contains(new SimpleEdge(0, 1)));
        e.remove(new SimpleEdge(0, 1));
        assertEquals(0, e.size());
        assertFalse(e.contains(new SimpleEdge(0, 1)));
        // extra remove on empty set
        e.remove(new SimpleEdge(0, 1));
        assertEquals(0, e.size());
        assertFalse(e.contains(new SimpleEdge(0, 1)));
    }
    
    @Test public void testIterate() {
        Set<Edge> control = new HashSet<Edge>();
        EdgeSet<Edge> e = new SparseSymmetricEdgeSet(0);

        for (int i = 1; i <= 10; ++i) {
            e.add(new SimpleEdge(0, i));
            control.add(new SimpleEdge(0, i));
        }
        assertEquals(control.size(), e.size());
        
        for (Edge j : e)
            assertTrue(control.contains(j));
    }
}