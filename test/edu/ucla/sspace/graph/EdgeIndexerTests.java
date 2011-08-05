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
public class EdgeIndexerTests { 

    @Test public void testWithoutIndexer() {
        Graph<Edge> g = new SparseUndirectedGraph();
        g.add(new SimpleEdge(0, 1));
        g.add(new SimpleEdge(0, 2));
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(3, 4));
        g.add(new SimpleEdge(3, 5));
        g.add(new SimpleEdge(4, 5));
        
        EdgeIndexer indexer = new EdgeIndexer(g);
        SortedSet<Integer> indices = new TreeSet<Integer>();
        for (Edge e : g.edges()) {
            int index = indexer.find(e);
            System.out.printf("the index of %s is %d%n", e, index);
            assertEquals(e, indexer.lookup(index));
            indices.add(index);
        }
        assertEquals(g.size(), indices.size());
        assertEquals(Integer.valueOf(0), indices.first());
        assertEquals(Integer.valueOf(g.size() - 1), indices.last());
    }

    @Test public void testWithoutIndexerVertexNoEdges() {
        Graph<Edge> g = new SparseUndirectedGraph();
        g.add(new SimpleEdge(0, 1));
        g.add(new SimpleEdge(0, 2));
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(3, 5));
        g.add(new SimpleEdge(3, 6));
        g.add(4);
        g.add(new SimpleEdge(5, 6));
        
        EdgeIndexer indexer = new EdgeIndexer(g);
        SortedSet<Integer> indices = new TreeSet<Integer>();
        for (Edge e : g.edges()) {
            int index = indexer.find(e);
            System.out.printf("the index of %s is %d%n", e, index);
            assertEquals(e, indexer.lookup(index));
            indices.add(index);
        }
        assertEquals(g.size(), indices.size());
        assertEquals(Integer.valueOf(0), indices.first());
        assertEquals(Integer.valueOf(g.size() - 1), indices.last());
    }

    @Test public void testWithoutIndexer2VertexNoEdges() {
        Graph<Edge> g = new SparseUndirectedGraph();
        g.add(new SimpleEdge(0, 1));
        g.add(new SimpleEdge(0, 2));
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(3, 6));
        g.add(new SimpleEdge(3, 7));
        g.add(4);
        g.add(5);
        g.add(new SimpleEdge(6, 7));
        
        EdgeIndexer indexer = new EdgeIndexer(g);
        SortedSet<Integer> indices = new TreeSet<Integer>();
        for (Edge e : g.edges()) {
            int index = indexer.find(e);
            System.out.printf("the index of %s is %d%n", e, index);
            assertEquals(e, indexer.lookup(index));
            indices.add(index);
        }
        assertEquals(g.size(), indices.size());
        assertEquals(Integer.valueOf(0), indices.first());
        assertEquals(Integer.valueOf(g.size() - 1), indices.last());
    }
}