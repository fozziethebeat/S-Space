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
 * Unit tests for {@link SparseWeightedGraph}
 */
public class SparseWeightedGraphTests { 

    @Test public void testConstructor() {
        Set<Integer> vertices = new HashSet<Integer>();
        for (int i = 0; i < 10; ++i)
            vertices.add(i);
        Graph<WeightedEdge> g = new SparseWeightedGraph(vertices);
        assertEquals(vertices.size(), g.order());
    }

    @Test(expected=NullPointerException.class) public void testConstructor1NullArg() {
        Graph<WeightedEdge> g = new SparseWeightedGraph((Graph<? extends WeightedEdge>)null);
    }

    @Test(expected=NullPointerException.class) public void testConstructor2NullArg() {
        Graph<WeightedEdge> g = new SparseWeightedGraph((Set<Integer>)null);
    }

    @Test public void testAddVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test public void testContainsEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 100; ++i)
            for (int j = i + 1; j < 100; ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));

        for (int i = 0; i < 100; ++i) {
            for (int j = i + 1; j < 100; ++j) {
                g.contains(new SimpleWeightedEdge(i, j, .5));
                g.contains(new SimpleWeightedEdge(j, i, .5));
                g.contains(i, j);
                g.contains(j, i);
            }
        }
    }

    @Test public void testAddEdgeDifferentWeights() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        assertTrue(g.add(new SimpleWeightedEdge(0, 1, .5)));
        assertFalse(g.add(new SimpleWeightedEdge(0, 1, .7)));
    }

    @Test public void testAddEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        g.add(new SimpleWeightedEdge(0, 1, .5));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleWeightedEdge(0, 1, .5)));

        g.add(new SimpleWeightedEdge(0, 2, .5));
        assertEquals(3, g.order());
        assertEquals(2, g.size());
        assertTrue(g.contains(new SimpleWeightedEdge(0, 2, .5)));

        g.add(new SimpleWeightedEdge(3, 4, .5));
        assertEquals(5, g.order());
        assertEquals(3, g.size());
        assertTrue(g.contains(new SimpleWeightedEdge(3, 4, .5)));
        
        // Change the weight but not the size/order
        g.add(new SimpleWeightedEdge(3, 4, .8));
        assertEquals(5, g.order());
        assertEquals(3, g.size());
        assertFalse(g.contains(new SimpleWeightedEdge(3, 4, .5)));
        assertTrue(g.contains(new SimpleWeightedEdge(3, 4, .8)));
    }

    @Test public void testGetEdges() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        g.add(new SimpleWeightedEdge(0, 1, .5));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleWeightedEdge(0, 1, .5)));

        Set<WeightedEdge> edges = g.getEdges(0, 1);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new SimpleWeightedEdge(0, 1, .5)));

        assertEquals(0, g.getEdges(0, 4).size());
        assertFalse(g.add(new SimpleWeightedEdge(1, 0, .5)));
        edges = g.getEdges(0, 1);
        assertEquals(1, edges.size());
        assertTrue(edges.contains(new SimpleWeightedEdge(0, 1, .5)));
        assertTrue(edges.contains(new SimpleWeightedEdge(1, 0, .5)));
        assertFalse(edges.contains(new SimpleWeightedEdge(1, 0, .8)));
    }

    @Test public void testRemoveLesserVertexWithEdges() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        for (int i = 1; i < 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
        }
       
        assertTrue(g.contains(0));
        assertTrue(g.remove(0));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }

    @Test public void testRemoveHigherVertexWithEdges() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        for (int i = 0; i < 99; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(100, i, .5);
            g.add(e);
        }
        
        assertTrue(g.contains(100));
        assertTrue(g.remove(100));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }


    @Test public void testRemoveVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        for (int i = 1; i < 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
        }
        
        for (int i = 99; i > 0; --i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            assertTrue(g.remove(e));
            assertEquals(i-1, g.size());
            assertFalse(g.contains(e));
            assertFalse(g.remove(e));
        }
    }

    @Test public void testVertexIterator() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (WeightedEdge e : g.edges()) {
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }

    @Test public void testEdgeIteratorSmall() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 5; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (WeightedEdge e : g.edges()) {
            //System.out.println(e);
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }

    @Test public void testEdgeIteratorSmallReverse() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 5; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(i, 0, .5);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (WeightedEdge e : g.edges()) {
            System.out.println("reverse case: " + e);
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }

    @Test public void testEdgeIteratorBidirectionalEdges() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        for (int i = 0; i < 3; i++)  {
            for (int j = i+1; j < 3; j++)  {
                // Only one edge is really added
                g.add(new SimpleWeightedEdge(i, j, .5));
                g.add(new SimpleWeightedEdge(j, i, .5));
            }
        }
        
        int numEdges = 0;
        for (WeightedEdge e : g.edges()) {
            numEdges++;
        }
        assertEquals(3, numEdges);
    }

    @Test public void testAdjacentEdges() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedEdge> test = g.getAdjacencyList(0);
        assertEquals(control, test);
    }

    @Test public void testAdjacencyListSize() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());
        
        Set<WeightedEdge> adjList = g.getAdjacencyList(0);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedEdge> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        Edge removed = new SimpleWeightedEdge(0, 1, .5);
        assertTrue(test.remove(removed));
        assertTrue(control.remove(removed));
        assertEquals(control, test);
        assertEquals(99, g.size());
    }

    @Test public void testAdjacentEdgesAdd() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 1; i <= 100; ++i) {
            WeightedEdge e = new SimpleWeightedEdge(0, i, .5);
            g.add(e);
            control.add(e);
        }
        
        Set<WeightedEdge> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        WeightedEdge added = new SimpleWeightedEdge(0, 101, .5);
        assertTrue(test.add(added));
        assertTrue(control.add(added));
        assertEquals(control, test);
        assertEquals(101, g.size());
        assertTrue(g.contains(added));
        assertTrue(g.contains(101));
        assertEquals(102, g.order());
    }

    @Test public void testClear() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
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

    @Test public void testVertexSetAdd() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test public void testVertexSetAddFromGraph() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test public void testVertexSetRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test public void testVertexSetIteratorRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test(expected=IllegalStateException.class) public void testVertexSetIteratorRemoveTwice() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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

    @Test(expected=IllegalStateException.class) public void testVertexSetIteratorRemoveEarly() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleWeightedEdge(0, 1, .5));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertEquals(1, edges.size());
        assertTrue(g.contains(new SimpleWeightedEdge(0, 1, .5)));
        assertTrue(edges.contains(new SimpleWeightedEdge(0, 1, .5)));
    }

    @Test public void testEdgeViewRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleWeightedEdge(0, 1, .5));
        edges.remove(new SimpleWeightedEdge(0, 1, .5));
        assertEquals(2, g.order());
        assertEquals(0, g.size());
        assertEquals(0, edges.size());
        assertFalse(g.contains(new SimpleWeightedEdge(0, 1, .5)));
        assertFalse(edges.contains(new SimpleWeightedEdge(0, 1, .5)));
    }

    @Test public void testEdgeViewIterator() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> edges = g.edges();

        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 0; i < 100; i += 2)  {
            WeightedEdge e = new SimpleWeightedEdge(i, i+1, .5);
            g.add(e); // all disconnected
            control.add(e);
        }
    

        assertEquals(100, g.order());
        assertEquals(50, g.size());
        assertEquals(50, edges.size());
        
        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        for (WeightedEdge e : edges)
            test.add(e);
        assertEquals(control.size(), test.size());
        for (WeightedEdge e : test)
            assertTrue(control.contains(e));        
    }

    @Test public void testEdgeViewIteratorRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> edges = g.edges();

        Set<WeightedEdge> control = new HashSet<WeightedEdge>();
        for (int i = 0; i < 10; i += 2)  {
            WeightedEdge e = new SimpleWeightedEdge(i, i+1, .5);
            g.add(e); // all disconnected
            control.add(e);
        }
    
        assertEquals(10, g.order());
        assertEquals(5, g.size());
        assertEquals(5, edges.size());
        
        Iterator<WeightedEdge> iter = edges.iterator();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));

        for (int i = 0; i < 10; ++i) {
            Set<WeightedEdge> adjacencyList = g.getAdjacencyList(i);
            assertEquals(9, adjacencyList.size());
            
            for (int j = 0; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                if (i == j)
                    assertFalse(adjacencyList.contains(e));
                else
                    assertTrue(adjacencyList.contains(e));
            }
        }
    }

    @Test public void testAdjacencyListRemoveEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));

        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        WeightedEdge e = new SimpleWeightedEdge(0, 1, .5);
        assertTrue(adjacencyList.contains(e));
        assertTrue(adjacencyList.remove(e));
        assertEquals(8, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 1, g.size());
    }

    @Test public void testAdjacencyListAddEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 2; j < 10; ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));

        assertEquals( (10 * 9) / 2 - 9, g.size());

        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        WeightedEdge e = new SimpleWeightedEdge(0, 1, .5);
        assertFalse(adjacencyList.contains(e));
        assertFalse(g.contains(e));
        
        assertTrue(adjacencyList.add(e));
        assertTrue(g.contains(e));
      
        assertEquals(9, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 8, g.size());
    }

    @Test public void testAdjacencyListIterator() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedEdge> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
    }

    @Test public void testAdjacencyListNoVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);        
        assertEquals(0, adjacencyList.size());
    }

    @Test(expected=NoSuchElementException.class)
    public void testAdjacencyListIteratorNextOffEnd() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedEdge> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
        it.next();
    }

    @Test public void testAdjacencyListIteratorRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedEdge> it = adjacencyList.iterator();
        assertTrue(it.hasNext());
        WeightedEdge e = it.next();
        it.remove();
        assertFalse(adjacencyList.contains(e));
        assertEquals(8, adjacencyList.size());
        assertFalse(g.contains(e));
        assertEquals( (10 * 9) / 2 - 1, g.size());
    }

    @Test(expected=IllegalStateException.class)
    public void testAdjacencyListIteratorRemoveFirst() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedEdge> it = adjacencyList.iterator();
        it.remove();
    }

    @Test(expected=IllegalStateException.class)
    public void testAdjacencyListIteratorRemoveTwice() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<WeightedEdge> test = new HashSet<WeightedEdge>();
        Set<WeightedEdge> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<WeightedEdge> it = adjacencyList.iterator();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.add(1);
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacentVerticesRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.remove(1);
    }

    @Test public void testAdjacentVerticesIterator() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
    }

    @Test public void testSubgraphContainsVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);        
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);        
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
        for (int i = 0; i < 5; ++i) {
            for (int j = i+1; j < 5; ++j) {
                assertTrue(subgraph.contains(new SimpleWeightedEdge(i, j, .5)));
            }
        }

        for (int i = 5; i < 10; ++i) {
            for (int j = i+1; j < 10; ++j) {
                WeightedEdge e = new SimpleWeightedEdge(i, j, .5);
                assertTrue(g.contains(e));
                assertFalse(subgraph.contains(e));
            }
        }
    }

    @Test public void testSubgraphAddEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
    }


    @Test public void testSubgraphAddEdgeNewVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Add an edge to a new vertex
        assertFalse(subgraph.add(new SimpleWeightedEdge(0, 5, .5)));
        assertEquals( (5 * 4) / 2, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.order());
        assertEquals( (9*10)/2, g.size());
    }

    @Test public void testSubgraphRemoveEdge() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Remove an existing edge
        assertTrue(subgraph.remove(new SimpleWeightedEdge(0, 1, .5)));
        assertEquals( (5 * 4) / 2 - 1, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.order());
        assertEquals( (9*10)/2 - 1, g.size());

        // Remove a non-existent edge, which should have no effect even though
        // the edge is present in the backing graph
        assertFalse(subgraph.remove(new SimpleWeightedEdge(0, 6, .5)));
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            g.add(i);
        }
        g.add(new SimpleWeightedEdge(0, 1, .5));
        g.add(new SimpleWeightedEdge(0, 2, .5));
        g.add(new SimpleWeightedEdge(1, 2, .5));

        assertEquals(3, g.size());

        Set<Integer> verts = new HashSet<Integer>();
        for (int i = 0; i < 3; ++i)
            verts.add(i);

        Graph<WeightedEdge> sub = g.subgraph(verts);
        assertEquals(3, sub.order());
        assertEquals(3, sub.size());
     
        Set<WeightedEdge> edges = sub.edges();
        assertEquals(3, edges.size());
        int j = 0; 
        Iterator<WeightedEdge> iter = edges.iterator();
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<WeightedEdge> adjList = subgraph.getAdjacencyList(0);

        for (int i = 1; i < 5; ++i)
            assertTrue(adjList.contains(new SimpleWeightedEdge(0, i, .5)));

        for (int i = 5; i < 10; ++i)
            assertFalse(adjList.contains(new SimpleWeightedEdge(0, i, .5)));
    }

    @Test public void testSubgraphAdjacencyListSize() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<WeightedEdge> adjList = subgraph.getAdjacencyList(0);
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

    @Test public void testSubgraphAdjacencyListAdd() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        Set<WeightedEdge> adjList = subgraph.getAdjacencyList(0);
        int size = adjList.size();
        boolean added = adjList.add(new SimpleWeightedEdge(0, 10, .5));

        // Add an edge to a new vertex not in the subgraph
        assertFalse(adjList.add(new SimpleWeightedEdge(0, 10, .5)));
        assertEquals(size, adjList.size());        

        // Add an edge to a vertice that are not the root
        assertFalse(adjList.add(new SimpleWeightedEdge(1, 4, .5)));
        assertEquals(size, adjList.size());        
    }

    @Test public void testSubgraphAdjacencyListAddNewVertex() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<WeightedEdge> adjList = subgraph.getAdjacencyList(0);
        // Add an edge to a new vertex
        assertFalse(adjList.add(new SimpleWeightedEdge(0, 5, .5)));
    }

    /******************************************************************
     *
     *
     * SubgraphAdjacentVerticesView tests 
     *
     *
     ******************************************************************/

    @Test public void testSubgraphAdjacentVertices() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        assertEquals(4, adjacent.size());

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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.add(0);
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAdjacentVerticesRemove() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.remove(0);
    }

    @Test public void testSubgraphAdjacentVerticesIterator() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);

        g.remove(0);
        assertEquals(4, subgraph.order());
        assertEquals((3 * 4) / 2, subgraph.size());

        g.remove(1);
        assertFalse(subgraph.contains(1));

        g.remove(2);
        assertFalse(subgraph.contains(new SimpleWeightedEdge(2, 3, .5)));
    }

    @Test public void testSubgraphVertexIteratorWhenVerticesRemovedFromBacking() {
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
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
        Graph<WeightedEdge> g = new SparseWeightedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleWeightedEdge(i, j, .5));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        Graph<WeightedEdge> subgraph = g.subgraph(vertices);
        Set<WeightedEdge> edges = subgraph.edges();
        g.remove(0);
        assertEquals((4 * 3) / 2, edges.size());
        assertFalse(edges.contains(new SimpleWeightedEdge(0, 1, .5)));
    }

}