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
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ucla.sspace.graph.isomorphism;

import java.util.*;

import edu.ucla.sspace.graph.*;
import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link TypedVF2IsomorphismTester}
 */
public class TypedVF2IsomorphismTesterTests { 

    @Test public void testStarGraph() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(0, 1));
        g1.add(new SimpleEdge(0, 2));
        g1.add(new SimpleEdge(0, 3));
        g1.add(new SimpleEdge(0, 4));
        g1.add(new SimpleEdge(0, 5));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 0));
        g2.add(new SimpleEdge(1, 2));
        g2.add(new SimpleEdge(1, 3));
        g2.add(new SimpleEdge(1, 4));
        g2.add(new SimpleEdge(1, 5));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testEdgelessGraphSame() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        for (int i = 0; i < 3; ++i)
            g1.add(i);

        Graph<Edge> g2 = new SparseUndirectedGraph();
        for (int i = 3; i < 6; ++i)
            g2.add(i);

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testSameTriangle() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(0, 1));
        g1.add(new SimpleEdge(0, 2));
        g1.add(new SimpleEdge(1, 2));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(3, 4));
        g2.add(new SimpleEdge(3, 5));
        g2.add(new SimpleEdge(4, 5));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();

        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testSquare() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(1, 2));
        g1.add(new SimpleEdge(1, 4));
        g1.add(new SimpleEdge(3, 2));
        g1.add(new SimpleEdge(3, 4));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 3));
        g2.add(new SimpleEdge(1, 4));
        g2.add(new SimpleEdge(2, 3));
        g2.add(new SimpleEdge(2, 4));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testSquareWithSpoke() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(1, 2));
        g1.add(new SimpleEdge(1, 4));
        g1.add(new SimpleEdge(3, 2));
        g1.add(new SimpleEdge(3, 4));
        g1.add(new SimpleEdge(4, 5));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 3));
        g2.add(new SimpleEdge(1, 4));
        g2.add(new SimpleEdge(2, 3));
        g2.add(new SimpleEdge(2, 4));
        g2.add(new SimpleEdge(4, 5));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testSquareWithTwoSpokes() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(1, 2));
        g1.add(new SimpleEdge(1, 4));
        g1.add(new SimpleEdge(3, 2));
        g1.add(new SimpleEdge(3, 4));
        g1.add(new SimpleEdge(4, 5));
        g1.add(new SimpleEdge(3, 6));

        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 3));
        g2.add(new SimpleEdge(1, 4));
        g2.add(new SimpleEdge(2, 3));
        g2.add(new SimpleEdge(2, 4));
        g2.add(new SimpleEdge(4, 5));
        g2.add(new SimpleEdge(2, 6));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testDifferentOrders() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(0);
        
        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(0);
        g2.add(1);

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertFalse(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testDifferentSize() {
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(0, 1));
        
        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(0);
        g2.add(1);

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertFalse(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testWikiExample() {
        // a = 1
        // b = 2
        // c = 3
        // d = 4
        // g = 5
        // h = 6
        // i = 7
        // j = 8
        Graph<Edge> g1 = new SparseUndirectedGraph();
        g1.add(new SimpleEdge(1, 5)); // a - g
        g1.add(new SimpleEdge(1, 6)); // a - h
        g1.add(new SimpleEdge(1, 7)); // a - i
        g1.add(new SimpleEdge(2, 5)); // b - g
        g1.add(new SimpleEdge(2, 6)); // b - h
        g1.add(new SimpleEdge(2, 8)); // b - j
        g1.add(new SimpleEdge(3, 5)); // c - g
        g1.add(new SimpleEdge(3, 7)); // c - i
        g1.add(new SimpleEdge(3, 8)); // c - j
        g1.add(new SimpleEdge(4, 6)); // d - h
        g1.add(new SimpleEdge(4, 7)); // d - i
        g1.add(new SimpleEdge(4, 8)); // d - j

        // Other graph just with numeric edges
        Graph<Edge> g2 = new SparseUndirectedGraph();
        g2.add(new SimpleEdge(1, 2)); 
        g2.add(new SimpleEdge(1, 4)); 
        g2.add(new SimpleEdge(1, 5));
        g2.add(new SimpleEdge(2, 3));
        g2.add(new SimpleEdge(2, 6));
        g2.add(new SimpleEdge(3, 4));
        g2.add(new SimpleEdge(3, 7));
        g2.add(new SimpleEdge(4, 8));
        g2.add(new SimpleEdge(5, 6));
        g2.add(new SimpleEdge(5, 8));
        g2.add(new SimpleEdge(6, 7));
        g2.add(new SimpleEdge(7, 8));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testIsomorphicMultigraphs() {    
        DirectedMultigraph<String> g1 = new DirectedMultigraph<String>();
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        DirectedMultigraph<String> g2 = new DirectedMultigraph<String>();
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testNonIsomorphicMultigraphsDifferentNumberOfParallelEdges() {    
        DirectedMultigraph<String> g1 = new DirectedMultigraph<String>();
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-4", 3, 4)));

        DirectedMultigraph<String> g2 = new DirectedMultigraph<String>();
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertFalse(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testNonIsomorphicMultigraphsDifferentTypeOfEdges() {    
        DirectedMultigraph<String> g1 = new DirectedMultigraph<String>();
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 2, 0)));

        DirectedMultigraph<String> g2 = new DirectedMultigraph<String>();
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 2)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 2, 0)));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertFalse(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testNonIsomorphicMultigraphs() {    
        DirectedMultigraph<String> g1 = new DirectedMultigraph<String>();
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        DirectedMultigraph<String> g2 = new DirectedMultigraph<String>();
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertFalse(isoTest.areIsomorphic(g1, g2));
    }

    @Test public void testIsomorphicVertexMapping() {    
        DirectedMultigraph<String> g1 = new DirectedMultigraph<String>();
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 2)));
        assertTrue(g1.add(new SimpleDirectedTypedEdge<String>("type-3", 1, 2)));

        DirectedMultigraph<String> g2 = new DirectedMultigraph<String>();
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-1", 3, 4)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-2", 3, 5)));
        assertTrue(g2.add(new SimpleDirectedTypedEdge<String>("type-3", 4, 5)));

        IsomorphismTester isoTest = new TypedVF2IsomorphismTester();
        assertTrue(isoTest.areIsomorphic(g1, g2));
        Map<Integer,Integer> vMap = isoTest.findIsomorphism(g1, g2);
        System.out.println(vMap);
        assertEquals(3, vMap.size());
        assertEquals(Integer.valueOf(3), vMap.get(0));
        assertEquals(Integer.valueOf(4), vMap.get(1));
        assertEquals(Integer.valueOf(5), vMap.get(2));
    }

}