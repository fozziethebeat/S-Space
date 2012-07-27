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

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SparseWeightedDirectedTypedEdgeSetTest {

    @Test public void testConstructor() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        assertEquals(0, edges.size());
        assertTrue(edges.isEmpty());
    }

    @Test public void testAdd() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertEquals(1, edges.size());
        // Duplicate add
        assertFalse(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertEquals(1, edges.size());
        // Add without matching root vertex
        assertFalse(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 4, 5, 1.0)));
        assertEquals(1, edges.size());
        // Add of second type
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertEquals(2, edges.size());
        // Add of third type
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertEquals(3, edges.size());
        // Add to different vertex
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertEquals(4, edges.size());
    }

    @Test public void testContains() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 1, 0, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertEquals(4, edges.size());

        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 1, 0, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 2, 1.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 2, 1.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 0, 2.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 2.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 3.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 4.0)));

    }


    @Test public void testContainsWithWeightReplacement() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 2.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 2.0)));
    }


    @Test public void testGetEdges() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertEquals(4, edges.size());

        Set<WeightedDirectedTypedEdge<String>> e = edges.getEdges(1);
        assertEquals(3, e.size());
        e = edges.getEdges(2);
        assertEquals(1, e.size());
    }

    @Test public void testConnected() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertEquals(4, edges.size());

        Set<Integer> connected = edges.connected();
        assertEquals(2, connected.size());
        assertTrue(connected.contains(1));
        assertTrue(connected.contains(2));

        Iterator<Integer> it = connected.iterator();
        int i = 0;
        Set<Integer> test = new HashSet<Integer>();
        while (it.hasNext()) {
            test.add(it.next());
            i++;
        }
        assertEquals(2, i);
        assertEquals(2, test.size());
        assertTrue(test.contains(2));
        assertTrue(test.contains(1));
    }

    @Test public void testIterator() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
        assertEquals(4, edges.size());

        Iterator<WeightedDirectedTypedEdge<String>> iter = edges.iterator();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        int i = 0;
        while (iter.hasNext()) {
            control.add(iter.next());
            i++;
        }
        assertEquals(4, i);
        assertEquals(4, control.size());
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
    }

    @Test public void testIteratorInAndOutEdges() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));

        assertEquals(4, edges.size());

        Iterator<WeightedDirectedTypedEdge<String>> iter = edges.iterator();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        int i = 0;
        while (iter.hasNext()) {
            control.add(iter.next());
            i++;
        }
        assertEquals(4, i);
        assertEquals(4, control.size());
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 1, 1.0)));
        assertTrue(control.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 2, 1.0)));
    }   
    
    @Test public void testSize() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        for (int j = 1; j < 10; ++j) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, j, 1.0);
            assertTrue(edges.add(e));
            assertEquals(j, edges.size());
        }
    }

    @Test public void testSizeAfterReplacement() {
        SparseWeightedDirectedTypedEdgeSet<String> edges = 
            new SparseWeightedDirectedTypedEdgeSet<String>(0);
        for (int j = 1; j < 10; ++j) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, j, 1.0);
            assertTrue(edges.add(e));
            assertEquals(j, edges.size());
        }

        assertEquals(9, edges.size());
        WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 2.0);
        edges.add(e);
        assertEquals(9, edges.size());
    }
}