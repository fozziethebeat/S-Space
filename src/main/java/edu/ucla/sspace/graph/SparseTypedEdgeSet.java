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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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

import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.util.primitive.CompactIntSet;
import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import gnu.trove.TDecorators;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.procedure.TIntObjectProcedure;


/**
 * An {@link EdgeSet} implementation that stores {@link TypedEdge} instances for
 * a vertex.  This class provides additional methods beyond the {@code EdgeSet}
 * interface for interacting with edges on the basis of their type.
 */
public class SparseTypedEdgeSet<T> extends AbstractSet<TypedEdge<T>> 
        implements EdgeSet<TypedEdge<T>>, java.io.Serializable {
        
    private static final long serialVersionUID = 1L;
       
    /**
     * The vertex to which all edges in the set are connected
     */
    private final int rootVertex;
    
    /**
     * A mapping from a type to the set of outgoing edges
     */
    private final TIntObjectMap<Set<T>> edges;
        
    /**
     * The number of edges in this set.
     */
    private int size;

    /**
     * Creates a new {@code SparseTypedEdgeSet} for the specfied vertex.
     */
    public SparseTypedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = new TIntObjectHashMap<Set<T>>();
        size = 0;
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(TypedEdge<T> e) {
        if (e.from() == rootVertex) 
            return add(e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return add(e.from(), e.edgeType());
        return false;
    }

    /**
     * Adds an edge to the spectied set that connectes t{@code i} according to
     * the given type, or returns {@code false} if the edge already existed.
     */
    private boolean add(int i, T type) {
        Set<T> types = edges.get(i);
        // If there weren't any edges to this vertex, then special case the
        // creation and return true.
        if (types == null) {
            types = new HashSet<T>();
            edges.put(i, types);
        }
        
        boolean b = types.add(type);
        if (b)
            size++;
        return b;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        edges.clear();
    }

    /**
     * {@inheritDoc}  The set of vertices returned by this set is immutable.
     */
    public IntSet connected() {
        return TroveIntSet.wrap(edges.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return edges.containsKey(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex, T type) {
        Set<T> types = edges.get(vertex);
        return types != null && types.contains(type);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof TypedEdge))
            return false;
        @SuppressWarnings("unchecked")
        TypedEdge<T> e = (TypedEdge<T>)o;

        if (e.from() == rootVertex) 
            return contains(edges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return contains(edges, e.from(), e.edgeType());
        return false;
    }

    private boolean contains(TIntObjectMap<Set<T>> edges, int i, T type) {
        Set<T> types = edges.get(i);
        return types != null && types.contains(type);
    }

    /**
     * {@inheritDoc}
     */
     public SparseTypedEdgeSet<T> copy(IntSet vertices) {        
         SparseTypedEdgeSet<T> copy = new SparseTypedEdgeSet<T>(rootVertex);
         TIntObjectIterator<Set<T>> iter = edges.iterator();
         while (iter.hasNext()) {
             iter.advance();
             int v = iter.key();
             if (vertices.contains(v)) {
                 Set<T> types = iter.value();
                 copy.edges.put(v, types);
                 copy.size += types.size();
             }
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int disconnect(int v) {
        Set<T> types = edges.remove(v);
        if (types != null) {
            int edges = types.size();
            size -= edges;
            return edges;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(final T type) {    
        final Set<TypedEdge<T>> s = new HashSet<TypedEdge<T>>();
        edges.forEachEntry(new TIntObjectProcedure<Set<T>>() {
                public boolean execute(int v, Set<T> types) {
                    if (types.contains(type))
                        s.add(new SimpleTypedEdge<T>(
                                      type, v, rootVertex));
                    return true;
                }
            });
        return s;
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex) {
        return new EdgesForVertex(vertex);
        /*
        Set<T> types = edges.get(vertex);
        // For unconnected vertices, return the empty set
        if (types == null)
            return Collections.<TypedEdge<T>>emptySet();
        // If there's only a single type, just use the singleton set to avoid
        // allocating a new HashSet for just one object
        else if (types.size() == 1) {
            return Collections.<TypedEdge<T>>singleton(
                new SimpleTypedEdge(types.iterator().next(), vertex, rootVertex));
        }
        else {
            Set<TypedEdge<T>> s = new HashSet<TypedEdge<T>>();
            for (T type : types) 
                s.add(new SimpleTypedEdge<T>(type, vertex, rootVertex));
            return s;
        }
        */
    }    

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex, Set<T> types) {
        // NOTE: this is purely unoptimized code, so fix if it ever gets in a
        // hotspot
        Set<TypedEdge<T>> set = new HashSet<TypedEdge<T>>();
        for (TypedEdge<T> e : new EdgesForVertex(vertex))
            if (types.contains(e.edgeType()))
                set.add(e);
        return set;
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
    public boolean isEmpty() {
        return edges.isEmpty();
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<TypedEdge<T>> iterator() {
        return new EdgeIterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof TypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        TypedEdge<T> e = (TypedEdge<T>)o;

        if (e.from() == rootVertex) 
            return remove(e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return remove(e.from(), e.edgeType());
        return false;
    }

    private boolean remove(int i, T type) {
        Set<T> types = edges.get(i);
        boolean b = types.remove(type);
        if (b)
            size--;
        return b;
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return size;
    }

    /**
     * Returns the set of types contained within this set
     */
    public Set<T> types() {
        // NOTE: purely unoptimized!
        Set<T> types = new HashSet<T>();
        for (Object o : edges.values()) {
            Set<T> s = (Set<T>)o;
            types.addAll(s);
        }
        return types;
    }

    /**
     *
     */
    public Iterator<TypedEdge<T>> uniqueIterator() {
        return new UniqueEdgeIterator();
    }

    /**
     * A wrapper around the set of edges that connect another vertex to the root
     * vertex
     */
    private class EdgesForVertex extends AbstractSet<TypedEdge<T>> {
        
        /**
         * The vertex in the edges that is not this root vertex
         */
        private final int otherVertex;

        public EdgesForVertex(int otherVertex) {
            this.otherVertex = otherVertex;
        }

        @Override public boolean add(TypedEdge<T> e) {
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseTypedEdgeSet.this.add(e);
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof TypedEdge))
                return false;
            TypedEdge<?> e = (TypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseTypedEdgeSet.this.contains(e);
        }

        @Override public boolean isEmpty() {
            return !SparseTypedEdgeSet.this.connects(otherVertex);
        }

        @Override public Iterator<TypedEdge<T>> iterator() {
            return new EdgesForVertexIterator(otherVertex);
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof TypedEdge))
                return false;
            TypedEdge<?> e = (TypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && SparseTypedEdgeSet.this.remove(e);
        }

        @Override public int size() {
            Set<T> types = edges.get(otherVertex);
            return (types == null) ? 0 : types.size();
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * TypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class EdgesForVertexIterator implements Iterator<TypedEdge<T>> {

        private final Iterator<T> typeIter;

        int otherVertex;

        public EdgesForVertexIterator(int otherVertex) {
            this.otherVertex = otherVertex;
            Set<T> types = edges.get(otherVertex);
            
            typeIter = (types != null) 
                ? types.iterator()
                : Collections.<T>emptySet().iterator();
        }

        public boolean hasNext() {
            return typeIter.hasNext();
        }

        public TypedEdge<T> next() {
            if (!typeIter.hasNext())
                throw new NoSuchElementException();
            return new SimpleTypedEdge(
                typeIter.next(), rootVertex, otherVertex);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * An iterator over the edges in this set that constructs {@link
     * TypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class EdgeIterator implements Iterator<TypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private TIntObjectIterator<Set<T>> iter;

        private Iterator<T> typeIter;

        TypedEdge<T> next;

        public EdgeIterator() {
            this.iter = edges.iterator();
            typeIter = null;
            advance();
        }

        private void advance() {
            next = null;            
            while (next == null) {
                if (typeIter == null || !typeIter.hasNext()) {
                    // If there were no more vertices to load, stop searching
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    typeIter = iter.value().iterator();
                }

                if (typeIter.hasNext()) {
                    next = new SimpleTypedEdge<T>(
                        typeIter.next(), iter.key(), rootVertex);
                }
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public TypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            TypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * TypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class UniqueEdgeIterator implements Iterator<TypedEdge<T>> {

        Iterator<TypedEdge<T>> it;

        TypedEdge<T> next;

        public UniqueEdgeIterator() {
            it = iterator();
            advance();
        }

        private void advance() {
            next = null;
            while (it.hasNext() && next == null) {
                TypedEdge<T> e = it.next();
                if ((e.from() == rootVertex && e.to() < rootVertex)
                        || (e.to() == rootVertex && e.from() < rootVertex)) 
                    next = e;
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public TypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            TypedEdge<T> n = next;
//             System.out.println("next: " + n);
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
