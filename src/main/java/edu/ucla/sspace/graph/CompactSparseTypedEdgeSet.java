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
import java.util.BitSet;
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
public class CompactSparseTypedEdgeSet<T> extends AbstractSet<TypedEdge<T>> 
        implements EdgeSet<TypedEdge<T>>, java.io.Serializable {
        
    private static final long serialVersionUID = 1L;

    /////
    //
    // IMPLEMENTATION NOTE: This class stores a set of types associated each
    // each in coming and outgoing edge's vertex.  Rather than storing the set
    // of types as a Set<T>, the set is represented in a compact form using a
    // BitSet, where each bit corresponds to a type index.  Given the potential
    // for a huge number of edge sets in any give graph, having each set
    // maintain its own type-to-bit-index mapping wastes a significant amount of
    // space -- especially if the sets are all using the same types.  Therefore,
    // we use a class-level cache of mapping the types to indices with two
    // global static variables.  This results in a significant space savings.
    // However, because these are static variables, their mapping state needs to
    // be preserved upon serialization, which leads to a (rather complex) custom
    // serialization code.
    //
    ////

    /**
     * A mapping from indices to their corresponding types 
     */
    private static final List<Object> TYPES = new ArrayList<Object>();

    /**
     * The mapping from types to their indices
     */
    private static final Map<Object,Integer> TYPE_INDICES = 
         new HashMap<Object,Integer>();

    /**
     * Returns the index for the given type, creating a new index if necessary
     */
    private static int index(Object o) {
        Integer i = TYPE_INDICES.get(o);
        if (i == null) {
            synchronized (TYPE_INDICES) {
                // check that another thread did not already update the index
                i = TYPE_INDICES.get(o);
                if (i != null)
                    return i;
                else {
                    int j = TYPE_INDICES.size();
                    TYPE_INDICES.put(o, j);
                    TYPES.add(o);
                    return j;
                }
            }
        }
        return i;
    }
       
    /**
     * The vertex to which all edges in the set are connected
     */
    private final int rootVertex;
    
    /**
     * A mapping from a type to the set of outgoing edges
     */
    private final TIntObjectHashMap<BitSet> edges;
        
    /**
     * The number of edges in this set.
     */
    private int size;

    /**
     * The types that are contained in this set;
     */
    private BitSet setTypes;

    /**
     * Creates a new {@code CompactSparseTypedEdgeSet} for the specfied vertex.
     */
    public CompactSparseTypedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        edges = new TIntObjectHashMap<BitSet>();
        setTypes = new BitSet();
        size = 0;
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(TypedEdge<T> e) {
        if (e.from() == rootVertex) 
            return add(edges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return add(edges, e.from(), e.edgeType());
        return false;
    }

    /**
     * Adds an edge to the spectied set that connectes t{@code i} according to
     * the given type, or returns {@code false} if the edge already existed.
     */
    private boolean add(TIntObjectHashMap<BitSet> edges, int i, T type) {
        BitSet types = edges.get(i);
        // If there weren't any edges to this vertex, then special case the
        // creation and return true.
        if (types == null) {
            types = new BitSet();
            edges.put(i, types);
            types.set(index(type));
            size++;
            return true;
        }
        // Otherwise, lookup the type's index and see if it already exists in
        // the bitset, indicating the edge does too
        int index = index(type);
        setTypes.set(index);
        if (!types.get(index)) {
            types.set(index);            
            size++;
            return true;            
        }
        // If the type was already there, then return false because the edge
        // already exists
        return false;
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
        BitSet types = edges.get(vertex);
        return types != null && types.get(index(type));
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

    private boolean contains(TIntObjectHashMap<BitSet> edges, int i, T type) {
        BitSet types = edges.get(i);
        if (types == null) 
            return false;
        int index = index(type);
        return types.get(index);
    }

    /**
     * {@inheritDoc}
     */
     public CompactSparseTypedEdgeSet<T> copy(IntSet vertices) {        
         CompactSparseTypedEdgeSet<T> copy = new CompactSparseTypedEdgeSet<T>(rootVertex);
         
         if (vertices.size() < edges.size()) {
            IntIterator iter = vertices.iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                if (edges.containsKey(v)) {
                    BitSet b = edges.get(v);
                    BitSet b2 = new BitSet();
                    b2.or(b);
                    copy.edges.put(v, b2);
                }
            }            
        }
        else {
            TIntObjectIterator<BitSet> iter = edges.iterator();
            while (iter.hasNext()) {
                iter.advance();
                int v = iter.key();
                if (vertices.contains(v)) {
                    BitSet b = iter.value();
                    BitSet b2 = new BitSet();
                    b2.or(b);
                    copy.edges.put(v, b2);
                }
            }
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int disconnect(int v) {
        BitSet b = edges.remove(v);
        if (b != null) {
            int edges = b.cardinality();
            size -= edges;
            return edges;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(final T type) {    
        if (!TYPE_INDICES.containsKey(type))
            return Collections.<TypedEdge<T>>emptySet();
        final int typeIndex = index(type);
        final Set<TypedEdge<T>> s = new HashSet<TypedEdge<T>>();
        edges.forEachEntry(new TIntObjectProcedure<BitSet>() {
                public boolean execute(int v, BitSet types) {
                    if (types.get(typeIndex))
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
        BitSet b = edges.get(vertex);
        if (b == null)
            return Collections.<TypedEdge<T>>emptySet();
        Set<TypedEdge<T>> s = new HashSet<TypedEdge<T>>();
        for (int i = b.nextSetBit(0); i >= 0; i = b.nextSetBit(i+1)) {
            @SuppressWarnings("unchecked")
            T type = (T)(TYPES.get(i));
            s.add(new SimpleTypedEdge<T>(type, vertex, rootVertex));
        }
        return s;
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
            return remove(edges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return remove(edges, e.from(), e.edgeType());
        return false;
    }

    private boolean remove(TIntObjectHashMap<BitSet> edges, int i, T type) {
        BitSet types = edges.get(i);
        if (types == null) 
            return false;
        int index = index(type);
        // If there was an edge of that type, remove it and update the
        // "connected" set as necessary
        if (types.get(index)) {
            types.set(index, false);
            // If this was the last edge to that vertex, remove this BitMap
            if (types.cardinality() == 0) {
                edges.remove(i);
                size--;
            }
            return true;
        }
        return false;
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
        return new Types();
    }

    /**
     *
     */
    public Iterator<TypedEdge<T>> uniqueIterator() {
        return new UniqueEdgeIterator();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // The TYPE_INDICES mapping is not longer valid upon deserialization so
        // we need to write it as a part of this object's state.  Serialization
        // uses some caching, so if multiple instances of this class are being
        // written, the cache is only saved once, which saves significant space.
        out.writeObject(TYPE_INDICES);
    }

    private void readObject(ObjectInputStream in) 
            throws IOException, ClassNotFoundException {
        // Restore the existing state of the Set
        in.defaultReadObject();

        // Then read in the type indices, which may or may not need to be
        // restored depending on the current state of the cache
        @SuppressWarnings("unchecked")
        Map<Object,Integer> typeIndices = 
            (Map<Object,Integer>)Map.class.cast(in.readObject());
        boolean needToRemapIndices = true;
        if (!TYPE_INDICES.equals(typeIndices)) {
            if (TYPE_INDICES.isEmpty()) {
                synchronized (TYPE_INDICES) {
                    // Check whether some thread might have modified the map in
                    // the mean-time.  If not, then use our type mapping as the
                    // default
                    if (TYPE_INDICES.isEmpty()) {
                        TYPE_INDICES.putAll(typeIndices);
                        // Fill in the VALUES array with nulls first so that we
                        // can iterate through the typeIndices map once without
                        // having to worry about the indexing
                        for (int i = 0; i < TYPE_INDICES.size(); ++i)
                            TYPES.add(null);
                        for (Map.Entry<Object,Integer> e : 
                                 TYPE_INDICES.entrySet()) {
                            TYPES.set(e.getValue(), e.getKey());
                        }                       
                        needToRemapIndices = false;
                    }
                   
                }
            }
        }
        // Check if the indices we have are a subset or superset of the current
        // type indices
        else {
            boolean foundMismatch = false;
            for (Map.Entry<Object,Integer> e : typeIndices.entrySet()) {
                Object o = e.getKey();
                int oldIndex = e.getValue();
                Integer curIndex = TYPE_INDICES.get(o);
                // If the current index is null, then map it to what this has,
                // which is possibly beyond the range of the current set of
                // types.  Note that our type mapping isn't invalidated yet by
                // this action, so we don't need to remap.
                if (curIndex == null) {
                    // Grow the TYPES list until there is room for this
                    // additional index                    
                    while (TYPES.size() <= oldIndex) 
                        TYPES.add(null);
                    TYPES.set(oldIndex, o);
                    TYPE_INDICES.put(o, oldIndex);
                }
                else if (curIndex != oldIndex) {
                    foundMismatch = true;
                }
            }
            // If we were successfully able to add the indices we have without
            // disturbing the existing mapping, or our indices were just a
            // subset of the existing ones, then we don't need to remap the
            // total set of indices.
            if (!foundMismatch)
                needToRemapIndices = false;
        }

        // If the state of this set's type is inconsistent with the current type
        // mapping, then update the mapping with any missing types and then
        // reset all of its BitSet contents with the correct indices
        if (needToRemapIndices) {
            TIntIntMap typeRemapping = new TIntIntHashMap();
            for (Map.Entry<Object,Integer> e : typeIndices.entrySet()) {
                Object o = e.getKey();
                int oldIndex = e.getValue();
                // NOTE: the else {} case above may have added several of our
                // types that weren't inconsistent, so this may be an identity
                // mapping for some types, which is nice.
                typeRemapping.put(oldIndex, index(o));
            }
            // Remap all the in-edges vertices' types...
            for (TIntObjectIterator<BitSet> it = edges.iterator(); it.hasNext(); ) {
                it.advance();
                int v = it.key();
                BitSet oldIndices = it.value();
                BitSet newIndices = new BitSet();
                for (int i = oldIndices.nextSetBit(0); i >= 0; 
                         i = oldIndices.nextSetBit(i+1)) {
                    newIndices.set(typeRemapping.get(i));
                }
                it.setValue(newIndices);
            }
        }
    }

    /**
     * A utility class for exposing the objects for types of the edges in this
     * set, which are otherwise represented as bits.
     */
    private class Types extends AbstractSet<T> {
        
        public boolean contains(Object o) {
            if (TYPE_INDICES.containsKey(o)) {
                Integer i = TYPE_INDICES.get(o);
                return setTypes.get(i);
            }
            return false;
        }

        public Iterator <T> iterator() {
            return new TypeIter();
        }

        public int size() {
            return setTypes.cardinality();
        }

        private class TypeIter implements Iterator<T> {

            IntIterator typeIndices;
            
            public TypeIter() {
                typeIndices = CompactIntSet.wrap(setTypes).iterator();
            }

            public boolean hasNext() {
                return typeIndices.hasNext();
            }

            public T next() {
                if (!typeIndices.hasNext())
                    throw new NoSuchElementException();
                int i = typeIndices.nextInt();
                @SuppressWarnings("unchecked")
                T type = (T)(TYPES.get(i));
                return type;
            }

            public void remove() { 
                throw new UnsupportedOperationException();
            }
        }        
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
                && CompactSparseTypedEdgeSet.this.add(e);
        }

        @Override public boolean contains(Object o) {
            if (!(o instanceof TypedEdge))
                return false;
            TypedEdge<?> e = (TypedEdge)o;
            return ((e.to() == rootVertex && e.from() == otherVertex)
                    || (e.from() == rootVertex && e.to() == otherVertex))
                && CompactSparseTypedEdgeSet.this.contains(e);
        }

        @Override public boolean isEmpty() {
            return !CompactSparseTypedEdgeSet.this.connects(otherVertex);
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
                && CompactSparseTypedEdgeSet.this.remove(e);
        }

        @Override public int size() {
            BitSet b = edges.get(otherVertex);
            return (b == null) ? 0 : b.cardinality();
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * TypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class EdgesForVertexIterator implements Iterator<TypedEdge<T>> {

        private int curTypeIndex;

        private BitSet curTypes;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private TypedEdge<T> next;

        int otherVertex;

        public EdgesForVertexIterator(int otherVertex) {
            this.otherVertex = otherVertex;
            curTypeIndex = -1;
            curTypes = edges.get(otherVertex);
            advance();
        }

        private void advance() {
            next = null;
            while (next == null && curTypes != null) {
                if (curTypes == null) {
                    curTypes = edges.get(otherVertex);
                    curTypeIndex = -1;
                }
                
                if (curTypes == null)
                    break;
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = new SimpleTypedEdge<T>(type, otherVertex, rootVertex);
                }
                // If there were no further types in this edge set, then loop
                // again to load the next set of types for a new vertex, if it exists
                else 
                    curTypes = null;
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
    private class EdgeIterator implements Iterator<TypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private TIntObjectIterator<BitSet> iter;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private TypedEdge<T> next;

        private int curVertex;

        private IntIterator curVertexTypes;


        public EdgeIterator() {
            this.iter = edges.iterator();
            advance();
        }

        private void advance() {
            next = null;            
            while (next == null) {
                // Check whether the current vertex has types left, and if not,
                // load a new vertex's types
                if (curVertexTypes == null || !curVertexTypes.hasNext()) {
                    // If there were no more types to load, stop searching
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    curVertex = iter.key();
                    curVertexTypes = CompactIntSet.wrap(iter.value()).iterator();
                }

                if (curVertexTypes.hasNext()) {
                    int typeIndex = curVertexTypes.nextInt();
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(typeIndex));
                    next = new SimpleTypedEdge<T>(type, curVertex, rootVertex);
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