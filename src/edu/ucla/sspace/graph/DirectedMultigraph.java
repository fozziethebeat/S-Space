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

import java.lang.ref.WeakReference;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.DisjointSets;
import edu.ucla.sspace.util.SetDecorator;

import edu.ucla.sspace.util.primitive.AbstractIntSet;
import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.PrimitiveCollections;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


/**
 * A directed multigraph implementation.  
 *
 * @param T a class type whose values are used to distinguish between edge types
 * 
 * @author David Jurgens
 */
public class DirectedMultigraph<T> 
    implements Multigraph<T,DirectedTypedEdge<T>>, 
               DirectedGraph<DirectedTypedEdge<T>>, 
               java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
   
    /**
     * The count of the type distribution for all edges in the graph.
     */
    private final TObjectIntMap<T> typeCounts;
    
    /**
     * The set of vertices in this mutligraph.  This set is maintained
     * independently of the vertex sets in the type-specific edge graphs to
     * provide a fast, type independent state of which vertices are in the
     * graph.
     */
    private final TIntObjectMap<SparseDirectedTypedEdgeSet<T>> vertexToEdges;

    /**
     * A collection of all the subgraphs that have been returned from this
     * graph.  This collection is necessary to inform subgraphs of any vertex
     * changes (removals) to this graph without requiring them to constantly
     * check for updates.  We use a {@link WeakReference} in order to keep track
     * of the canonical {@code Subgraph} instance while ensuring that it is
     * garbage collected when it is no longer referred to (which would never
     * happen if this list contained strong references).
     */
    private Collection<WeakReference<Subgraph>> subgraphs;

    /**
     * The number of edges contained in this graph.
     */
    private int size;
    
    /**
     * Creates an empty graph with node edges
     */
    public DirectedMultigraph() { 
        typeCounts = new TObjectIntHashMap<T>();
        vertexToEdges = new TIntObjectHashMap<SparseDirectedTypedEdgeSet<T>>();
        subgraphs = new ArrayList<WeakReference<Subgraph>>();
        size = 0;
    }

    /**
     * Creates a directed multigraph with a copy of all the vertices and edges
     * in {@code g}.
     */
    public DirectedMultigraph(Graph<? extends DirectedTypedEdge<T>> g) {
        this();
        for (Integer v : g.vertices())
            add(v);
        for (DirectedTypedEdge<T> e : g.edges())
            add(e);
    }    

    /**
     * {@inheritDoc}
     */
    public boolean add(int vertex) {
        if (!vertexToEdges.containsKey(vertex)) {
            vertexToEdges.put(vertex, new SparseDirectedTypedEdgeSet<T>(vertex));
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(DirectedTypedEdge<T> e) {
        SparseDirectedTypedEdgeSet<T> from = vertexToEdges.get(e.from());
        if (from == null) {
            from = new SparseDirectedTypedEdgeSet<T>(e.from());
            vertexToEdges.put(e.from(), from);
        }
        if (from.add(e)) {
            updateTypeCounts(e.edgeType(), 1);
            SparseDirectedTypedEdgeSet<T> to = vertexToEdges.get(e.to());
            if (to == null) {
                to = new SparseDirectedTypedEdgeSet<T>(e.to());
                vertexToEdges.put(e.to(), to);
            }
            to.add(e);
            size++;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        vertexToEdges.clear();
        typeCounts.clear();
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() { 
        // NOTE: potentially rewrite with a Procedure if this ever needs to be
        // optimized
        for (SparseDirectedTypedEdgeSet<T> edges 
                 : vertexToEdges.valueCollection())
            edges.clear();
        size = 0;
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges(T edgeType) { 
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex) {
        return vertexToEdges.containsKey(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Edge e) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(e.to());
        return edges != null && edges.contains(e);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex1);
        return edges != null && edges.connects(vertex2);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2, T edgeType) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex2);
        return edges != null && edges.connects(vertex2, edgeType);
    }

    /**
     * {@inheritDoc}
     */
    public DirectedMultigraph<T> copy(Set<Integer> toCopy) {
        // special case for If the called is requesting a copy of the entire
        // graph, which is more easily handled with the copy constructor
        if (toCopy.size() == order() && toCopy.equals(vertices()))
            return new DirectedMultigraph<T>(this);

        DirectedMultigraph<T> g = new DirectedMultigraph<T>();
        for (int v : toCopy) {
            if (!vertexToEdges.containsKey(v))
                throw new IllegalArgumentException(
                    "Request copy of non-present vertex: " + v);
            g.add(v);
            SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(v);
            if (edges == null)
                throw new IllegalArgumentException();
            for (int v2 : toCopy) {
                if (v == v2)
                    break;
                if (edges.connects(v2))
                    for (DirectedTypedEdge<T> e : edges.getEdges(v2))
                        g.add(e);
            }
        }
        return g;
    }

    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) ? 0 : edges.size();
    }

    /**
     * Returns the set of typed edges in the graph
     */
    public Set<DirectedTypedEdge<T>> edges() {
        return new EdgeView();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> edges(T t) {
        Set<DirectedTypedEdge<T>> edges =
            new HashSet<DirectedTypedEdge<T>>();
        for (SparseDirectedTypedEdgeSet<T> set :
                 vertexToEdges.valueCollection()) {
            Set<DirectedTypedEdge<T>> edgesOfType = set.getEdges(t);
            if (!edgesOfType.isEmpty())
                edges.addAll(edgesOfType);
        }
        return edges;
    }

    /**
     * Returns the set of edge types currently present in this graph.
     */
    public Set<T> edgeTypes() {
        return Collections.unmodifiableSet(typeCounts.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (o instanceof DirectedMultigraph) {
            DirectedMultigraph<?> dm = (DirectedMultigraph<?>)(o);
            if (dm.typeCounts.equals(typeCounts)) {
                return vertexToEdges.equals(dm.vertexToEdges);
            }
            return false;
        }
        else if (o instanceof Multigraph) {
            @SuppressWarnings("unchecked")
            Multigraph<?,TypedEdge<?>> m = (Multigraph<?,TypedEdge<?>>)o;
            if (m.edgeTypes().equals(typeCounts.keySet())) {
                return m.order() == order()
                    && m.size() == size()
                    && m.vertices().equals(vertices())
                    && m.edges().equals(edges());
            }
            return false;
        }
        else if (o instanceof Graph) {
            Graph<?> g = (Graph<?>)o;
            return g.order() == order()
                && g.size() == size()
                && g.vertices().equals(vertices())
                && g.edges().equals(edges());                    
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : new EdgeListWrapper(edges);
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex1);
        return (edges == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : edges.getEdges(vertex2);
    }


    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2, 
                                              Set<T> types) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex1);        
        return (edges == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : edges.getEdges(vertex2, types);        
    }

    /**
     * {@inheritDoc}
     */
    public IntSet getNeighbors(int vertex) {
         SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
         return (edges == null) 
             ? PrimitiveCollections.emptyIntSet()
             : PrimitiveCollections.unmodifiableSet(edges.connected());
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCycles() {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return vertexToEdges.keySet().hashCode() ^ (typeCounts.hashCode() * size);
    }

    /**
     * {@inheritDoc}
     */
    public int inDegree(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) 
            ? 0
            : edges.predecessors().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> inEdges(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : new EdgeListWrapper(edges.incoming());
    }

    /**
     * {@inheritDoc}
     */
    public int order() {
        return vertexToEdges.size();
    }

    /**
     * {@inheritDoc}
     */
    public int outDegree(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) 
            ? 0
            : edges.successors().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedTypedEdge<T>> outEdges(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
        return (edges == null) 
            ? Collections.<DirectedTypedEdge<T>>emptySet()
            : new EdgeListWrapper(edges.outgoing());
    }

    /**
     * {@inheritDoc}
     */
    public IntSet predecessors(int vertex) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
         return (edges == null) 
             ? PrimitiveCollections.emptyIntSet()
             : PrimitiveCollections.unmodifiableSet(edges.predecessors());
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        // If we can remove the vertex from the global set, then at least one of
        // the type-specific graphs has this vertex.
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.remove(vertex);
        if (edges != null) {
            size -= edges.size();
            for (int other : edges.connected()) {
                vertexToEdges.get(other).disconnect(vertex);
            }

            // Check whether removing this vertex has caused us to remove
            // the last edge for this type in the graph.  If so, the graph
            // no longer has this type and we need to update the state.
            for (DirectedTypedEdge<T> e : edges)
                updateTypeCounts(e.edgeType(), -1);

            // Update any of the subgraphs that had this vertex to notify them
            // that it was removed
            Iterator<WeakReference<Subgraph>> iter = subgraphs.iterator();
            while (iter.hasNext()) {
                WeakReference<Subgraph> ref = iter.next();
                Subgraph s = ref.get();
                // Check whether this subgraph was already gc'd (the subgraph
                // was no longer in use) and if so, remove the ref from the list
                // to avoid iterating over it again
                if (s == null) {
                    iter.remove();
                    continue;
                }
                // If we removed the vertex from the subgraph, then check
                // whether we also removed any of the types in that subgraph
                if (s.vertexSubset.remove(vertex)) {
                    Iterator<T> subgraphTypesIter = s.validTypes.iterator();
                    while (subgraphTypesIter.hasNext()) {
                        if (!typeCounts.containsKey(subgraphTypesIter.next()))
                            subgraphTypesIter.remove();
                    }
                }
            }
            return true;
        }
        return false;        
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(DirectedTypedEdge<T> edge) {
        SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(edge.to());
        if (edges != null && edges.remove(edge)) {
            vertexToEdges.get(edge.from()).remove(edge);
            size--;
            // Check whether we've just removed the last edge for this type
            // in the graph.  If so, the graph no longer has this type and
            // we need to update the state.
            updateTypeCounts(edge.edgeType(), -1);

            if (!typeCounts.containsKey(edge.edgeType())) {
                // Remove this edge type from all the subgraphs as well
                Iterator<WeakReference<Subgraph>> sIt = subgraphs.iterator();
                while (sIt.hasNext()) {
                    WeakReference<Subgraph> ref = sIt.next();
                    Subgraph s = ref.get();
                    // Check whether this subgraph was already gc'd (the
                    // subgraph was no longer in use) and if so, remove the
                    // ref from the list to avoid iterating over it again
                    if (s == null) {
                        sIt.remove();
                        continue;
                    }
                    s.validTypes.remove(edge.edgeType());
                }
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
     * {@inheritDoc}
     */
    public IntSet successors(int vertex) {
         SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
         return (edges == null) 
             ? PrimitiveCollections.emptyIntSet()
             : PrimitiveCollections.unmodifiableSet(edges.successors());
    }

    /**
     * {@inheritDoc}
     */
    public DirectedMultigraph<T> subgraph(Set<Integer> subset) {
        Subgraph sub = new Subgraph(typeCounts.keySet(), subset);
        subgraphs.add(new WeakReference<Subgraph>(sub));
        return sub;
    }

    /**
     * {@inheritDoc}
     */
    public DirectedMultigraph<T> subgraph(Set<Integer> subset, Set<T> edgeTypes) {
        if (edgeTypes.isEmpty()) 
            throw new IllegalArgumentException("Must specify at least one type");
        if (!typeCounts.keySet().containsAll(edgeTypes)) {
            throw new IllegalArgumentException(
                "Cannot create subgraph with more types than exist");
        }
        Subgraph sub = new Subgraph(edgeTypes, subset);
        subgraphs.add(new WeakReference<Subgraph>(sub));
        return sub;
    }     

    /**
     * Returns a description of the graph as the sequence of its edges.
     */
    public String toString() {
        // REMINDER: make this more efficient with a StringBuilder
        return "{ vertices: " + vertices() + ", edges: " + edges() + "}";
    }

    /**
     * Updates how many edges have this type in the graph
     */
    private void updateTypeCounts(T type, int delta) {
        if (!typeCounts.containsKey(type)) {
            assert delta > 0 
                : "removing edge type that was not originally present";
            typeCounts.put(type, delta);
        }
        else {
            int curCount = typeCounts.get(type);
            int newCount = curCount + delta;
            assert newCount >= 0 
                : "removing edge type that was not originally present";
            if (newCount == 0)
                typeCounts.remove(type);
            else
                typeCounts.put(type, newCount);
        }
    }

    /**
     * {@inheritDoc}
     */
    public IntSet vertices() {
        return PrimitiveCollections.unmodifiableSet(
            TroveIntSet.wrap(vertexToEdges.keySet()));
    }

    /**
     * The {@code EdgeView} class encapsulates all the of the edge sets from the
     * different edge types.  Because {@link CombinedSet} does not allow for
     * addition, we add an {@code add} method that handles adding new edges to
     * the graph via this set.
     */
    class EdgeView extends AbstractSet<DirectedTypedEdge<T>> {

        public EdgeView() { }

        public boolean add(DirectedTypedEdge<T> e) {
            // Calling the backing graph's add will route the edge to the
            // type-appropriate set
            return DirectedMultigraph.this.add(e);
        }

        public boolean contains(Object o) {
            return (o instanceof DirectedTypedEdge) 
                && DirectedMultigraph.this.contains((DirectedTypedEdge<?>)o);
        }

        public Iterator<DirectedTypedEdge<T>> iterator() {
            return new EdgeIterator();
        }
        
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            if (o instanceof DirectedTypedEdge) {
                DirectedTypedEdge<?> e = (DirectedTypedEdge<?>)o;
                return DirectedMultigraph.this.typeCounts.
                           containsKey(e.edgeType())
                    && DirectedMultigraph.this.remove((DirectedTypedEdge<T>)o);
            }
            return false;
        }

        public int size() {
            return DirectedMultigraph.this.size();
        }

        class EdgeIterator implements Iterator<DirectedTypedEdge<T>> {
            
            Iterator<SparseDirectedTypedEdgeSet<T>> edgeSets;
            Iterator<DirectedTypedEdge<T>> edges;
            DirectedTypedEdge<T> cur = null;
            public EdgeIterator() {
                edgeSets = vertexToEdges.valueCollection().iterator();
                advance();
            }
            
            private void advance() {
                while ((edges == null || !edges.hasNext())
                           && edgeSets.hasNext()) {
                    SparseDirectedTypedEdgeSet<T> edgeSet = edgeSets.next();
                    edges = edgeSet.uniqueIterator();                    
                }
            }
            
            public boolean hasNext() {
                return edges != null && edges.hasNext();
            }

            public DirectedTypedEdge<T> next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                cur = edges.next();
                advance();
                return cur;
            }

            public void remove() {
                if (cur == null)
                    throw new IllegalStateException();
                DirectedMultigraph.this.remove(cur);
                cur = null;
            }
        }
    }

    /**
     * A wrapper around a set of of edges that automatically decrements the size
     * of this graph when an edge is removed from it.
     *
     * @see DirectedMultigraph#inEdges(int)
     * @see DirectedMultigraph#outEdges(int)
     */
    class EdgeListWrapper extends SetDecorator<DirectedTypedEdge<T>> {

        private static final long serialVersionUID = 1L;

        public EdgeListWrapper(Set<DirectedTypedEdge<T>> set) {
            super(set);
        }

        @Override public boolean add(DirectedTypedEdge<T> e) {
            return DirectedMultigraph.this.add(e);
        }

        @Override public boolean addAll(Collection<? extends DirectedTypedEdge<T>> c) {
            boolean added = false;
            for (DirectedTypedEdge<T> e : c) {
                if (DirectedMultigraph.this.add(e)) 
                    added = true;
            }
            return added;
        }

        @Override public boolean remove(Object o) {
            if (o instanceof DirectedTypedEdge) {
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;
                return DirectedMultigraph.this.remove(e);
            }
            return false;
        }

        @Override public boolean removeAll(Collection<?> c) {
            boolean removed = false;
            for (Object o : c) {
                if (o instanceof DirectedTypedEdge) {
                    @SuppressWarnings("unchecked")
                    DirectedTypedEdge<T> e = (DirectedTypedEdge<T>)o;
                    if (DirectedMultigraph.this.remove(e)) 
                        removed = true;
                }
            }
            return removed;
        }

        @Override public boolean retainAll(Collection<?> c) {
            throw new Error("FIXME");
        }
    } 

    /**
     * An implementation for handling the subgraph behavior.
     */
     class Subgraph extends DirectedMultigraph<T> {

        private static final long serialVersionUID = 1L;

        /**
         * The set of types in this subgraph
         */
        private final Set<T> validTypes;

        /**
         * The set of vertices in this subgraph
         */
        private final IntSet vertexSubset;

        public Subgraph(Set<T> validTypes, Set<Integer> vertexSubset) {
            this.validTypes = validTypes;
            this.vertexSubset = new TroveIntSet(vertexSubset);
        }

        /**
         * {@inheritDoc}
         */
        public boolean add(int vertex) {
            if (vertexSubset.contains(vertex))
                return false;
            throw new UnsupportedOperationException(
                "Cannot add new vertex to subgraph");
        }

        /**
         * {@inheritDoc}
         */
        public boolean add(DirectedTypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                throw new UnsupportedOperationException(
                    "Cannot add new vertex to subgraph");

            return DirectedMultigraph.this.add(e);
        }

        /**
         * {@inheritDoc}
         */        
        public void clear() {
            for (Integer v : vertexSubset)
                DirectedMultigraph.this.remove(v);
            vertexSubset.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges() { 
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges(T edgeType) { 
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex) {
            return vertexSubset.contains(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(Edge e) {
            if (e instanceof DirectedTypedEdge) {
                DirectedTypedEdge<?> te = (DirectedTypedEdge<?>)e;
                return vertexSubset.contains(e.from())
                    && vertexSubset.contains(e.to())
                    && validTypes.contains(te.edgeType())
                    && DirectedMultigraph.this.contains(e);
            }
            return false;
        }
    
        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex1, int vertex2) {
            if (!vertexSubset.contains(vertex1)
                    || !vertexSubset.contains(vertex2))
                return false;

            Set<DirectedTypedEdge<T>> edges = 
                DirectedMultigraph.this.getEdges(vertex1, vertex2);
            if (edges != null) {
                // If there were edges between the two vertices, ensure that at
                // least one of them has the type represented in this subgraph
                for (DirectedTypedEdge<T> e : edges)
                    if (validTypes.contains(e.edgeType()))
                        return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(int vertex1, int vertex2, T edgeType) {
            if (!vertexSubset.contains(vertex1)
                    || !vertexSubset.contains(vertex2))
                return false;
            
            return DirectedMultigraph.this.contains(vertex1, vertex2, edgeType);
        }

        /**
         * {@inheritDoc}
         */
        public DirectedMultigraph<T> copy(Set<Integer> vertices) {
            // special case for If the called is requesting a copy of the entire
            // graph, which is more easily handled with the copy constructor
            if (vertices.size() == order() && vertices.equals(vertices()))
                return new DirectedMultigraph<T>(this);
            DirectedMultigraph<T> g = new DirectedMultigraph<T>();
            for (int v : vertices) {
                if (!contains(v))
                    throw new IllegalArgumentException(
                        "Requested copy with non-existant vertex: " + v);
                g.add(v);
                for (DirectedTypedEdge<T> e : getAdjacencyList(v)) {
                    if (vertices.contains(e.from()) && vertices.contains(e.to()))
                        g.add(e);
                }
            }
            return g;            
        }

        /**
         * {@inheritDoc}
         */
        public int degree(int vertex) {
            return getNeighbors(vertex).size();
        }

        /**
         * Returns the set of typed edges in the graph
         */
        public Set<DirectedTypedEdge<T>> edges() {
            return new SubgraphEdgeView();
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> edges(T t) {
            throw new Error("implement me");
        }

        /**
         * Returns the set of edge types currently present in this graph.
         */
        public Set<T> edgeTypes() {
            return Collections.unmodifiableSet(validTypes);
        }

        /**
         * {@inheritDoc}
         */
        @Override public boolean equals(Object o) {
            if (o instanceof Graph) {
                Graph<?> g = (Graph<?>)o;
                return g.order() == order()
                    && g.size() == size()
                    && g.vertices().equals(vertices())
                    && g.edges().equals(edges());                    
            }
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> getAdjacencyList(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<DirectedTypedEdge<T>>emptySet();
            Set<DirectedTypedEdge<T>> adj = 
                DirectedMultigraph.this.getAdjacencyList(vertex);
            return (adj.isEmpty()) 
                ? Collections.<DirectedTypedEdge<T>>emptySet()
                : new SubgraphAdjacencyListView(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> getEdges(int vertex1, int vertex2) {
            if (!vertexSubset.contains(vertex1) 
                    || !vertexSubset.contains(vertex2))
                return Collections.<DirectedTypedEdge<T>>emptySet();

            Set<DirectedTypedEdge<T>> edges = 
                DirectedMultigraph.this.getEdges(vertex1, vertex2, validTypes);
            return edges; 
        }

        /**
         * {@inheritDoc}
         */
        public IntSet getNeighbors(int vertex) {
            SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
            return (edges == null) 
                ? PrimitiveCollections.emptyIntSet()
                : new SubgraphNeighborsView(vertex);
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasCycles() {
            throw new Error();
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return vertices().hashCode() ^ (validTypes.hashCode() * size());
        }

        /**
         * Returns {@code true} if the two vertices are contained within this
         * subgraph and have an edge that is valid within this subgraph's type
         * constraints.
         */
        private boolean hasEdge(int v1, int v2) {
            if (vertexSubset.contains(v1) && vertexSubset.contains(v2)) {
                Set<DirectedTypedEdge<T>> edges = getEdges(v1, v2);
                return edges != null;
            }
            return false;            
        }

        /**
         * {@inheritDoc}
         */
        public int inDegree(int vertex) {
            int inDegree = 0;
            for (DirectedTypedEdge<T> e : getAdjacencyList(vertex)) {
                if (e.to() == vertex)
                    inDegree++;
            }
            return inDegree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> inEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedTypedEdge<T>> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedTypedEdge<T>>emptySet();

            Set<DirectedTypedEdge<T>> in = new HashSet<DirectedTypedEdge<T>>();
            for (DirectedTypedEdge<T> e : edges) {
                if (e.to() == vertex)
                    in.add(e);
            }
            return in;
        }

        /**
         * {@inheritDoc}
         */
        public int order() {
            return vertexSubset.size();
        }

        /**
         * {@inheritDoc}
         */
        public int outDegree(int vertex) {
            int outDegree = 0;
            for (DirectedTypedEdge<T> e : getAdjacencyList(vertex)) {
                if (e.from() == vertex)
                    outDegree++;
            }
            return outDegree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedTypedEdge<T>> outEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedTypedEdge<T>> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedTypedEdge<T>>emptySet();;
            Set<DirectedTypedEdge<T>> out = new HashSet<DirectedTypedEdge<T>>();
            for (DirectedTypedEdge<T> e : edges) {
                if (e.from() == vertex)
                    out.add(e);
            }
            return out;
        }

        /**
         * {@inheritDoc}
         */
        public IntSet predecessors(int vertex) {
         SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
         return (edges == null) 
             ? PrimitiveCollections.emptyIntSet()
             : PrimitiveCollections.unmodifiableSet(edges.predecessors());
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(int vertex) {
            throw new UnsupportedOperationException(
                "Cannot remove vertices from a subgraph");
        }

        /**
         * {@inheritDoc}
         */
        @Override public boolean remove(DirectedTypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                return false;
            return DirectedMultigraph.this.remove(e);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            int size = 0;
            System.out.println("types: " + validTypes);
            next_vertex:
            for (int v1 : vertexSubset) {
                for (int v2 : vertexSubset) {
                    if (v1 == v2)
                        break;
                    size += DirectedMultigraph.this.
                        getEdges(v1, v2, validTypes).size();
                    System.out.printf("%d -> %d had %d edges%n", v1, v2, 
                                      DirectedMultigraph.this.
                                      getEdges(v1, v2, validTypes).size());
                }
            }
            return size;
        }

        /**
         * {@inheritDoc}
         */
        public IntSet successors(int vertex) {
            SparseDirectedTypedEdgeSet<T> edges = vertexToEdges.get(vertex);
            return (edges == null) 
                ? PrimitiveCollections.emptyIntSet()
                : PrimitiveCollections.unmodifiableSet(edges.successors());
        }

        /**
         * {@inheritDoc}
         */
        public DirectedMultigraph<T> subgraph(Set<Integer> verts) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            return DirectedMultigraph.this.subgraph(verts, validTypes);
        }

        /**
         * {@inheritDoc}
         */
        public DirectedMultigraph<T> subgraph(Set<Integer> verts, Set<T> edgeTypes) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            if (!validTypes.containsAll(edgeTypes))
                throw new IllegalArgumentException("provided types is not a " +
                    "subset of the edge types of this graph");
            return DirectedMultigraph.this.subgraph(verts, edgeTypes);
        }
     
        /**
         * {@inheritDoc}
         */
        public IntSet vertices() {
            return PrimitiveCollections.unmodifiableSet(vertexSubset);
        }


        /**
         * A view for the {@code Edge} adjacent edges of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView 
                extends AbstractSet<DirectedTypedEdge<T>> {

            private final int root;

            public SubgraphAdjacencyListView(int root) {
                this.root = root;
            }

            public boolean add(DirectedTypedEdge<T> edge) {
                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.add(edge);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return (e.to() == root ||
                         e.from() == root)
                        && Subgraph.this.contains(e);
            }
            
            public Iterator<DirectedTypedEdge<T>> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            public boolean remove(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                DirectedTypedEdge<?> e = (DirectedTypedEdge<?>)o;
                if (!validTypes.contains(e.edgeType())) 
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> e2 = (DirectedTypedEdge<T>)o;
                return (e2.to() == root ||
                        e2.from() == root)
                    && Subgraph.this.remove(e2);
            }

            public int size() {
                int sz = 0; 
                for (Integer i : vertexSubset)
                    sz += getEdges(root, i).size();
                return sz;
            }

            /**
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class SubgraphAdjacencyListIterator 
                    implements Iterator<DirectedTypedEdge<T>> {

                Iterator<DirectedTypedEdge<T>> edges;

                public SubgraphAdjacencyListIterator() {
                    throw new Error();
                }

                public boolean hasNext() {
                    return edges.hasNext();
                }

                public DirectedTypedEdge<T> next() {
                    return edges.next();
                }

                public void remove() {
                    edges.remove();
                }
            }
        }

        /**
         * A view-based class over the edges in a {@link Subgraph}.  This class
         * masks the state of the edges that are in the backing graph but not in
         * the subgraph.
         */
        private class SubgraphEdgeView extends AbstractSet<DirectedTypedEdge<T>> {           

            public SubgraphEdgeView() { }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(DirectedTypedEdge<T> e) {
                return Subgraph.this.add(e);
            }

            public boolean contains(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;
                return Subgraph.this.contains(edge);
            }

            public Iterator<DirectedTypedEdge<T>> iterator() {
                return new SubgraphEdgeIterator();
            }

            /**
             * {@inheritDoc}
             */
            public boolean remove(Object o) {
                if (!(o instanceof DirectedTypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                DirectedTypedEdge<T> edge = (DirectedTypedEdge<T>)o;
                return Subgraph.this.remove(edge);
            }

            public int size() {
                return Subgraph.this.size();
            }

            /**
             * An iterator over the edges in this subgraph, which uses the
             * pair-wise combinations of edges to identify which edges should be
             * returned.
             */
            private class SubgraphEdgeIterator 
                    implements Iterator<DirectedTypedEdge<T>> {                

                private final Queue<DirectedTypedEdge<T>> edgesToReturn;

                private final Queue<Integer> remainingVertices;

                private Iterator<Integer> possibleNeighbors;

                private Integer curVertex;

                public SubgraphEdgeIterator() {
                    remainingVertices = 
                        new ArrayDeque<Integer>(Subgraph.this.vertices());
                    edgesToReturn = new ArrayDeque<DirectedTypedEdge<T>>();
                    curVertex = null;
                    advance();
                }
                
                private void advance() {
                    // If there are still edges from the current pair, skip
                    // processing any further connections
                    if (!edgesToReturn.isEmpty())
                        return;
                    
                    do {
                        // Find the next vertex to analyze for connections
                        while ((possibleNeighbors == null
                                    || !possibleNeighbors.hasNext())
                               && !remainingVertices.isEmpty()) {
                            // Pull off the next vertex and then use the remaining
                            // vertices to check for connections
                            curVertex = remainingVertices.poll();
                            possibleNeighbors = remainingVertices.iterator();
                        }
                        
                        // Iterate through the possible neighbors of this
                        // vertex, stopping when we find another vertex that has
                        // at least one edge
                        while (edgesToReturn.isEmpty()
                                   && possibleNeighbors.hasNext()) {
                            Integer v = possibleNeighbors.next();
                            edgesToReturn.addAll(getEdges(curVertex, v));
                        }
                    } while (edgesToReturn.isEmpty()
                             && !remainingVertices.isEmpty());
                }

                public boolean hasNext() {
                    return edgesToReturn.size() > 0;
                }

                public DirectedTypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    DirectedTypedEdge<T> n = edgesToReturn.poll();
                    advance();
                    return n;
                }

                public void remove() {
                    throw new IllegalStateException();
                }
            }
        }

        /**
         * A view of a subgraph's vertex's neighbors that are also in the
         * subview.  This view monitors for additions and removals to the set in
         * order to update the state of this {@code Subgraph}.
         */
        private class SubgraphNeighborsView extends AbstractIntSet {

            private int root;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphNeighborsView(int root) {
                this.root = root;
            }

            public boolean add(int vertex) {
                throw new UnsupportedOperationException(
                    "Cannot add vertices to subgraph");
            }
            
            public boolean add(Integer vertex) {
                throw new UnsupportedOperationException(
                    "Cannot add vertices to subgraph");
            }

            public boolean contains(int vertex) {
                return vertexSubset.contains(vertex) 
                    && isNeighboringVertex(vertex);
            }
            
            public boolean contains(Object o) {
                if (!(o instanceof Integer))
                    return false;
                Integer i = (Integer)o;
                return vertexSubset.contains(i) && isNeighboringVertex(i);
            }
            
            private boolean isNeighboringVertex(Integer i) {
                return Subgraph.this.contains(root, i);
            }

            public IntIterator iterator() {
                return new SubgraphNeighborsIterator();
            }
            
            public boolean remove(int vertex) {
                throw new UnsupportedOperationException(
                    "Cannot remove vertices from subgraph");
            }
            
            public boolean remove(Object vertex) {
                throw new UnsupportedOperationException(
                    "Cannot remove vertices from subgraph");
            }
            
            public int size() {
                int sz = 0;
                for (Integer v : vertexSubset) {
                    if (isNeighboringVertex(v))
                        sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for a subgraphs's neighboring
             * vertices set, which keeps track of which neighboring vertices are
             * actually in this subview.
             */
            private class SubgraphNeighborsIterator implements IntIterator {

                private final Iterator<Integer> iter;

                private Integer next;

                public SubgraphNeighborsIterator() {
                    iter = vertexSubset.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subview.
                 */
                private void advance() {
                    next = null;
                    while (iter.hasNext() && next == null) {
                        Integer v = iter.next();
                        if (isNeighboringVertex(v))
                            next = v;
                    }                    
                }

                public boolean hasNext() {
                    return next != null;
                }

                public Integer next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Integer cur = next;
                    advance();
                    return cur;
                }

                public int nextInt() {
                    return next();
                }

                /**
                 * Throws an {@link UnsupportedOperationException} if called.
                 */
                public void remove() {
                    throw new UnsupportedOperationException();
                }            
            }
        }
    }
}
