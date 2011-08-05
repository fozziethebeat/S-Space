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
 * Tests for the {@link MotifCounter} class
 */
public class MotifCounterTests { 

    @Test public void testIsomorphic() {
        MotifCounter<Graph<Edge>> gc = new MotifCounter<Graph<Edge>>();
        for (int i = 0; i < 5; ++i) {
            Graph<Edge> g1 = new SparseUndirectedGraph();
            g1.add(new SimpleEdge(0, 1));
            g1.add(new SimpleEdge(0, 2));
            g1.add(new SimpleEdge(1, 2));
            gc.count(g1);
        }
        assertEquals(5, gc.sum());
        assertEquals(1, gc.items().size());
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(0, 1));
        g1.add(new SimpleEdge(0, 2));
        g1.add(new SimpleEdge(1, 2));
        assertEquals(5, gc.getCount(g1));
    }

    @Test public void testNonIsomorphic() {
        MotifCounter<Graph<Edge>> gc = new MotifCounter<Graph<Edge>>();
        for (int i = 0; i < 2; ++i) {
            Graph<Edge> g1 = new SparseUndirectedGraph();
            g1.add(new SimpleEdge(0, 1));
            g1.add(new SimpleEdge(0, 2));
            g1.add(new SimpleEdge(1, 2));
            gc.count(g1);
        }

       for (int i = 0; i < 3; ++i) {
           Graph<Edge> g2 = new SparseUndirectedGraph();
           g2.add(new SimpleEdge(1, 3));
           g2.add(new SimpleEdge(1, 4));
           g2.add(new SimpleEdge(2, 3));
           g2.add(new SimpleEdge(2, 4));
           g2.add(new SimpleEdge(4, 5));
           g2.add(new SimpleEdge(2, 6));
           gc.count(g2);
        }
 
        assertEquals(5, gc.sum());
        assertEquals(2, gc.items().size());

        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(0, 1));
        g1.add(new SimpleEdge(0, 2));
        g1.add(new SimpleEdge(1, 2));
        assertEquals(2, gc.getCount(g1));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 3));
        g2.add(new SimpleEdge(1, 4));
        g2.add(new SimpleEdge(2, 3));
        g2.add(new SimpleEdge(2, 4));
        g2.add(new SimpleEdge(4, 5));
        g2.add(new SimpleEdge(2, 6));
        assertEquals(3, gc.getCount(g2));
    }
}