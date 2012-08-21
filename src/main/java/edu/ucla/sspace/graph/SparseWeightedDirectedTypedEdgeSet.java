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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.DisjointSets;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.util.primitive.AbstractIntSet;
import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;


/**
 * An {@link EdgeSet} implementation that stores {@link DirectedEdge} instances
 * for a vertex.
 *
 * @author David Jurgens
 */
public class SparseWeightedDirectedTypedEdgeSet<T> 
        extends AbstractSet<WeightedDirectedTypedEdge<T>> 
        implements EdgeSet<WeightedDirectedTypedEdge<T>>, java.io.Serializable {

    private final int rootVertex;

    MultiMap<Integer,WeightedDirectedTypedEdge<T>> inEdges;
    MultiMap<Integer,WeightedDirectedTypedEdge<T>> outEdges;

    public SparseWeightedDirectedTypedEdgeSet(int root) {
        this.rootVertex = root;
        inEdges = new HashMultiMap<Integer,WeightedDirectedTypedEdge<T>>();
        outEdges = new HashMultiMap<Integer,WeightedDirectedTypedEdge<T>>();
    }

    /**
     * Adds the edge to this set if one of the vertices is the root vertex and
     * if the non-root vertex has a greater index that this vertex.
     */
    public boolean add(WeightedDirectedTypedEdge<T> e) {
        if (e.from() == rootVertex) {
            Set<WeightedDirectedTypedEdge<T>> edges = outEdges.get(e.to());
            if (edges.contains(e))
                return false;

            // We can't rely on the edge's equality method since that uses the
            // edge weight, so we'll have to iterate over the edges to see if we
            // have one that matches the type, and if so replace it with the
            // different weight
            if (!edges.isEmpty()) {
                Iterator<WeightedDirectedTypedEdge<T>> iter = edges.iterator();
                WeightedDirectedTypedEdge<T> existing = null;
                while (iter.hasNext()) {
                    WeightedDirectedTypedEdge<T> n = iter.next();
                    if (n.to() == e.to() 
                            && n.edgeType().equals(e.edgeType())) {
                        existing = n;
                        break;
                    }
                }
                if (existing == null) {
                    outEdges.put(e.to(), e);
                    return true;                    
                }
                // Check if has the same weight;
                else if (e.weight() != existing.weight()) {
                    outEdges.remove(e.to(), existing);
                    outEdges.put(e.to(), e);
                    return true;
                }
                else 
                    return false;
            }
            else 
                return  outEdges.put(e.to(), e);
        }

        else if (e.to() == rootVertex) {
            Set<WeightedDirectedTypedEdge<T>> edges = inEdges.get(e.from());
            if (edges.contains(e))
                return false;

            // We can't rely on the edge's equality method since that uses the
            // edge weight, so we'll have to iterate over the edges to see if we
            // have one that matches the type, and if so replace it with the
            // different weight
            if (!edges.isEmpty()) {
                Iterator<WeightedDirectedTypedEdge<T>> iter = edges.iterator();
                WeightedDirectedTypedEdge<T> existing = null;
                while (iter.hasNext()) {
                    WeightedDirectedTypedEdge<T> n = iter.next();
                    if (n.from() == e.from() 
                            && n.edgeType().equals(e.edgeType())) {
                        existing = n;
                        break;
                    }
                }

                if (existing == null) {
                    inEdges.put(e.from(), e);
                    return true;                    
                }
                // Check if has the same weight;
                else if (e.weight() != existing.weight()) {
                    inEdges.remove(e.from(), existing);
                    inEdges.put(e.from(), e);
                    return true;
                }
                return false;
            }
            else 
                return inEdges.put(e.from(), e);
        }
        return false;
    }    

    /**
     * {@inheritDoc}
     */
    public void clear() {
        inEdges.clear();
        outEdges.clear();
    }
    
    /**
     * {@inheritDoc}
     */
    public IntSet connected() {
        TroveIntSet t = new TroveIntSet();
        t.addAll(inEdges.keySet());
        t.addAll(outEdges.keySet());
        return t;
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex) {
        return inEdges.containsKey(vertex) || outEdges.containsKey(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean connects(int vertex, T type) {
        if (inEdges.containsKey(vertex)) {
            for (WeightedDirectedTypedEdge<T> e : inEdges.get(vertex)) {
                if (e.edgeType().equals(type))
                    return true;
            }
        }
        else if (outEdges.containsKey(vertex)) {
            for (WeightedDirectedTypedEdge<T> e : outEdges.get(vertex)) {
                if (e.edgeType().equals(type))
                    return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Object o) {
        if (!(o instanceof WeightedDirectedTypedEdge))
            return false;

        @SuppressWarnings("unchecked")
        WeightedDirectedTypedEdge<T> e = (WeightedDirectedTypedEdge<T>)o;

        if (e.from() == rootVertex) {
            Set<WeightedDirectedTypedEdge<T>> edges = outEdges.get(e.to());
            return edges.contains(e);
        }

        else if (e.to() == rootVertex) {
            Set<WeightedDirectedTypedEdge<T>> edges = inEdges.get(e.from());
            return edges.contains(e);
        }
        return false;
        
    }

    public SparseWeightedDirectedTypedEdgeSet<T> copy(IntSet vertices) {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public int disconnect(int vertex) {
        Set<WeightedDirectedTypedEdge<T>> in = inEdges.remove(vertex);
        Set<WeightedDirectedTypedEdge<T>> out = outEdges.remove(vertex);
        return in.size() + out.size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<WeightedDirectedTypedEdge<T>> getEdges(T type) {
        Set<WeightedDirectedTypedEdge<T>> edges =
            new HashSet<WeightedDirectedTypedEdge<T>>();
        for (WeightedDirectedTypedEdge<T> e : inEdges.values())
            if (e.edgeType().equals(type))
                edges.add(e);
        for (WeightedDirectedTypedEdge<T> e : outEdges.values())
            if (e.edgeType().equals(type))
                edges.add(e);
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public Set<WeightedDirectedTypedEdge<T>> getEdges(Set<T> types) {
        Set<WeightedDirectedTypedEdge<T>> edges =
            new HashSet<WeightedDirectedTypedEdge<T>>();
        for (WeightedDirectedTypedEdge<T> e : inEdges.values())
            if (types.contains(e.edgeType()))
                edges.add(e);
        for (WeightedDirectedTypedEdge<T> e : outEdges.values())
            if (types.contains(e.edgeType()))
                edges.add(e);
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public Set<WeightedDirectedTypedEdge<T>> getEdges(int vertex) {
        DisjointSets<WeightedDirectedTypedEdge<T>> edges =
            new DisjointSets<WeightedDirectedTypedEdge<T>>();
        edges.append(inEdges.get(vertex));
        edges.append(outEdges.get(vertex));
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public Set<WeightedDirectedTypedEdge<T>> getEdges(int vertex, Set<T> types) {
        Set<WeightedDirectedTypedEdge<T>> edges =
            new HashSet<WeightedDirectedTypedEdge<T>>();
        for (WeightedDirectedTypedEdge<T> e : inEdges.get(vertex))
            if (types.contains(e.edgeType()))
                edges.add(e);
        for (WeightedDirectedTypedEdge<T> e : outEdges.get(vertex))
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

    public Set<WeightedDirectedTypedEdge<T>> incoming() {
        return Collections.<WeightedDirectedTypedEdge<T>>unmodifiableSet(
            new HashSet<WeightedDirectedTypedEdge<T>>(inEdges.values()));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return inEdges.isEmpty() && outEdges.isEmpty();
    }

    /**
     * {@inheritDoc}
     */ 
    public Iterator<WeightedDirectedTypedEdge<T>> iterator() {
        List<Iterator<WeightedDirectedTypedEdge<T>>> iters =
            new ArrayList<Iterator<WeightedDirectedTypedEdge<T>>>();
        iters.add(Collections.<WeightedDirectedTypedEdge<T>>
                      unmodifiableCollection(inEdges.values()).iterator());
        iters.add(Collections.<WeightedDirectedTypedEdge<T>>
                      unmodifiableCollection(outEdges.values()).iterator());
        return new CombinedIterator(iters);
    }

    public Set<WeightedDirectedTypedEdge<T>> outgoing() {
        return Collections.<WeightedDirectedTypedEdge<T>>unmodifiableSet(
            new HashSet<WeightedDirectedTypedEdge<T>>(outEdges.values()));
    }

    public IntSet predecessors() {
        return new TroveIntSet(inEdges.keySet());
    }

    /**
     * {@inheritDoc}
     */    
    public int size() {
        return inEdges.range() + outEdges.range();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(Object o) {
        if (!(o instanceof WeightedDirectedTypedEdge))
            return false;
        
        @SuppressWarnings("unchecked")
        WeightedDirectedTypedEdge<T> e = (WeightedDirectedTypedEdge<T>)o;

        if (e.from() == rootVertex) {
            return outEdges.remove(e.to(), e);
        }

        else if (e.to() == rootVertex) {
            return inEdges.remove(e.from(), e);
        }
        return false;
    }

    public IntSet successors() {
        return new TroveIntSet(outEdges.keySet());
    }

    /**
     * Returns the sum of the weights of the edges contained in this set.
     */
    public double sum() {
        double sum = 0;
        for (WeightedDirectedTypedEdge<T> e : inEdges.values())
            sum += e.weight();
        for (WeightedDirectedTypedEdge<T> e : outEdges.values())
            sum += e.weight();
        return sum;
    }

    /**
     *
     */
    public Iterator<WeightedDirectedTypedEdge<T>> uniqueIterator() {
        return new UniqueEdgeIterator();
    }

    /**
     * An iterator over the edges in this set that constructs {@link
     * DirectedTypedEdge} instances as it traverses through the set of connected
     * vertices.
     */
    private class UniqueEdgeIterator 
        implements Iterator<WeightedDirectedTypedEdge<T>> {

        Iterator<WeightedDirectedTypedEdge<T>> iter;

        WeightedDirectedTypedEdge<T> next;

        public UniqueEdgeIterator() {
            this.iter = iterator();
            next = null;
            advance();
        }

        private void advance() {
            next = null;
            while (next == null && iter.hasNext()) {
                WeightedDirectedTypedEdge<T> e = iter.next();
                boolean isInEdge = e.to() == rootVertex;
                int otherVertex = (isInEdge) ? e.from() : e.to();
                if (!((isInEdge && rootVertex < otherVertex)
                      || (!isInEdge && rootVertex < otherVertex))) {
                    continue;
                }
                next = e;
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public WeightedDirectedTypedEdge<T> next() {
            if (next == null)
                throw new NoSuchElementException();
            WeightedDirectedTypedEdge<T> n = next;
//             System.out.println("next: " + n);
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
