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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 *
 */
public class SparseUndirectedEdgeSet extends AbstractSet<Edge> 
        implements EdgeSet<Edge> {
        
    private final int rootVertex;
    
    private final// Set<Integer> edges; 
     OpenIntSet edges;
    
    public SparseUndirectedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = //new HashSet<Integer>();
            new OpenIntSet();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(Edge e) {
        int toAdd = -1;
        if (e.from() == rootVertex) 
            toAdd = e.to();        
        else if (e.to() == rootVertex)
            toAdd = e.from();
        else
            return false;
        
//         System.out.printf("Indices to %d before adding %d: %s%n", rootVertex, toAdd, java.util.Arrays.toString(edges.buckets));
        boolean b = edges.add(toAdd);
//         System.out.printf("Indices to %d after adding %d: %s%n", rootVertex, toAdd, java.util.Arrays.toString(edges.buckets));
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> connected() {
        // REMINDER: wrap to prevent adding self edges?
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return edges.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
//             return ((e.to() == rootVertex) && edges.contains(e.from()))
//                 || (e.from() == rootVertex && edges.contains(e.to()));
            int toFind = 0;
            if (e.to() == rootVertex) 
                toFind = e.from();
            else if (e.from() == rootVertex)
                toFind = e.to();
            else return false;

            boolean b = edges.contains(toFind);
            // System.out.printf("Was %d in %d's indices?: %s, %s%n",  toFind, rootVertex, b, java.util.Arrays.toString(edges.buckets));
            return b;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Edge> getEdges(int vertex) {
        return (edges.contains(vertex))
            ? Collections.<Edge>singleton(new SimpleEdge(rootVertex, vertex))
            : Collections.<Edge>emptySet();
    }    

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<Edge> iterator() {
        return new EdgeIterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
//             return (e.to() == rootVertex && edges.remove(e.from()))
//                 || (e.from() == rootVertex && edges.remove(e.to()));
            int toRemove = 0;
            if (e.to() == rootVertex) 
                toRemove = e.from();
            else if (e.from() == rootVertex)
                toRemove = e.to();
            else return false;

//             System.out.printf("Indices to %d before removing %d: %s%n", rootVertex, toRemove, java.util.Arrays.toString(edges.buckets));
            boolean b = edges.remove(toRemove);
//             System.out.printf("Indices to %d after removing %d: %s%n", rootVertex, toRemove, java.util.Arrays.toString(edges.buckets));
//             System.out.printf("Was able to remove %d? %s%n", toRemove, b);
            return b;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return edges.size();
    }

    /**
     * An iterator over the edges in this set that constructs {@link Edge}
     * instances as it traverses through the set of connected vertices.
     */
    private class EdgeIterator implements Iterator<Edge> {

        private Iterator<Integer> otherVertices;
        
        public EdgeIterator() {
            otherVertices = edges.iterator();
        }

        public boolean hasNext() {
            return otherVertices.hasNext();
        }

        public Edge next() {
            Integer i = otherVertices.next();
            return new SimpleEdge(rootVertex, i);
        }

        public void remove() {
            otherVertices.remove();
        }
    }
}