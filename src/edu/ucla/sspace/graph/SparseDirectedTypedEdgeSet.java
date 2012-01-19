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
 * An {@link EdgeSet} implementation that stores {@link DirectedTypedEdge}
 * instances for a vertex.  This class provides additional methods beyond the
 * {@code EdgeSet} interface for interacting with edges on the basis of their
 * type and their orientation.
 */
public class SparseDirectedTypedEdgeSet<T> 
        extends AbstractSet<DirectedTypedEdge<T>> 
        implements EdgeSet<DirectedTypedEdge<T>>, java.io.Serializable {

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
     * A mapping from a type to the set of incoming edges
     */
    private final TIntObjectHashMap<BitSet> inEdges;

    /**
     * A mapping from a type to the set of outgoing edges
     */
    private final TIntObjectHashMap<BitSet> outEdges;

    /**
     * The set of vertices that are connected to the root vertex (its
     * neighbors).  Although this set could be computed by unioning the keyset
     * of {@code inEdges} and {@code outEdges}, representing it as its own data
     * structure offers significantly faster iteration over the set returned by
     * {@link #connected()}.
     */
    private final TIntHashSet connected;

    /**
     * The number of edges in this set.  Note that this is different than the
     * size of {@code connected}, which doesn't include bidirectional
     * connections.
     */
    private int size;
        
    /**
     * Creates a new {@code SparseDirectedTypedEdgeSet} for the specfied vertex.
     */
    public SparseDirectedTypedEdgeSet(int rootVertex) {
        this.rootVertex = rootVertex;
        inEdges = new TIntObjectHashMap<BitSet>();
        outEdges = new TIntObjectHashMap<BitSet>();
        connected = new TIntHashSet();
        size = 0;
    }
    
    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(DirectedTypedEdge<T> e) {
        if (e.from() == rootVertex) 
            return add(outEdges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return add(inEdges, e.from(), e.edgeType());
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
            connected.add(i);
            size++;
            return true;
        }
        // Otherwise, lookup the type's index and see if it already exists in
        // the bitset, indicating the edge does too
        int index = index(type);
        if (!types.get(index)) {
            types.set(index);
            connected.add(i);
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
        inEdges.clear();
        outEdges.clear();
        connected.clear();
        size = 0;
    }

    /**
     * {@inheritDoc}  The set of vertices returned by this set is immutable.
     */
    public IntSet connected() {
        return TroveIntSet.wrap(connected);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return connected.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex, T type) {
        BitSet types = inEdges.get(vertex);
        if (types != null && types.get(index(type)))
            return true;
        types = outEdges.get(vertex);
        return types != null && types.get(index(type));
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;
        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;

        if (e.from() == rootVertex) 
            return contains(outEdges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return contains(inEdges, e.from(), e.edgeType());
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
    public SparseDirectedTypedEdgeSet<T> copy(IntSet vertices) {        
        SparseDirectedTypedEdgeSet<T> copy = 
            new SparseDirectedTypedEdgeSet<T>(rootVertex);
        if (vertices.size() < inEdges.size()
                && vertices.size() < outEdges.size()) {

            IntIterator iter = vertices.iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                if (inEdges.containsKey(v)) 
                    copy.inEdges.put(v, outEdges.get(v));
                if (outEdges.containsKey(v)) 
                    copy.inEdges.put(v, outEdges.get(v));
            }            
        }
        else {
            TIntObjectIterator<BitSet> iter = inEdges.iterator();
            while (iter.hasNext()) {
                iter.advance();
                int v = iter.key();
                if (vertices.contains(v))
                    copy.inEdges.put(v, iter.value());
            }
            iter = outEdges.iterator();
            while (iter.hasNext()) {
                iter.advance();
                int v = iter.key();
                if (vertices.contains(v))
                    copy.outEdges.put(v, iter.value());
            }
        }
        return copy;
    }

    /*
     * This code was *MUCH* slower than an Object-based copy for unknown
     * reasons.  I was hoping the DirectedMultigraph.copy() could be sped up by
     * copying the raw data faster than the Edge-based data, but this
     * implementation actually slows down DirectedMultigraph.copy() by 100X.
     * It's being left in as a future study on how to fix it to speed up the
     * copy operation.
     */

//     public SparseDirectedTypedEdgeSet<T> copy(Set<Integer> vertices) {
//         SparseDirectedTypedEdgeSet<T> set = 
//             new SparseDirectedTypedEdgeSet<T>(rootVertex);
//         // REMINDER: put a special case in for size==0?            
//         for (int v : vertices) {
//             if (connected.contains(v)) {
//                 set.connected.add(v);
//                 // Test whether the vertex is connected by an incoming edge
//                 if (inEdges.containsKey(v)) {
//                     BitSet types = (BitSet)(inEdges.get(v).clone());
//                     set.inEdges.put(v, types);
//                     set.size += types.cardinality();
//                     // Check for the bi-directional case.
//                     if (outEdges.containsKey(v)) {
//                         types = (BitSet)(outEdges.get(v).clone());
//                         set.outEdges.put(v, types);
//                         set.size += types.cardinality();
//                     }

//                 }
//                 // If the vertex was in connected and was not in the inEdges,
//                 // then it must be connected by an out edge
//                 else {
//                     BitSet types = (BitSet)(outEdges.get(v).clone());
//                     set.outEdges.put(v, types);
//                     set.size += types.cardinality();
//                 }
//             }
//         }
//         return set;
//     }

    /**
     * {@inheritDoc}
     */
    public int disconnect(int v) {
        if (connected.remove(v)) {
            int removed = 0;
            BitSet b = inEdges.remove(v);
            if (b != null) {
                int edges = b.cardinality();
                size -= edges;
                removed += edges;
            }
            b = outEdges.remove(v);
            if (b != null) {
                int edges = b.cardinality();
                size -= edges;
                removed += edges;
            }
            assert removed > 0 :
                "connected removed an edge that wasn't listed elsewhere";
            return removed;
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(final T type) {    
        if (!TYPE_INDICES.containsKey(type))
            return Collections.<DirectedTypedEdge<T>>emptySet();
        final int typeIndex = index(type);
        final Set<DirectedTypedEdge<T>> edges = new HashSet<DirectedTypedEdge<T>>();
        inEdges.forEachEntry(new TIntObjectProcedure<BitSet>() {
                public boolean execute(int v, BitSet types) {
                    if (types.get(typeIndex))
                        edges.add(new SimpleDirectedTypedEdge<T>(
                                      type, v, rootVertex));
                    return true;
                }
            });
        outEdges.forEachEntry(new TIntObjectProcedure<BitSet>() {
                public boolean execute(int v, BitSet types) {
                    if (types.get(typeIndex))
                        edges.add(new SimpleDirectedTypedEdge<T>(
                                      type, rootVertex, v));
                    return true;
                }
            });
        return edges;
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
    public Set<DirectedTypedEdge<T>> getEdges(int vertex, Set<T> types) {
        // NOTE: this is purely unoptimized code, so fix if it ever gets in a
        // hotspot
        Set<DirectedTypedEdge<T>> edges = new HashSet<DirectedTypedEdge<T>>();
        for (DirectedTypedEdge<T> e : new EdgesForVertex(vertex))
            if (types.contains(e.edgeType()))
                edges.add(e);
        return edges;
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
    public Set<DirectedTypedEdge<T>> incoming() {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return connected.isEmpty();
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<DirectedTypedEdge<T>> iterator() {
        return new EdgeIterator();
    }

    /**
     * Returns the set of edges that originate from the vertex associated with
     * this edge set.
     */
     public Set<DirectedTypedEdge<T>> outgoing() {
         throw new Error();
     }

    public IntSet predecessors() {
        return TroveIntSet.wrap(inEdges.keySet());
    }

    public IntSet successors() {
        return TroveIntSet.wrap(outEdges.keySet());
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof DirectedTypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;


        if (e.from() == rootVertex) 
            return remove(outEdges, e.to(), e.edgeType());
        else if (e.to() == rootVertex) 
            return remove(inEdges, e.from(), e.edgeType());
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
                // Check whether the other set has this edge, and if not, remove
                // it from the cache of connected vertices
                TIntObjectHashMap<BitSet> other = (edges == inEdges)
                    ? outEdges : inEdges;
                if (!other.containsKey(i))
                    connected.remove(i);
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
        throw new Error();
    }

    /**
     *
     */
    public Iterator<DirectedTypedEdge<T>> uniqueIterator() {
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
            for (TIntObjectIterator<BitSet> it = inEdges.iterator(); it.hasNext(); ) {
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
            // Remap all the in-edges vertices' types...
            for (TIntObjectIterator<BitSet> it = outEdges.iterator(); it.hasNext(); ) {
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
            return new EdgesForVertexIterator(otherVertex);
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
            BitSet in = inEdges.get(otherVertex);
            BitSet out = outEdges.get(otherVertex);
            return ((in == null) ? 0 : in.cardinality())
                + ((out == null) ? 0 : out.cardinality());
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class EdgesForVertexIterator implements Iterator<DirectedTypedEdge<T>> {

        private int curTypeIndex;

        private BitSet curTypes;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;

        boolean areInEdges;

        int otherVertex;

        public EdgesForVertexIterator(int otherVertex) {
            this.otherVertex = otherVertex;
            areInEdges = true;
            curTypeIndex = -1;
            curTypes = inEdges.get(otherVertex);
            advance();
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (curTypes == null && areInEdges) {
                    curTypes = outEdges.get(otherVertex);
                    areInEdges = false;
                    curTypeIndex = -1;
                }
                
                if (curTypes == null)
                    break;
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = (areInEdges)
                        ? new SimpleDirectedTypedEdge<T>(
                              type, otherVertex, rootVertex)
                        : new SimpleDirectedTypedEdge<T>(
                              type, rootVertex, otherVertex);
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

        public DirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class EdgeIterator implements Iterator<DirectedTypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private TIntObjectIterator<BitSet> iter;

        private int curTypeIndex;

        private BitSet curTypes;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;

        boolean areInEdges;

        public EdgeIterator() {
            areInEdges = true;
            curTypeIndex = -1;
            this.iter = inEdges.iterator();
            advance();
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (curTypes == null) {
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    curTypeIndex = -1;
                    curTypes = iter.value();
                    //System.out.printf("number of types for %d: %d%n", iter.key(), iter.value().cardinality());                    
                }
                    
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
//                 System.out.printf("root: %d, v: %d, in: %s: %d%n", 
//                                   rootVertex, iter.key(), areInEdges, curTypeIndex);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = (areInEdges)
                        ? new SimpleDirectedTypedEdge<T>(
                              type, iter.key(), rootVertex)
                        : new SimpleDirectedTypedEdge<T>(
                              type, rootVertex, iter.key());
                }
                // If there were no further types in this edge set, then loop
                // again to load the next set of types for a new vertex, if it exists
                else 
                    curTypes = null;
            }

            // One time case: for switching iterators to the outgoing edges
            if (next == null && areInEdges) {
                areInEdges = false;
                iter = outEdges.iterator();
                curTypes = null;
                advance();
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
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class UniqueEdgeIterator implements Iterator<DirectedTypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private TIntObjectIterator<BitSet> iter;

        private int curTypeIndex;

        private BitSet curTypes;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;

        boolean areInEdges;

        public UniqueEdgeIterator() {
            areInEdges = true;
            this.iter = inEdges.iterator();
            advance();
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (curTypes == null) {
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    curTypeIndex = -1;
                    int otherVertex = iter.key();
                    
                    // Check if we should create edges for this vertex combination
                    if (!((areInEdges && rootVertex < otherVertex)
                              || (!areInEdges && rootVertex < otherVertex))) {
                        curTypes = null;
                        continue;
                    }
                    else 
                        curTypes = iter.value();
                }
                    
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = (areInEdges)
                        ? new SimpleDirectedTypedEdge<T>(
                              type, iter.key(), rootVertex)
                        : new SimpleDirectedTypedEdge<T>(
                              type, rootVertex, iter.key());
                }
                // If there were no further types in this edge set, then loop
                // again to load the next set of types for a new vertex, if it exists
                else 
                    curTypes = null;
            }

            // One time case: for switching iterators to the outgoing edges
            if (next == null && areInEdges) {
                areInEdges = false;
                iter = outEdges.iterator();
                curTypes = null;
                advance();
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public DirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
//             System.out.println("next: " + n);
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class InEdgeIterator implements Iterator<DirectedTypedEdge<T>> {
        /**
         * An iterator over the incoming edges for the current type
         */
        private final TIntObjectIterator<BitSet> iter;

        private int curTypeIndex;

        private BitSet curTypes;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;

        public InEdgeIterator() {
            this.iter = inEdges.iterator();
            advance();
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (curTypes == null) {
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    curTypeIndex = -1;
                    curTypes = iter.value();
//                     System.out.printf("number of types for %d: %d%n", iter.key(), iter.value().cardinality());                    
                }
                    
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = new SimpleDirectedTypedEdge<T>(
                        type, iter.key(), rootVertex);
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

        public DirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class OutEdgeIterator implements Iterator<DirectedTypedEdge<T>> {

        /**
         * An iterator over the incoming edges for the current type
         */
        private final TIntObjectIterator<BitSet> iter;

        private BitSet curTypes;

        private int curTypeIndex;

        /**
         * The next edge to return.  This field is updated by {@link advance()}
         */
        private DirectedTypedEdge<T> next;

        public OutEdgeIterator() {
            this.iter = outEdges.iterator();
            advance();
        }

        private void advance() {
            next = null;
            while (next == null) {
                if (curTypes == null) {
                    if (!iter.hasNext())
                        break;
                    iter.advance();
                    curTypeIndex = -1;
                    curTypes = iter.value();
                    // System.out.printf("number of types for %d: %d%n", iter.key(), iter.value().cardinality());                    
                }
                    
                curTypeIndex = curTypes.nextSetBit(curTypeIndex + 1);
                if (curTypeIndex >= 0) {
                    // We know that the TYPES map has the right object type
                    @SuppressWarnings("unchecked")
                    T type = (T)(TYPES.get(curTypeIndex));
                    next = new SimpleDirectedTypedEdge<T>(
                        type, rootVertex, iter.key());
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

        public DirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            DirectedTypedEdge<T> n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}