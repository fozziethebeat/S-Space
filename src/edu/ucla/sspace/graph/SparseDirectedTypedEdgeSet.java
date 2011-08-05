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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.OpenIntSet;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.HashMultiMap;

import gnu.trove.TDecorators;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.iterator.TIntIterator;

/**
 * An {@link EdgeSet} implementation that stores {@link TypedEdge} instances for
 * a vertex.  This class provides additional methods beyond the {@code EdgeSet}
 * interface for interacting with edges on the basis of their type.
 */
public class SparseDirectedTypedEdgeSet<T> extends AbstractSet<DirectedTypedEdge<T>> 
        implements EdgeSet<DirectedTypedEdge<T>>, java.io.Serializable {

    private static final long serialVersionUID = 1L;
        
    /**
     * The vertex to which all edges in the set are connected
     */
    private final int rootVertex;

    private final T type;
    
    /**
     * A mapping from a type to the set of incoming edges
     */
    private final TIntHashSet inEdges;

    /**
     * A mapping from a type to the set of outgoing edges
     */
    private final TIntHashSet outEdges;
        
    /**
     * Creates a new {@code SparseDirectedTypedEdgeSet} for the specfied vertex.
     */
    public SparseDirectedTypedEdgeSet(int rootVertex, T type) {
        this.rootVertex = rootVertex;
        this.type = type;
        inEdges = new TIntHashSet();
        outEdges = new TIntHashSet();
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(DirectedTypedEdge<T> e) {
        if (!e.edgeType().equals(type))
            return false;
        else if (e.from() == rootVertex) 
            return outEdges.add(e.to());
        else if (e.to() == rootVertex) 
            return inEdges.add(e.from());
        return false;
    }

    /**
     * {@inheritDoc}  The set of vertices returned by this set is immutable.
     */
    public Set<Integer> connected() {
        TIntHashSet connected = 
            new TIntHashSet(Math.max(inEdges.size(), outEdges.size()));
        connected.addAll(inEdges);
        connected.addAll(outEdges);
        return TDecorators.wrap(connected);

//         Set<Integer> connected = new HashSet<Integer>();
//         TIntIterator in = inEdges.iterator();
//         while (in.hasNext())
//             connected.add(in.next());
//         TIntIterator out = outEdges.iterator();
//         while (out.hasNext())
//             connected.add(out.next());
//         return connected;

//         Collection<Set<Integer>> sets = new ArrayList<Set<Integer>>(2);
//         sets.add(inEdges);
//         sets.add(outEdges);
//         return new CombinedSet<Integer>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return inEdges.contains(vertex) || outEdges.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;
        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;

        if (!e.edgeType().equals(type))
            return false;
        else if (e.from() == rootVertex) 
            return outEdges.contains(e.to());
        else if (e.to() == rootVertex)
            return inEdges.contains(e.from());
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex) {
        return new EdgesForVertex(vertex);
    }    

    /**
     * {@inheritDoc}
     */
    public int getRoot() {
        return rootVertex;
    }

    /**
     * Returns the set of edges that point to the vertex associated with this
     * edge set.
     */
//     public Set<DirectedTypedEdge<T>> inEdges() {
//         return new InEdges();
//     }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return inEdges.isEmpty() && outEdges.isEmpty();
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<DirectedTypedEdge<T>> iterator() {
        return new DirectedTypedEdgeIterator();
    }

    /**
     * Returns the set of edges that originate from the vertex associated with
     * this edge set.
     */
//     public Set<DirectedTypedEdge<T>> outEdges() {
//         return new OutEdges();
//     }

    public Set<Integer> predecessors() {
        return TDecorators.wrap(inEdges);
    }

    TIntSet predecessorsPrimitive() {
        return inEdges;
    }

    public Set<Integer> successors() {
        return TDecorators.wrap(outEdges);
    }

    TIntSet successorsPrimitive() {
        return outEdges;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;

        if (!e.edgeType().equals(type))
            return false;
        else if (e.from() == rootVertex) 
            return outEdges.remove(e.to());
        else if (e.to() == rootVertex) 
            return inEdges.remove(e.from());
        return false;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return inEdges.size() + outEdges.size();
    }

    /**
     * Returns the set of types contained within this set
     */
    public Set<T> types() {
        return Collections.<T>singleton(type);
    }

    /**
     * A wrapper around the set of edges that connect another vertex to the root
     * vertex
     */
    private class EdgesForVertex extends AbstractSet<DirectedTypedEdge<T>> {
        
        /**
         * The vertex in the edges that is not this root vertex
         */
        private final int otherVertex;

        public EdgesForVertex(int otherVertex) {
            this.otherVertex = otherVertex;
        }

        @Override public boolean add(DirectedTypedEdge<T> e) {
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.add(e);
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.contains(e);
        }

        @Override public boolean isEmpty() {
            return !SparseDirectedTypedEdgeSet.this.connects(otherVertex);
        }

        @Override public Iterator<DirectedTypedEdge<T>> iterator() {
            return new EdgeIterator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedTypedEdge))
                return false;
            DirectedTypedEdge<?> e = (DirectedTypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseDirectedTypedEdgeSet.this.remove(e);
        }

        @Override public int size() {
            int sz = 0;
            if (inEdges.contains(otherVertex))
                sz++;
            if (outEdges.contains(otherVertex))
                sz++;
            return sz;
        }

        /**
         * An iterator over all the edges that connect the root vertex to a
         * single vertex
         */
        class EdgeIterator implements Iterator<DirectedTypedEdge<T>> {
            
            /**
             * The next edge to return
             */ 
            DirectedTypedEdge<T> next;

            /**
             * The edge that was most recently returned or {@code null} if the
             * edge was removed or has yet to be returned.
             */
            DirectedTypedEdge<T> cur;

            boolean returnedIn;
            boolean returnedOut;

            public EdgeIterator() {
                advance();
            }

            private void advance() {
                next = null;
                if (inEdges.contains(otherVertex) && !returnedIn) {
                    next = new SimpleDirectedTypedEdge<T>(type, otherVertex, rootVertex);
                    returnedIn = true;
                }
                else if (next == null && !returnedOut 
                         && outEdges.contains(otherVertex)) {
                    next = new SimpleDirectedTypedEdge<T>(type, rootVertex, otherVertex);
                    returnedOut = true;
                } 
            }

            public boolean hasNext() {
                return next != null;
            }

            public DirectedTypedEdge<T> next() {
                cur = next;
                advance();
                return cur;
            }

            public void remove() {
                if (cur == null)
                    throw new IllegalStateException();
                SparseDirectedTypedEdgeSet.this.remove(cur);
                cur = null;
            }
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class DirectedTypedEdgeIterator implements Iterator<DirectedTypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private TIntIterator in;

        /**
         * An iterator over the outgoing edges for the current type
         */
        private TIntIterator out;

        /**
         * The iterator on which remove() should be called
         */
        private TIntIterator toRemoveFrom;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;
        
        public DirectedTypedEdgeIterator() {
            in = inEdges.iterator();
            out = outEdges.iterator();
            advance();
        }

        private void advance() {
            next = null;           
            
            while (in.hasNext() && next == null) {
                int v = in.next();
                next = new SimpleDirectedTypedEdge<T>(
                    type, v, rootVertex);
            }
            while (next == null && out.hasNext()) {
                int v = out.next();
                next = new SimpleDirectedTypedEdge<T>(
                    type, rootVertex, v);
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public DirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
//             if (toRemoveFrom == null)
//                 throw new IllegalStateException("No element to remove");
//             toRemoveFrom.remove();
        }
    }
}