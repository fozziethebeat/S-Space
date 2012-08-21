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

public class SparseDirectedTypedEdgeSetTests {

    @Test public void testConstructor() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        assertEquals(0, edges.size());
        assertTrue(edges.isEmpty());
    }

    @Test public void testAdd() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertEquals(1, edges.size());
        // Duplicate add
        assertFalse(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertEquals(1, edges.size());
        // Add without matching root vertex
        assertFalse(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 4, 5)));
        assertEquals(1, edges.size());
        // Add of second type
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertEquals(2, edges.size());
        // Add of third type
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertEquals(3, edges.size());
        // Add to different vertex
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertEquals(4, edges.size());
        // Add to reverse orientation
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertEquals(5, edges.size());
        // Add to duplicate reverse orientation
        assertFalse(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertEquals(5, edges.size());
    }

    @Test public void testContains() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertEquals(4, edges.size());

        assertTrue(edges.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertFalse(edges.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 2)));
        assertFalse(edges.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 2)));
    }

    @Test public void testGetEdges() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertEquals(4, edges.size());

        Set<DirectedTypedEdge<String>> e = edges.getEdges(1);
        assertEquals(3, e.size());
        e = edges.getEdges(2);
        assertEquals(1, e.size());
    }

    @Test public void testConnected() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
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

    @Test public void testSuccessors() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 3, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 4, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 4, 0)));

        assertEquals(8, edges.size());

        Set<Integer> successors = edges.successors();
        assertEquals(2, successors.size());
        assertTrue(successors.contains(1));
        assertTrue(successors.contains(2));

        Iterator<Integer> it = successors.iterator();
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

    @Test public void testPredecessors() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 3, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 3, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 4, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 4, 0)));

        assertEquals(8, edges.size());

        Set<Integer> predecessors = edges.predecessors();
        assertEquals(2, predecessors.size());
        assertTrue(predecessors.contains(3));
        assertTrue(predecessors.contains(4));

        Iterator<Integer> it = predecessors.iterator();
        int i = 0;
        Set<Integer> test = new HashSet<Integer>();
        while (it.hasNext()) {
            test.add(it.next());
            i++;
        }
        assertEquals(2, i);
        assertEquals(2, test.size());
        assertTrue(test.contains(3));
        assertTrue(test.contains(4));
    }

    @Test public void testIterator() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertEquals(4, edges.size());

        Iterator<DirectedTypedEdge<String>> iter = edges.iterator();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        int i = 0;
        while (iter.hasNext()) {
            control.add(iter.next());
            i++;
        }
        assertEquals(4, i);
        assertEquals(4, control.size());
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
    }

    @Test public void testIteratorInAndOutEdges() {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));

        assertEquals(8, edges.size());

        Iterator<DirectedTypedEdge<String>> iter = edges.iterator();
        Set<DirectedTypedEdge<String>> control = new HashSet<DirectedTypedEdge<String>>();
        int i = 0;
        while (iter.hasNext()) {
            control.add(iter.next());
            i++;
        }
        assertEquals(8, i);
        assertEquals(8, control.size());
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(control.contains(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));
    }
    
    @Test public void testSerialization() throws Exception {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(edges);

        // Clear the cache using "dark gray" magic
        Field cacheField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPE_INDICES");
        cacheField.setAccessible(true);
        Map<?,?> cache = (Map<?,?>)cacheField.get(null);

        Field typesField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPES");
        typesField.setAccessible(true);
        List<?> types = (List<?>)typesField.get(null);
        
        System.out.println("cache before: " + cache);
        cache.clear();
        types.clear();
        System.out.println("cache after clear: " + cache);

        // Should cause an error if uncommented because TYPES is empty
        //         for (Edge e : edges)
        //             System.out.println(e);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        SparseDirectedTypedEdgeSet<String> test =
            (SparseDirectedTypedEdgeSet<String>)(ois.readObject());
        System.out.println("cache after deserialization: " + cache);        

        assertEquals(8, test.size());
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));
        
        // Sanity check that we can iterate over the edges
        for (Edge e : test)
            ;
    }

    @Test public void testSerializationWithConsistentTypeChanges() throws Exception {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(edges);

        // Clear the cache using "dark gray" magic
        Field cacheField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPE_INDICES");
        cacheField.setAccessible(true);
        Map<?,?> cache = (Map<?,?>)cacheField.get(null);

        Field typesField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPES");
        typesField.setAccessible(true);
        List<?> types = (List<?>)typesField.get(null);
        Object first = types.get(0);
        
        System.out.println("cache before: " + cache);
        cache.clear();
        types.clear();
        System.out.println("cache after clear: " + cache);

        // Add a type to the TYPE_INDICES set which is consistent with the
        // serialized mapping, but doesn't let the deserialization use the
        // equality case
        new SparseDirectedTypedEdgeSet<Object>(0).
            add(new SimpleDirectedTypedEdge<Object>(first, 2, 0));        

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        SparseDirectedTypedEdgeSet<String> test =
            (SparseDirectedTypedEdgeSet<String>)(ois.readObject());
        System.out.println("cache after deserialization: " + cache);        

        assertEquals(8, test.size());
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));
        
        // Sanity check that we can iterate over the edges
        for (Edge e : test)
            ;
    }

    @Test public void testSerializationWithInconsistentTypeChanges() throws Exception {
        SparseDirectedTypedEdgeSet<String> edges = 
            new SparseDirectedTypedEdgeSet<String>(0);
        // Single add
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(edges.add(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(edges);

        // Clear the cache using "dark gray" magic
        Field cacheField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPE_INDICES");
        cacheField.setAccessible(true);
        Map<?,?> cache = (Map<?,?>)cacheField.get(null);

        Field typesField = SparseDirectedTypedEdgeSet.class.getDeclaredField("TYPES");
        typesField.setAccessible(true);
        List<?> types = (List<?>)typesField.get(null);
        Object second = types.get(1);
        
        System.out.println("cache before: " + cache);
        cache.clear();
        types.clear();
        System.out.println("cache after clear: " + cache);

        // Add a type to the TYPE_INDICES set which is *INCONSISTENT* with the
        // serialized set's type mapping.  This forces the deserialization to
        // remap the types for all of its vertices.
        new SparseDirectedTypedEdgeSet<Object>(0).
            add(new SimpleDirectedTypedEdge<Object>(second, 2, 0));        

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        SparseDirectedTypedEdgeSet<String> test =
            (SparseDirectedTypedEdgeSet<String>)(ois.readObject());
        System.out.println("cache after deserialization: " + cache);        

        assertEquals(8, test.size());
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 1)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 0, 2)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-1", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-2", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 1, 0)));
        assertTrue(test.contains(new SimpleDirectedTypedEdge<String>("type-3", 2, 0)));
        
        // Sanity check that we can iterate over the edges
        for (Edge e : test)
            ;
    }
}