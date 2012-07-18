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
public class WeightedDirectedMultigraphTest { 

    @Test public void testConstructor() {
        Set<Integer> vertices = new HashSet<Integer>();
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertEquals(0, g.order());
        assertEquals(0, g.size());
    }

    @Test(expected=NullPointerException.class) public void testConstructor2NullArg() {
        Graph<Edge> g = new SparseUndirectedGraph((Graph<DirectedTypedEdge<String>>)null);
    }

    @Test public void testAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(0));
        assertEquals(1, g.order());
        assertTrue(g.contains(0));
        // second add should have no effect
        assertFalse(g.add(0));
        assertEquals(1, g.order());
        assertTrue(g.contains(0));

        assertTrue(g.add(1));
        assertEquals(2, g.order());
        assertTrue(g.contains(1));
    }

    @Test public void testEquals() {
        WeightedDirectedMultigraph<String> g1 = new WeightedDirectedMultigraph<String>();
        WeightedDirectedMultigraph<String> g2 = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                g1.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
                g2.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
            }
        }
        assertEquals(g1, g2);

        g1 = new WeightedDirectedMultigraph<String>();
        g2 = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                g1.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
                g2.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",j, i, 1.0));
            }
        }

        assertFalse(g1.equals(g2));
        assertFalse(g2.equals(g1));
    }

    @Test public void testEqualGeneric() {
        WeightedDirectedMultigraph<String> g1 = new WeightedDirectedMultigraph<String>();
        Graph<WeightedDirectedTypedEdge<String>> g2 = new GenericGraph<WeightedDirectedTypedEdge<String>>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                g1.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
                g2.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
            }
        }
        assertEquals(g1, g2);
    }

    @Test public void testContainsEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 100; ++i)
            for (int j = i + 1; j < 100; ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));

        for (int i = 0; i < 100; ++i) {
            for (int j = i + 1; j < 100; ++j) {
                g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
                g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",j, i, 1.0));
                g.contains(i, j);
                g.contains(j, i);
            }
        }
    }

    @Test public void testAddEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));

        g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 2, 1.0));
        assertEquals(3, g.order());
        assertEquals(2, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 2, 1.0)));

        g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",3, 4, 1.0));
        assertEquals(5, g.order());
        assertEquals(3, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",3, 4, 1.0)));
    }

    @Test public void testRemoveLesserVertexWithEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        for (int i = 1; i < 100; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
        }
       
        assertTrue(g.contains(0));
        assertTrue(g.remove(0));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }

    @Test public void testRemoveHigherVertexWithEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        for (int i = 0; i < 99; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",100, i, 1.0);
            g.add(e);
        }
        
        assertTrue(g.contains(100));
        assertTrue(g.remove(100));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }


    @Test public void testRemoveVertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
        }

        for (int i = 99; i >= 0; --i) {            
            assertTrue(g.remove(i));
            assertEquals(i, g.order());
            assertFalse(g.contains(i));
            assertFalse(g.remove(i));
        }
    }

    @Test public void testRemoveEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        for (int i = 1; i < 100; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
        }
        
        for (int i = 99; i > 0; --i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            assertTrue(g.remove(e));
            assertEquals(i-1, g.size());
            assertFalse(g.contains(e));
            assertFalse(g.remove(e));
        }
    }

    @Test public void testVertexIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }
        assertEquals(control.size(), g.order());
        for (Integer i : g.vertices())
            assertTrue(control.contains(i));        
    }

    @Test public void testEdgeIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (Edge e : g.edges()) {
            assertTrue(control.remove(e));
            returned++;
        }
        assertEquals(g.size(), returned);
        assertEquals(0, control.size());
    }

    @Test public void testEdgeIteratorSmall() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 5; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            assertTrue(g.add(e));
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (Edge e : g.edges()) {
            System.out.println(e);
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }

    @Test public void testEdgeIteratorSmallReverse() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 5; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, 0, 1.0);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (Edge e : g.edges()) {
            System.out.println(e);
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }


    @Test public void testAdjacentEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedDirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);
    }

    @Test public void testAdjacencyListSize() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());
        
        Set<WeightedDirectedTypedEdge<String>> adjList = g.getAdjacencyList(0);
        assertEquals(9, adjList.size());

        adjList = g.getAdjacencyList(1);
        assertEquals(9, adjList.size());

        adjList = g.getAdjacencyList(2);
        assertEquals(9, adjList.size());

        adjList = g.getAdjacencyList(3);
        assertEquals(9, adjList.size());

        adjList = g.getAdjacencyList(5);
        assertEquals(9, adjList.size());
    }


    @Test public void testAdjacentEdgesRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedDirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        Edge removed = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0);
        assertTrue(test.remove(removed));
        assertTrue(control.remove(removed));
        assertEquals(control, test);
        assertEquals(99, g.size());
    }

    @Test public void testAdjacentEdgesAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> control = 
            new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            WeightedDirectedTypedEdge<String> e = 
                new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedDirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        WeightedDirectedTypedEdge<String> added = 
            new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 101, 1.000);
        assertTrue(test.add(added));
        assertTrue(control.add(added));
        assertEquals(control, test);
        assertEquals(101, g.size());
        assertTrue(g.contains(added));
        assertTrue(g.contains(101));
        assertEquals(102, g.order());
    }

    @Test public void testClear() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        g.clear();
        assertEquals(0, g.size());
        assertEquals(0, g.order());
        assertEquals(0, g.vertices().size());
        assertEquals(0, g.edges().size());
        
        // Error checking case for double-clear
        g.clear();
    }

    @Test public void testClearEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        g.clearEdges();
        assertEquals(0, g.size());
        assertEquals(10, g.order());
        assertEquals(10, g.vertices().size());
        assertEquals(0, g.edges().size());
        
        // Error checking case for double-clear
        g.clearEdges();
    }

    @Test public void testToString() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        g.toString();

        // only vertices
        g.clearEdges();
        g.toString();

        // empty graph
        g.clear();
        g.toString();
        
    }

    /******************************************************************
     *
     *
     * VertexSet tests 
     *
     *
     ******************************************************************/

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(vertices.add(100));
        assertTrue(g.contains(100));
        assertEquals(101, vertices.size());
        assertEquals(101, g.order());
        
        // dupe
        assertFalse(vertices.add(100));
        assertEquals(101, vertices.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetAddFromGraph() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.add(100));
        assertTrue(g.contains(100));
        assertTrue(vertices.contains(100));
        assertEquals(101, vertices.size());
        assertEquals(101, g.order());
        
        // dupe
        assertFalse(vertices.add(100));
        assertEquals(101, vertices.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.contains(99));
        assertTrue(vertices.remove(99));
        assertFalse(g.contains(99));
        assertEquals(99, vertices.size());
        assertEquals(99, g.order());
        
        // dupe
        assertFalse(vertices.remove(99));
        assertEquals(99, vertices.size());
    }

    @Test public void testVertexSetRemoveFromGraph() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.remove(99));

        assertFalse(g.contains(99));
        assertFalse(vertices.contains(99));
        assertEquals(99, vertices.size());
        assertEquals(99, g.order());        
    }

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        Iterator<Integer> iter = vertices.iterator();
        assertTrue(iter.hasNext());
        Integer toRemove = iter.next();
        assertTrue(g.contains(toRemove));
        assertTrue(vertices.contains(toRemove));
        iter.remove();
        assertFalse(g.contains(toRemove));
        assertFalse(vertices.contains(toRemove));
        assertEquals(g.order(), vertices.size());
    }

    @Test(expected=NoSuchElementException.class) public void testVertexSetIteratorTooFar() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        Iterator<Integer> iter = vertices.iterator();
        int i = 0;
        while (iter.hasNext()) {
            i++; 
            iter.next();
        }
        assertEquals(vertices.size(), i);
        iter.next();
    }

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetIteratorRemoveTwice() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        Iterator<Integer> iter = vertices.iterator();
        assertTrue(iter.hasNext());
        Integer toRemove = iter.next();
        assertTrue(g.contains(toRemove));
        assertTrue(vertices.contains(toRemove));
        iter.remove();
        iter.remove();
    }

    @Test(expected=UnsupportedOperationException.class) public void testVertexSetIteratorRemoveEarly() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.add(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        Iterator<Integer> iter = vertices.iterator();
        iter.remove();
    }
    

    /******************************************************************
     *
     *
     * EdgeView tests 
     *
     *
     ******************************************************************/

    @Test public void testEdgeViewAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertEquals(1, edges.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
    }

    @Test public void testEdgeViewRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0));
        edges.remove(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0));
        assertEquals(2, g.order());
        assertEquals(0, g.size());
        assertEquals(0, edges.size());
        assertFalse(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
    }

    @Test public void testEdgeViewIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> edges = g.edges();

        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 0; i < 100; i += 2)  {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, i+1, 1.0);
            g.add(e); // all disconnected
            control.add(e);
        }
    

        assertEquals(100, g.order());
        assertEquals(50, g.size());
        assertEquals(50, edges.size());
        
        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (WeightedDirectedTypedEdge<String> e : edges)
            test.add(e);
        assertEquals(control.size(), test.size());
        for (Edge e : test)
            assertTrue(control.contains(e));        
    }

    @Test public void testEdgeViewIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> edges = g.edges();

        Set<WeightedDirectedTypedEdge<String>> control = new HashSet<WeightedDirectedTypedEdge<String>>();
        for (int i = 0; i < 10; i += 2)  {
            WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, i+1, 1.0);
            g.add(e); // all disconnected
            control.add(e);
        }
    
        assertEquals(10, g.order());
        assertEquals(5, g.size());
        assertEquals(5, edges.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> iter = edges.iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        assertEquals(0, g.size());
        assertFalse(g.edges().iterator().hasNext());
        assertEquals(0, edges.size());
        assertEquals(10, g.order());            
    }

    /******************************************************************
     *
     *
     * AdjacencyListView tests 
     *
     *
     ******************************************************************/

    @Test public void testAdjacencyList() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));

        for (int i = 0; i < 10; ++i) {
            Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(i);
            assertEquals(9, adjacencyList.size());
            
            for (int j = 0; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                if (i >= j)
                    assertFalse(adjacencyList.contains(e));
                else 
                    assertTrue(adjacencyList.contains(e));
            }
        }
    }

    @Test public void testAdjacencyListRemoveEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));

        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        Edge e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0);
        assertTrue(adjacencyList.contains(e));
        assertTrue(adjacencyList.remove(e));
        assertEquals(8, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 1, g.size());
    }

    public void testAdjacencyListAddEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 2; j < 10; ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));

        assertEquals( (10 * 9) / 2 - 9, g.size());

        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0);
        assertFalse(adjacencyList.contains(e));
        assertFalse(g.contains(e));
        
        assertTrue(adjacencyList.add(e));
        assertTrue(g.contains(e));
      
        assertEquals(9, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 8, g.size());
    }

    @Test public void testAdjacencyListIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
    }

    @Test public void testAdjacencyListNoVertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);        
        assertEquals(0, adjacencyList.size());
    }

    @Test(expected=NoSuchElementException.class)
    public void testAdjacencyListIteratorNextOffEnd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
        it.next();
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacencyListIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> it = adjacencyList.iterator();
        assertTrue(it.hasNext());
        Edge e = it.next();
        it.remove();
    }

    @Test(expected=IllegalStateException.class)
    public void testAdjacencyListIteratorRemoveFirst() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> it = adjacencyList.iterator();
        it.remove();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAdjacencyListIteratorRemoveTwice() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<WeightedDirectedTypedEdge<String>> test = new HashSet<WeightedDirectedTypedEdge<String>>();
        Set<WeightedDirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedDirectedTypedEdge<String>> it = adjacencyList.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        it.remove();
    }

    /******************************************************************
     *
     *
     * AdjacentVerticesView tests 
     *
     *
     ******************************************************************/


    @Test public void testAdjacentVertices() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        assertEquals(9, adjacent.size());
        for (int i = 1; i < 10; ++i)
            assertTrue(adjacent.contains(i));
        assertFalse(adjacent.contains(0));
        assertFalse(adjacent.contains(10));
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacentVerticesAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.add(1);
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacentVerticesRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.remove(1);
    }

    @Test public void testAdjacentVerticesIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        Iterator<Integer> it = adjacent.iterator();
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
    }


    @Test(expected=UnsupportedOperationException.class) public void testAdjacentVerticesIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        Iterator<Integer> it = adjacent.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
    }

    /******************************************************************
     *
     *
     * Subgraph tests 
     *
     *
     ******************************************************************/

    @Test public void testSubgraph() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
    }

    @Test public void testSubgraphContainsVertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);        
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
        for (int i = 0; i < 5; ++i)
            assertTrue(subgraph.contains(i));
        for (int i = 5; i < 10; ++i) {
            assertTrue(g.contains(i));
            assertFalse(subgraph.contains(i));
        }
    }

    @Test public void testSubgraphContainsEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);        
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
        for (int i = 0; i < 5; ++i) {
            for (int j = i+1; j < 5; ++j) {
                assertTrue(subgraph.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0)));
            }
        }

        for (int i = 5; i < 10; ++i) {
            for (int j = i+1; j < 10; ++j) {
                WeightedDirectedTypedEdge<String> e = new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0);
                assertTrue(g.contains(e));
                assertFalse(subgraph.contains(e));
            }
        }
    }

    @Test public void testSubgraphAddEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < i+2 && j < 10; ++j)
                assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0)));
        }    

        assertEquals(9, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals(4, subgraph.size());

        // Add an edge to a new vertex
        assertTrue(subgraph.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 0, 1.0)));
        assertEquals(5, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.size());

    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAddEdgeNewVertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Add an edge to a new vertex
        assertTrue(subgraph.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 5, 1.0)));
        assertEquals( (5 * 4) / 2 + 1, subgraph.size());
        assertEquals(6, subgraph.order());
        assertEquals(11, g.order());
        assertEquals( (9*10)/2 + 1, g.size());
    }

    @Test public void testSubgraphRemoveEdge() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Remove an existing edge
        assertTrue(subgraph.remove(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertEquals( (5 * 4) / 2 - 1, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.order());
        assertEquals( (9*10)/2 - 1, g.size());

        // Remove a non-existent edge, which should have no effect even though
        // the edge is present in the backing graph
        assertFalse(subgraph.remove(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 6, 1.0)));
        assertEquals( (5 * 4) / 2 - 1, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.order());
        assertEquals( (9*10)/2 - 1, g.size());
    }


    /******************************************************************
     *
     *
     * SubgraphVertexView tests 
     *
     *
     ******************************************************************/


    @Test(expected=UnsupportedOperationException.class) public void testSubgraphVerticesAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        Set<Integer> test = subgraph.vertices();
        assertEquals(5, test.size());

        // Add a vertex 
        assertTrue(test.add(5));
        assertEquals(6, test.size());
        assertEquals(6, subgraph.order());
        assertEquals(11, g.order());
        assertEquals( (5*4)/2, subgraph.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphVerticesRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        Set<Integer> test = subgraph.vertices();
        assertEquals(5, test.size());

        // Add a vertex 
        assertTrue(test.remove(0));
        assertEquals(4, test.size());
        assertEquals(4, subgraph.order());
        assertEquals(9, g.order());
        assertEquals( (4*3)/2, subgraph.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphVerticesIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        Set<Integer> test = subgraph.vertices();
        assertEquals(5, test.size());
        Iterator<Integer> it = test.iterator();
        assertTrue(it.hasNext());
        // Remove the first vertex returned
        it.next();
        it.remove();
        
        assertEquals(4, test.size());
        assertEquals(4, subgraph.order());
        assertEquals(9, g.order());
        assertEquals( (4*3)/2, subgraph.size());
    }


    /******************************************************************
     *
     *
     * SubgraphEdgeView tests 
     *
     *
     ******************************************************************/

    @Test public void testSubgraphEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            g.add(i);
        }
        g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0));
        g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 2, 1.0));
        g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",1, 2, 1.0));

        assertEquals(3, g.size());

        Set<Integer> verts = new HashSet<Integer>();
        for (int i = 0; i < 3; ++i)
            verts.add(i);

        WeightedDirectedMultigraph<String> sub = g.subgraph(verts);
        assertEquals(3, sub.order());
        assertEquals(3, sub.size());
     
        Set<WeightedDirectedTypedEdge<String>> edges = sub.edges();
        assertEquals(3, edges.size());
        int j = 0; 
        Iterator<WeightedDirectedTypedEdge<String>> iter = edges.iterator();
        while (iter.hasNext()) {
            iter.next();
            j++;
        }
        assertEquals(3, j);

        verts.clear();
        for (int i = 3; i < 6; ++i)
            verts.add(i);

        sub = g.subgraph(verts);
        assertEquals(3, sub.order());
        assertEquals(0, sub.size());
    
        edges = sub.edges();
        assertEquals(0, edges.size());

        iter = edges.iterator();
        assertFalse(iter.hasNext());
    }


    /******************************************************************
     *
     *
     * SubgraphAdjacencyListView tests 
     *
     *
     ******************************************************************/


    @Test public void testSubgraphAdjacencyListContains() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<WeightedDirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);

        for (int i = 1; i < 5; ++i)
            assertTrue(adjList.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0)));

        for (int i = 5; i < 10; ++i)
            assertFalse(adjList.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, i, 1.0)));
    }

    @Test public void testSubgraphAdjacencyListSize() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<WeightedDirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);
        assertEquals(4, adjList.size());

        adjList = subgraph.getAdjacencyList(1);
        assertEquals(4, adjList.size());

        adjList = subgraph.getAdjacencyList(2);
        assertEquals(4, adjList.size());

        adjList = subgraph.getAdjacencyList(3);
        assertEquals(4, adjList.size());

        adjList = subgraph.getAdjacencyList(4);
        assertEquals(4, adjList.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAdjacencyListAddNewVertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<WeightedDirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);
        // Add an edge to a new vertex
        assertTrue(adjList.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 5, 1.0)));
    }

    /******************************************************************
     *
     *
     * SubgraphAdjacentVerticesView tests 
     *
     *
     ******************************************************************/

    @Test public void testSubgraphAdjacentVertices() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        assertEquals(4, adjacent.size());
        // check contents
        for (int i = 1; i < 5; ++i)
            assertTrue(adjacent.contains(i));

        adjacent = subgraph.getNeighbors(1);
        assertEquals(4, adjacent.size());

        adjacent = subgraph.getNeighbors(2);
        assertEquals(4, adjacent.size());

        adjacent = subgraph.getNeighbors(3);
        assertEquals(4, adjacent.size());

        adjacent = subgraph.getNeighbors(4);
        assertEquals(4, adjacent.size());

        adjacent = subgraph.getNeighbors(5);
        assertEquals(0, adjacent.size());

        
    }


    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAdjacentVerticesAdd() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.add(0);
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAdjacentVerticesRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.remove(0);
    }

    @Test public void testSubgraphAdjacentVerticesIterator() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        vertices.remove(0); // now is the adjacent vertices of 0

        Set<Integer> adjacent = subgraph.getNeighbors(0);
        assertEquals(vertices, adjacent);
        Iterator<Integer> it = adjacent.iterator();
        int i = 0;
        while (it.hasNext()) {
            i++;
            vertices.remove(it.next());
        }
        assertEquals(4, i);
        assertEquals(0, vertices.size());        
    }

    @Test(expected=UnsupportedOperationException.class) 
    public void testSubgraphAdjacentVerticesIteratorRemove() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        vertices.remove(0); // now is the adjacent vertices of 0
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);        
        assertEquals(vertices, adjacent);
        Iterator<Integer> it = adjacent.iterator();
        assertTrue(it.hasNext());
        it.next();
        it.remove();
    }

    /******************************************************************
     *
     *
     * Tests that create a subgraph and then modify the backing graph
     *
     *
     ******************************************************************/

    @Test public void testSubgraphWhenVerticesRemovedFromBacking() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);

        g.remove(0);
        assertEquals(4, subgraph.order());
        assertEquals((3 * 4) / 2, subgraph.size());

        g.remove(1);
        assertFalse(subgraph.contains(1));

        g.remove(2);
        assertFalse(subgraph.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",2,3, 1.0)));
    }

    @Test public void testSubgraphVertexIteratorWhenVerticesRemovedFromBacking() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        g.remove(0);
        Iterator<Integer> it = subgraph.vertices().iterator();
        int i = 0;
        while (it.hasNext()) {
            assertTrue(0 != it.next());
            i++;
        }
        assertEquals(4, i);
    }

    @Test public void testSubgraphEdgesWhenVerticesRemovedFromBacking() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",i, j, 1.0));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        WeightedDirectedMultigraph<String> subgraph = g.subgraph(vertices);
        Set<WeightedDirectedTypedEdge<String>> edges = subgraph.edges();
        g.remove(0);
        assertEquals((4 * 3) / 2, edges.size());
        assertFalse(edges.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
    }

    /****************
     *
     *
     * Tests on graphs with multiple edge types
     *
     *
     ****************/

    @Test public void testAddTypedEdges() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertEquals(1, g.edgeTypes().size());

        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertEquals(2, g.order());
        assertEquals(2, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertEquals(2, g.edgeTypes().size());

        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));
        assertEquals(4, g.order());
        assertEquals(3, g.size());
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3",3, 4, 1.0)));
        assertEquals(3, g.edgeTypes().size());
    }

    @Test public void testNeighborsOfDifferentTypes() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 0, 3, 1.0)));

        Set<Integer> neighbors = g.getNeighbors(0);
        assertEquals(3, g.size());
        assertEquals(3, neighbors.size());
        assertTrue(neighbors.contains(1));
        assertTrue(neighbors.contains(2));
        assertTrue(neighbors.contains(3));

        // Test bi-directional case
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 0, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 2, 0, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 0, 1.0)));

        neighbors = g.getNeighbors(0);
        assertEquals(3, neighbors.size());
        assertEquals(6, g.size());

        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 4, 0, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 5, 0, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 6, 0, 1.0)));

        neighbors = g.getNeighbors(0);
        assertEquals(6, neighbors.size());
        assertEquals(9, g.size());
        for (int i = 1; i <= 6; ++i)
            assertTrue(neighbors.contains(i));
    }

    @Test public void testSubgraphOfSubtype() {
        // set up the network
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 3, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));

        
        Set<Integer> verts = new HashSet<Integer>();
        verts.add(0);
        verts.add(1);
        Set<String> types = Collections.singleton("type-1");
        WeightedDirectedMultigraph<String> sub = g.subgraph(g.vertices(), types);
        assertEquals(1, sub.edgeTypes().size());
        assertEquals(3, sub.size());
        assertEquals(g.order(), sub.order());       
        assertFalse(sub.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));        
    }

    /****************
     *
     *
     * Tests for copy()
     *
     *
     ****************/

    @Test public void testCopyAllVertices() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3",3, 4, 1.0)));

        WeightedDirectedMultigraph<String> copy = g.copy(g.vertices());
        assertEquals(g.order(), copy.order());
        assertEquals(g.size(), copy.size());
        assertEquals(g, copy);

        copy.remove(4);
        assertEquals(4, g.order());
        assertEquals(3, copy.order());
    }

    @Test public void testCopy1vertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3",3, 4, 1.0)));

        WeightedDirectedMultigraph<String> copy = g.copy(Collections.singleton(1));
        assertEquals(1, copy.order());
        assertEquals(0, copy.size());
    }

    @Test public void testCopy2vertex() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3",3, 4, 1.0)));

        Set<Integer> verts = new HashSet<Integer>();
        Collections.addAll(verts, 0, 1);
        WeightedDirectedMultigraph<String> copy = g.copy(verts);
        assertEquals(2, copy.order());
        assertEquals(2, copy.size());
    }


    @Test public void testCopy3vertexTriangle() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 2, 0, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 2, 3, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));

        Set<Integer> verts = new HashSet<Integer>();
        Collections.addAll(verts, 0, 1, 2);
        WeightedDirectedMultigraph<String> copy = g.copy(verts);
        assertEquals(3, copy.order());
        assertEquals(7, copy.size());
    }

    @Test public void testCopy3vertexVee() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 1, 2, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1", 2, 3, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));

        Set<Integer> verts = new HashSet<Integer>();
        Collections.addAll(verts, 0, 1, 2);
        WeightedDirectedMultigraph<String> copy = g.copy(verts);
        assertEquals(3, copy.order());
        assertEquals(5, copy.size());
    }


    @Test public void testEmptyCopy() {
        WeightedDirectedMultigraph<String> g = new WeightedDirectedMultigraph<String>();
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-1",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-2",0, 1, 1.0)));
        assertTrue(g.add(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));
        assertTrue(g.contains(new SimpleWeightedDirectedTypedEdge<String>("type-3", 3, 4, 1.0)));

        WeightedDirectedMultigraph<String> copy = g.copy(Collections.<Integer>emptySet());
        assertEquals(0, copy.order());
        assertEquals(0, copy.size());
        assertEquals(new WeightedDirectedMultigraph<String>(), copy);
        
        copy.add(5);
        assertTrue(copy.contains(5));
        assertFalse(g.contains(5));
    }


    /*
     * To test:
     *
     * - remove vertex causes edge type to no longer be present (graph + subgraph)
     * - remove a vertex and then add the vertex back in (not present in subgraph)
     * - all of the DirectedGraph methods
     *
     */
    

}