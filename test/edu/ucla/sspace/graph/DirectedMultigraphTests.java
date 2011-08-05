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
public class DirectedMultigraphTests { 

    @Test public void testConstructor() {
        Set<Integer> vertices = new HashSet<Integer>();
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertEquals(0, g.order());
        assertEquals(0, g.size());
    }

    @Test(expected=NullPointerException.class) public void testConstructor2NullArg() {
        Graph<Edge> g = new SparseUndirectedGraph((Graph<DirectedTypedEdge<String>>)null);
    }

    @Test public void testAdd() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 100; ++i)
            for (int j = i + 1; j < 100; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));

        for (int i = 0; i < 100; ++i) {
            for (int j = i + 1; j < 100; ++j) {
                g.contains(new SimpleDirectedTypedEdge<String>("type-1",i, j));
                g.contains(new SimpleDirectedTypedEdge<String>("type-1",j, i));
                g.contains(i, j);
                g.contains(j, i);
            }
        }
    }

    @Test public void testAddEdge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));

        g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 2));
        assertEquals(3, g.order());
        assertEquals(2, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 2)));

        g.add(new SimpleDirectedTypedEdge<String>("type-1",3, 4));
        assertEquals(5, g.order());
        assertEquals(3, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",3, 4)));
    }

    @Test public void testRemoveLesserVertexWithEdges() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        for (int i = 1; i < 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
        }
       
        assertTrue(g.contains(0));
        assertTrue(g.remove(0));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }

    @Test public void testRemoveHigherVertexWithEdges() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        for (int i = 0; i < 99; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",100, i);
            g.add(e);
        }
        
        assertTrue(g.contains(100));
        assertTrue(g.remove(100));
        assertEquals(99, g.order());
        assertEquals(0, g.size());
    }


    @Test public void testRemoveVertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        for (int i = 1; i < 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
        }
        
        for (int i = 99; i > 0; --i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            assertTrue(g.remove(e));
            assertEquals(i-1, g.size());
            assertFalse(g.contains(e));
            assertFalse(g.remove(e));
        }
    }

    @Test public void testVertexIterator() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
            control.add(e);
        }

        assertEquals(control.size(), g.size());
        assertEquals(control.size(), g.edges().size());
        int returned = 0;
        for (Edge e : g.edges()) {
            assertTrue(control.contains(e));
            returned++;
        }
        assertEquals(control.size(), returned);
    }

    @Test public void testEdgeIteratorSmall() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 5; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 5; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, 0);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
            control.add(e);
        }
        
        Set<DirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);
    }
    @Test public void testAdjacencyListSize() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());
        
        Set<DirectedTypedEdge<String>> adjList = g.getAdjacencyList(0);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
            control.add(e);
        }
        
        Set<DirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        Edge removed = new SimpleDirectedTypedEdge<String>("type-1",0, 1);
        assertTrue(test.remove(removed));
        assertTrue(control.remove(removed));
        assertEquals(control, test);
        assertEquals(99, g.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacentEdgesAdd() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 1; i <= 100; ++i) {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, i);
            g.add(e);
            control.add(e);
        }
        
        Set<DirectedTypedEdge<String>> test = g.getAdjacencyList(0);
        assertEquals(control, test);

        DirectedTypedEdge<String> added = new SimpleDirectedTypedEdge<String>("type-1",0, 101);
        assertTrue(test.add(added));
        assertTrue(control.add(added));
        assertEquals(control, test);
        assertEquals(101, g.size());
        assertTrue(g.contains(added));
        assertTrue(g.contains(101));
        assertEquals(102, g.order());
    }

    @Test public void testClear() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertEquals(1, edges.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(edges.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
    }

    @Test public void testEdgeViewRemove() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> edges = g.edges();
        assertEquals(g.size(), edges.size());
        edges.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1));
        edges.remove(new SimpleDirectedTypedEdge<String>("type-1",0, 1));
        assertEquals(2, g.order());
        assertEquals(0, g.size());
        assertEquals(0, edges.size());
        assertFalse(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertFalse(edges.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
    }

    @Test public void testEdgeViewIterator() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> edges = g.edges();

        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 0; i < 100; i += 2)  {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, i+1);
            g.add(e); // all disconnected
            control.add(e);
        }
    

        assertEquals(100, g.order());
        assertEquals(50, g.size());
        assertEquals(50, edges.size());
        
        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        for (DirectedTypedEdge<String> e : edges)
            test.add(e);
        assertEquals(control.size(), test.size());
        for (Edge e : test)
            assertTrue(control.contains(e));        
    }

    @Test public void testEdgeViewIteratorRemove() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> edges = g.edges();

        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        for (int i = 0; i < 10; i += 2)  {
            DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, i+1);
            g.add(e); // all disconnected
            control.add(e);
        }
    
        assertEquals(10, g.order());
        assertEquals(5, g.size());
        assertEquals(5, edges.size());
        
        Iterator<DirectedTypedEdge<String>> iter = edges.iterator();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));

        for (int i = 0; i < 10; ++i) {
            Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(i);
            assertEquals(9, adjacencyList.size());
            
            for (int j = 0; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                if (i >= j)
                    assertFalse(adjacencyList.contains(e));
                else 
                    assertTrue(adjacencyList.contains(e));
            }
        }
    }

    @Test public void testAdjacencyListRemoveEdge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 1; j < 10; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));

        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        Edge e = new SimpleDirectedTypedEdge<String>("type-1",0, 1);
        assertTrue(adjacencyList.contains(e));
        assertTrue(adjacencyList.remove(e));
        assertEquals(8, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 1, g.size());
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacencyListAddEdge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i)
            for (int j = i + 2; j < 10; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));

        assertEquals( (10 * 9) / 2 - 9, g.size());

        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",0, 1);
        assertFalse(adjacencyList.contains(e));
        assertFalse(g.contains(e));
        
        assertTrue(adjacencyList.add(e));
        assertTrue(g.contains(e));
      
        assertEquals(9, adjacencyList.size());
        assertEquals( (10 * 9) / 2 - 8, g.size());
    }

    @Test public void testAdjacencyListIterator() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<DirectedTypedEdge<String>> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
    }

    @Test public void testAdjacencyListNoVertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);        
        assertEquals(0, adjacencyList.size());
    }

    @Test(expected=NoSuchElementException.class)
    public void testAdjacencyListIteratorNextOffEnd() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<DirectedTypedEdge<String>> it = adjacencyList.iterator();
        int i = 0;
        while (it.hasNext())
            assertTrue(test.add(it.next()));
        assertEquals(9, test.size());
        it.next();
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacencyListIteratorRemove() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<DirectedTypedEdge<String>> it = adjacencyList.iterator();
        assertTrue(it.hasNext());
        Edge e = it.next();
        it.remove();
        assertFalse(adjacencyList.contains(e));
        assertEquals(8, adjacencyList.size());
        assertFalse(g.contains(e));
        assertEquals( (10 * 9) / 2 - 1, g.size());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAdjacencyListIteratorRemoveFirst() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<DirectedTypedEdge<String>> it = adjacencyList.iterator();
        it.remove();
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testAdjacencyListIteratorRemoveTwice() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<DirectedTypedEdge<String>> test = new HashSet<DirectedTypedEdge<String>>();
        Set<DirectedTypedEdge<String>> adjacencyList = g.getAdjacencyList(0);
        assertEquals(9, adjacencyList.size());
        
        Iterator<DirectedTypedEdge<String>> it = adjacencyList.iterator();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.add(1);
    }

    @Test(expected=UnsupportedOperationException.class) public void testAdjacentVerticesRemove() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                g.add(e);
            }
        }

        Set<Integer> test = new HashSet<Integer>();
        Set<Integer> adjacent = g.getNeighbors(0);
        adjacent.remove(1);
    }

    @Test public void testAdjacentVerticesIterator() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        for (int i = 0; i < 10; ++i) {
            for (int j = i + 1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
    }

    @Test public void testSubgraphContainsVertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);        
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);        
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());
        for (int i = 0; i < 5; ++i) {
            for (int j = i+1; j < 5; ++j) {
                assertTrue(subgraph.contains(new SimpleDirectedTypedEdge<String>("type-1",i, j)));
            }
        }

        for (int i = 5; i < 10; ++i) {
            for (int j = i+1; j < 10; ++j) {
                DirectedTypedEdge<String> e = new SimpleDirectedTypedEdge<String>("type-1",i, j);
                assertTrue(g.contains(e));
                assertFalse(subgraph.contains(e));
            }
        }
    }

    @Test public void testSubgraphAddEdge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < i+2 && j < 10; ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        assertEquals(9, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals(4, subgraph.size());

        // Add an edge to a new vertex
        assertTrue(subgraph.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertEquals(5, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.size());

    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAddEdgeNewVertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Add an edge to a new vertex
        assertTrue(subgraph.add(new SimpleDirectedTypedEdge<String>("type-1",0, 5)));
        assertEquals( (5 * 4) / 2 + 1, subgraph.size());
        assertEquals(6, subgraph.order());
        assertEquals(11, g.order());
        assertEquals( (9*10)/2 + 1, g.size());
    }

    @Test public void testSubgraphRemoveEdge() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        assertEquals(5, subgraph.order());
        assertEquals( (5 * 4) / 2, subgraph.size());

        // Remove an existing edge
        assertTrue(subgraph.remove(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertEquals( (5 * 4) / 2 - 1, subgraph.size());
        assertEquals(5, subgraph.order());
        assertEquals(10, g.order());
        assertEquals( (9*10)/2 - 1, g.size());

        // Remove a non-existent edge, which should have no effect even though
        // the edge is present in the backing graph
        assertFalse(subgraph.remove(new SimpleDirectedTypedEdge<String>("type-1",0, 6)));
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            g.add(i);
        }
        g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1));
        g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 2));
        g.add(new SimpleDirectedTypedEdge<String>("type-1",1, 2));

        assertEquals(3, g.size());

        Set<Integer> verts = new HashSet<Integer>();
        for (int i = 0; i < 3; ++i)
            verts.add(i);

        DirectedMultigraph<String> sub = g.subgraph(verts);
        assertEquals(3, sub.order());
        assertEquals(3, sub.size());
     
        Set<DirectedTypedEdge<String>> edges = sub.edges();
        assertEquals(3, edges.size());
        int j = 0; 
        Iterator<DirectedTypedEdge<String>> iter = edges.iterator();
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<DirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);

        for (int i = 1; i < 5; ++i)
            assertTrue(adjList.contains(new SimpleDirectedTypedEdge<String>("type-1",0, i)));

        for (int i = 5; i < 10; ++i)
            assertFalse(adjList.contains(new SimpleDirectedTypedEdge<String>("type-1",0, i)));
    }

    @Test public void testSubgraphAdjacencyListSize() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<DirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<DirectedTypedEdge<String>> adjList = subgraph.getAdjacencyList(0);
        // Add an edge to a new vertex
        assertTrue(adjList.add(new SimpleDirectedTypedEdge<String>("type-1",0, 5)));
    }

    /******************************************************************
     *
     *
     * SubgraphAdjacentVerticesView tests 
     *
     *
     ******************************************************************/

    @Test public void testSubgraphAdjacentVertices() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.add(0);
    }

    @Test(expected=UnsupportedOperationException.class) public void testSubgraphAdjacentVerticesRemove() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
        Set<Integer> adjacent = subgraph.getNeighbors(0);
        adjacent.remove(0);
    }

    @Test public void testSubgraphAdjacentVerticesIterator() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);

        g.remove(0);
        assertEquals(4, subgraph.order());
        assertEquals((3 * 4) / 2, subgraph.size());

        g.remove(1);
        assertFalse(subgraph.contains(1));

        g.remove(2);
        assertFalse(subgraph.contains(new SimpleDirectedTypedEdge<String>("type-1",2,3)));
    }

    @Test public void testSubgraphVertexIteratorWhenVerticesRemovedFromBacking() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
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
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleDirectedTypedEdge<String>("type-1",i, j));
        }    

        // (n * (n-1)) / 2
        assertEquals( (10 * 9) / 2, g.size());
        assertEquals(10, g.order());

        Set<Integer> vertices = new LinkedHashSet<Integer>();
        for (int i = 0; i < 5; ++i)
            vertices.add(i);

        DirectedMultigraph<String> subgraph = g.subgraph(vertices);
        Set<DirectedTypedEdge<String>> edges = subgraph.edges();
        g.remove(0);
        assertEquals((4 * 3) / 2, edges.size());
        assertFalse(edges.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
    }

    /****************
     *
     *
     * Tests on graphs with multiple edge types
     *
     *
     ****************/

    @Test public void testAddTypedEdges() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertEquals(2, g.order());
        assertEquals(1, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertEquals(1, g.edgeTypes().size());

        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertEquals(2, g.order());
        assertEquals(2, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertEquals(2, g.edgeTypes().size());

        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertEquals(4, g.order());
        assertEquals(3, g.size());
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-3",3, 4)));
        assertEquals(3, g.edgeTypes().size());
    }

    @Test public void testNeighborsOfDifferentTypes() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 2)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 3)));

        Set<Integer> neighbors = g.getNeighbors(0);
        assertEquals(3, g.size());
        assertEquals(3, neighbors.size());
        assertTrue(neighbors.contains(1));
        assertTrue(neighbors.contains(2));
        assertTrue(neighbors.contains(3));

        // Test bi-directional case
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2", 2, 0)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 0)));

        neighbors = g.getNeighbors(0);
        assertEquals(3, neighbors.size());
        assertEquals(6, g.size());

        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 4, 0)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2", 5, 0)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 6, 0)));

        neighbors = g.getNeighbors(0);
        assertEquals(6, neighbors.size());
        assertEquals(9, g.size());
        for (int i = 1; i <= 6; ++i)
            assertTrue(neighbors.contains(i));
    }

    @Test public void testSubgraphOfSubtype() {
        // set up the network
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 3)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 2)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));

        
        Set<Integer> verts = new HashSet<Integer>();
        verts.add(0);
        verts.add(1);
        Set<String> types = Collections.singleton("type-1");
        DirectedMultigraph<String> sub = g.subgraph(g.vertices(), types);
        assertEquals(1, sub.edgeTypes().size());
        assertEquals(3, sub.size());
        assertEquals(g.order(), sub.order());       
        assertFalse(sub.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));        
    }

    /****************
     *
     *
     * Tests for copy()
     *
     *
     ****************/

    @Test public void testCopy() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-3",3, 4)));

        DirectedMultigraph<String> copy = g.copy(g.vertices());
        assertEquals(g.order(), copy.order());
        assertEquals(g.size(), copy.size());
        assertEquals(g, copy);

        copy.remove(4);
        assertEquals(4, g.order());
        assertEquals(3, copy.order());
    }

    @Test public void testCopy1vertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-3",3, 4)));

        DirectedMultigraph<String> copy = g.copy(Collections.singleton(1));
        assertEquals(1, copy.order());
        assertEquals(0, copy.size());
    }

    @Test public void testCopy2vertex() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-3",3, 4)));

        Set<Integer> verts = new HashSet<Integer>();
        Collections.addAll(verts, 0, 1);
        DirectedMultigraph<String> copy = g.copy(verts);
        assertEquals(2, copy.order());
        assertEquals(2, copy.size());
    }

    @Test public void testEmptyCopy() {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-1",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-2",0, 1)));
        assertTrue(g.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 4)));
        assertTrue(g.contains(new SimpleDirectedTypedEdge<String>("type-3",3, 4)));

        DirectedMultigraph<String> copy = g.copy(Collections.<Integer>emptySet());
        assertEquals(0, copy.order());
        assertEquals(0, copy.size());
        assertEquals(new DirectedMultigraph<String>(), copy);
        
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