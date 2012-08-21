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

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


/**
 * A {@link EdgeSet} implementation for holding {@link Edge} instances.
 */
public class SparseUndirectedEdgeSet extends AbstractSet<Edge> 
        implements EdgeSet<Edge>, java.io.Serializable {

    private static final long serialVersionUID = 1L;
        
    /**
     * The vertex that is connected to all the edges in this set
     */
    private final int rootVertex;

    /**
     * The set of vertices to which the root vertex is connected
     */
    private final TIntSet edges;
    
    public SparseUndirectedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = new TIntHashSet();
    }

    public SparseUndirectedEdgeSet(int rootVertex, int capacity) {
        this.rootVertex = rootVertex;
        edges = new TIntHashSet(capacity);
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
        boolean b = edges.add(toAdd);
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public IntSet connected() {
        return TroveIntSet.wrap(edges);
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
            int toFind = 0;
            if (e.to() == rootVertex) 
                toFind = e.from();
            else if (e.from() == rootVertex)
                toFind = e.to();
            else return false;

            boolean b = edges.contains(toFind);
            return b;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public SparseUndirectedEdgeSet copy(IntSet vertices) {        
        SparseUndirectedEdgeSet copy = new SparseUndirectedEdgeSet(rootVertex);
        if (edges.size() < vertices.size()) {
            TIntIterator iter = edges.iterator();
            while (iter.hasNext()) {
                int v = iter.next();
                if (vertices.contains(v))
                    copy.edges.add(v);
            }            
        }
        else {
            IntIterator iter = vertices.iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                if (edges.contains(v)) 
                    copy.edges.add(v);
            }
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int disconnect(int vertex) {
        return (edges.remove(vertex)) ? 1 : 0;
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
            int toRemove = 0;
            if (e.to() == rootVertex) 
                toRemove = e.from();
            else if (e.from() == rootVertex)
                toRemove = e.to();
            else return false;
            boolean b = edges.remove(toRemove);
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

        private TIntIterator otherVertices;

        private boolean alreadyRemoved;
        
        public EdgeIterator() {
            otherVertices = edges.iterator();
            alreadyRemoved = false;
        }

        public boolean hasNext() {
            return otherVertices.hasNext();
        }

        public Edge next() {
            int i = otherVertices.next();
            alreadyRemoved = false;
            return new SimpleEdge(rootVertex, i);
        }

        public void remove() {
            if (alreadyRemoved)
                throw new IllegalStateException();
            // For some reason this doesn't through the expected Iterator API
            // exception, so we try/rethrow as necessary.
            try {
                otherVertices.remove();
                alreadyRemoved = true;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                throw new IllegalStateException();
            }
        }
    }
}