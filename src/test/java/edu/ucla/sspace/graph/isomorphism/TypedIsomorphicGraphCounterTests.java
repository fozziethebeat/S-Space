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

package edu.ucla.sspace.graph.isomorphism;

import java.util.*;
import edu.ucla.sspace.graph.*;
import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link TypedIsomorphicGraphCounter} class
 */
public class TypedIsomorphicGraphCounterTests { 

    @Test public void testIsomorphic() {
        TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>> gc = 
            new TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>>();
        for (int i = 0; i < 5; ++i) {
            UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
            g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
            gc.count(g1);
        }
        assertEquals(5, gc.sum());
        assertEquals(1, gc.items().size());
        UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
        g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
        assertEquals(5, gc.getCount(g1));

        g1 = new UndirectedMultigraph<String>();
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
        g1.add(new SimpleTypedEdge<String>("type-2", 1, 2));
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
        assertEquals(0, gc.getCount(g1));
    }

    @Test public void testNonIsomorphic() {
        TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>> gc = 
            new TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>>();
        for (int i = 0; i < 5; ++i) {
            UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
            g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
            gc.count(g1);
        }
        for (int i = 0; i < 2; ++i) {
            UndirectedMultigraph<String> g2 = new UndirectedMultigraph<String>();
            g2.add(new SimpleTypedEdge<String>("type-2", 0, 1));
            g2.add(new SimpleTypedEdge<String>("type-2", 1, 2));
            g2.add(new SimpleTypedEdge<String>("type-2", 0, 2));
            gc.count(g2);
        }

        assertEquals(7, gc.sum());
        assertEquals(2, gc.size());
        assertEquals(2, gc.items().size());

        UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
        g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
        assertEquals(5, gc.getCount(g1));

        UndirectedMultigraph<String> g2 = new UndirectedMultigraph<String>();
        g2.add(new SimpleTypedEdge<String>("type-2", 0, 1));
        g2.add(new SimpleTypedEdge<String>("type-2", 1, 2));
        g2.add(new SimpleTypedEdge<String>("type-2", 0, 2));
        assertEquals(2, gc.getCount(g2));
    }

    @Test public void testItems() {
        TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>> gc = 
            new TypedIsomorphicGraphCounter<String,UndirectedMultigraph<String>>();
        for (int i = 0; i < 5; ++i) {
            UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
            g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
            g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
            gc.count(g1);
        }
        for (int i = 0; i < 2; ++i) {
            UndirectedMultigraph<String> g2 = new UndirectedMultigraph<String>();
            g2.add(new SimpleTypedEdge<String>("type-2", 0, 1));
            g2.add(new SimpleTypedEdge<String>("type-2", 1, 2));
            g2.add(new SimpleTypedEdge<String>("type-2", 0, 2));
            gc.count(g2);
        }

        assertEquals(7, gc.sum());
        assertEquals(2, gc.size());
        assertEquals(2, gc.items().size());      

        Set<UndirectedMultigraph<String>> control = 
            new HashSet<UndirectedMultigraph<String>>();

        UndirectedMultigraph<String> g1 = new UndirectedMultigraph<String>();
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 1));
        g1.add(new SimpleTypedEdge<String>("type-1", 1, 2));
        g1.add(new SimpleTypedEdge<String>("type-1", 0, 2));
        assertEquals(5, gc.getCount(g1));

        UndirectedMultigraph<String> g2 = new UndirectedMultigraph<String>();
        g2.add(new SimpleTypedEdge<String>("type-2", 0, 1));
        g2.add(new SimpleTypedEdge<String>("type-2", 1, 2));
        g2.add(new SimpleTypedEdge<String>("type-2", 0, 2));
        assertEquals(2, gc.getCount(g2));

        control.add(g1);
        control.add(g2);

        assertEquals(control, gc.items());
    }

//     @Test public void testNonIsomorphic() {
//         TypedIsomorphicGraphCounter<Graph<Edge>> gc = new TypedIsomorphicGraphCounter<Graph<Edge>>();
//         for (int i = 0; i < 2; ++i) {
//             Graph<Edge> g1 = new SparseUndirectedGraph();
//             g1.add(new SimpleEdge(0, 1));
//             g1.add(new SimpleEdge(0, 2));
//             g1.add(new SimpleEdge(1, 2));
//             gc.count(g1);
//         }

//        for (int i = 0; i < 3; ++i) {
//            Graph<Edge> g2 = new SparseUndirectedGraph();
//            g2.add(new SimpleEdge(1, 3));
//            g2.add(new SimpleEdge(1, 4));
//            g2.add(new SimpleEdge(2, 3));
//            g2.add(new SimpleEdge(2, 4));
//            g2.add(new SimpleEdge(4, 5));
//            g2.add(new SimpleEdge(2, 6));
//            gc.count(g2);
//         }
 
//         assertEquals(5, gc.sum());
//         assertEquals(2, gc.items().size());

//         Graph<Edge> g1 = new SparseUndirectedGraph();
//         g1.add(new SimpleEdge(0, 1));
//         g1.add(new SimpleEdge(0, 2));
//         g1.add(new SimpleEdge(1, 2));
//         assertEquals(2, gc.getCount(g1));

//         Graph<Edge> g2 = new SparseUndirectedGraph();
//         g2.add(new SimpleEdge(1, 3));
//         g2.add(new SimpleEdge(1, 4));
//         g2.add(new SimpleEdge(2, 3));
//         g2.add(new SimpleEdge(2, 4));
//         g2.add(new SimpleEdge(4, 5));
//         g2.add(new SimpleEdge(2, 6));
//         assertEquals(3, gc.getCount(g2));
//     }
}