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
import java.util.Iterator;
import java.util.Set;

import edu.ucla.sspace.util.OpenIntSet;


/**
 *
 */
public class SparseSymmetricEdgeSet {
        
    private final int rootVertex;
    
    private final OpenIntSet edges;
    
    public SparseSymmetricEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = new OpenIntSet();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(Edge e) {
        int toAdd = -1;
        if (e.from() == rootVertex) {
            toAdd = e.to();
        }
        else {
            if (e.to() != rootVertex)
                return false;
            // else e.to() == rootVertex
            toAdd = e.from();
        }

        // Only add the vertex if the index for it is greated than this vertex.
        // In a set of EdgeSets, this ensure that for two indices i,j only one
        // edge is ever present
        if (rootVertex < toAdd)
            return edges.add(toAdd);
        return false;
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
            if (e.to() == rootVertex) 
                return e.from() > rootVertex && edges.contains(e.from());
            else
                return e.from() == rootVertex && edges.contains(e.to());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean disconnect(int vertex) {
        return edges.remove(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Edge> getEdges(int vertex) {
        return (edges.contains(vertex))
            ? Collections.<Edge>singleton(new SimpleEdge(rootVertex, vertex))
            : null;
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
            if (e.to() == rootVertex) 
                return e.from() > rootVertex && edges.remove(e.from());
            else
                return e.from() == rootVertex && edges.remove(e.to());
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