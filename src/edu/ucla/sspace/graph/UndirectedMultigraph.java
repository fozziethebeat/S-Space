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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.IntSet;
import edu.ucla.sspace.util.OpenIntSet;


/**
 *
 *
 * @param T a class type whose values are used to distinguish between edge types
 * 
 * @author David Jurgens
 */
public class UndirectedMultigraph<T> 
        implements Multigraph<T,TypedEdge<T>>, java.io.Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * A mapping from an edge type to the graph that contains all edges with
     * that type.
     */
    private final Map<T,Graph<TypedEdge<T>>> typeToEdges;

    // private final IntSet vertices;
    private final Set<Integer> vertices;

    /**
     * An interal view of the total set of edges across each of the
     * type-specific graphs
     */
    private EdgeView edges;

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
     * Creates an empty graph with node edges
     */
    public UndirectedMultigraph() { 
        typeToEdges = new HashMap<T,Graph<TypedEdge<T>>>();
        // vertices = new IntSet();
        vertices = new HashSet<Integer>();
        edges = new EdgeView();
        subgraphs = new ArrayList<WeakReference<Subgraph>>();
    }

    /**
     * Creates a directed multigraph with a copy of all the vertices and edges
     * in {@code g}.
     */
    public UndirectedMultigraph(Graph<? extends TypedEdge<T>> g) {
        this();
        for (Integer v : g.vertices())
            add(v);
        for (TypedEdge<T> e : g.edges())
            add(e);
    }    

    /**
     * {@inheritDoc}
     */
    public boolean add(int vertex) {
        return vertices.add(vertex);
    }

    public boolean add(TypedEdge<T> e) {
        Graph<TypedEdge<T>> g = typeToEdges.get(e.edgeType());
        if (g == null) {
            g = new UndirectedTypedGraph<T>();
            typeToEdges.put(e.edgeType(), g);
        }
        // If we've added a new edge, update the local state for the vertices
        // and edge types
         if (g.add(e)) { 
             vertices.add(e.from());
             vertices.add(e.to());             
             return true;
         }
         return false;
    }

    /**
     * InheritDoc
     */
    public void clear() {
        typeToEdges.clear();
        vertices.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() { 
        typeToEdges.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges(T edgeType) { 
        typeToEdges.remove(edgeType);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex) {
        return vertices.contains(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Edge e) {
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            if (g.contains(e))
                return true;
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2) {
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            if (g.contains(vertex1, vertex2))
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2, T edgeType) {
        Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
        if (g != null)
            return g.contains(vertex1, vertex2);
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> copy(Set<Integer> vertices) {
        // special case for If the called is requesting a copy of the entire
        // graph, which is more easily handled with the copy constructor
        if (vertices.size() == order() && vertices.equals(vertices())) {
            Multigraph<T,TypedEdge<T>> g = new UndirectedMultigraph<T>(this);
            assert g.size() == size() : "missing vertices";
            assert g.order() == order() : "missing edges";
            return g;
        }
        UndirectedMultigraph<T> g = new UndirectedMultigraph<T>();
        int order = order();
        int avgDegree = (order > 0) ? size() / order : 0;
        boolean useAdjacencyList = vertices.size() < avgDegree;
        for (int v : vertices) {
            if (!contains(v))
                throw new IllegalArgumentException(
                    "Requested copy with non-existant vertex: " + v);
            g.add(v);
            // If the number of vertices being requested is expected to be
            // lareger than the average degree, we should see fewer edges than
            // the number of requested vertices squared.  Therefore, use the
            // adjacency list
            if (useAdjacencyList) {
                for (Graph<TypedEdge<T>> g2 : typeToEdges.values()) {
                    Set<TypedEdge<T>> adj = g2.getAdjacencyList(v);
                    for (TypedEdge<T> e : adj) {
                        if (vertices.contains(e.from()) 
                                && vertices.contains(e.to()))
                            g.add(e);
                    }
                }
            }
            // If the number of requested vertices is small, then just try all
            // pairwise comparisons, as the adjacency lists would likely contain
            // many edges to vertices not in this subgraph
            else {
                for (int v2 : vertices) {
                    if (v == v2)
                        break;
                    // Iterate through the type-specific graphs, checking for
                    // edges between these two vertices.  This avoid creating a
                    // combined set.
                    for (Graph<TypedEdge<T>> g2 : typeToEdges.values()) {
                        for (TypedEdge<T> e : g2.getEdges(v, v2))
                            g.add(e);
                    }
                }
            }
        }
        assert g.size() == size() : "missing vertices";
        assert g.order() == order() : "missing edges";
        return g;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> copy(Set<Integer> vertices, T type) {
        throw new Error("to do");
    }

    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        int degree = 0;
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            degree += g.degree(vertex);
        return degree;
    }

    /**
     * Returns the set of typed edges in the graph
     */
    public Set<TypedEdge<T>> edges() {
        return edges;
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> edges(T t) {
        Graph<TypedEdge<T>> g = typeToEdges.get(t);
        return (g == null)
            ? Collections.<TypedEdge<T>>emptySet()
            : g.edges();
    }

    /**
     * Returns the set of edge types currently present in this graph.
     */
    public Set<T> edgeTypes() {
        return Collections.unmodifiableSet(typeToEdges.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override public boolean equals(Object o) {
        if (o instanceof Multigraph) {
            @SuppressWarnings("unchecked")
            Multigraph<?,TypedEdge<?>> g = (Multigraph<?,TypedEdge<?>>)o;
            return g.edgeTypes().equals(typeToEdges.keySet())
                && g.order() == order()
                && g.size() == size()
                && g.vertices().equals(vertices())
                && g.edges().equals(edges());                    
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getAdjacencyList(int vertex) {
        List<Set<TypedEdge<T>>> sets = 
            new ArrayList<Set<TypedEdge<T>>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<TypedEdge<T>> adj = g.getAdjacencyList(vertex);
            if (!adj.isEmpty())
                sets.add(adj);
        }
        return (sets.isEmpty())
            ? Collections.<TypedEdge<T>>emptySet()
            : new CombinedSet<TypedEdge<T>>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex1, int vertex2) {
        List<Set<TypedEdge<T>>> sets = 
            new ArrayList<Set<TypedEdge<T>>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<TypedEdge<T>> e = g.getEdges(vertex1, vertex2);
            if (!e.isEmpty()) {
                sets.add(e);
            }
        }
        return (sets.isEmpty()) ? Collections.<TypedEdge<T>>emptySet() : 
            new CombinedSet<TypedEdge<T>>(sets);
    }


    /**
     * {@inheritDoc}
     */
    public Set<TypedEdge<T>> getEdges(int vertex1, int vertex2, T t) {
        Graph<TypedEdge<T>> g = typeToEdges.get(t);
        return (g == null) 
            ? Collections.<TypedEdge<T>>emptySet()
            : g.getEdges(vertex1, vertex2);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        for (Graph<TypedEdge<T>> g : typeToEdges.values()) {
            Set<Integer> n = g.getNeighbors(vertex);
            if (n != null)
                sets.add(n);
        }
        return (sets.isEmpty()) 
            ? Collections.<Integer>emptySet() : new CombinedSet<Integer>(sets);
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
        return vertices().hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public int order() {
        return vertices.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        // If we can remove the vertex from the global set, then at least one of
        // the type-specific graphs has this vertex.
        if (vertices.remove(vertex)) {
            Iterator<Map.Entry<T,Graph<TypedEdge<T>>>> it = 
                typeToEdges.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<T,Graph<TypedEdge<T>>> e = it.next();
                Graph<TypedEdge<T>> g = e.getValue();
                // Check whether removing this vertex has caused us to remove
                // the last edge for this type in the graph.  If so, the graph
                // no longer has this type and we need to update the state.
                if (g.remove(vertex) && g.size() == 0) {
                    // Get rid of the type mapping
                    it.remove(); 
                }
            }
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
                    Iterator<T> types = s.validTypes.iterator();
                    while (types.hasNext()) {
                        if (!typeToEdges.containsKey(types.next()))
                            types.remove();
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
    public boolean remove(TypedEdge<T> edge) {
        Iterator<Map.Entry<T,Graph<TypedEdge<T>>>> it = 
            typeToEdges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<T,Graph<TypedEdge<T>>> e = it.next();
            Graph<TypedEdge<T>> g = e.getValue();
            if (g.remove(edge)) {
                // Check whether we've just removed the last edge for this type
                // in the graph.  If so, the graph no longer has this type and
                // we need to update the state.
                if (g.size() == 0) {
                    // Get rid of the type mapping
                    it.remove(); 

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
                        s.validTypes.remove(e.getKey());
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        int size = 0;
        for (Graph<TypedEdge<T>> g : typeToEdges.values())
            size += g.size();
        return size;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> subset) {
         Subgraph sub = new Subgraph(typeToEdges.keySet(), subset);
         subgraphs.add(new WeakReference<Subgraph>(sub));
         return sub;
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> subset, 
                                               Set<T> edgeTypes) {
        if (edgeTypes.isEmpty()) 
           throw new IllegalArgumentException("Must specify at least one type");
        if (!typeToEdges.keySet().containsAll(edgeTypes)) {
            throw new IllegalArgumentException(
                "Cannot create subgraph with more types than exist");
        }
        Subgraph sub = new Subgraph(edgeTypes, subset);
        if (sub.size() == 0) {
            throw new Error(sub +  " : " + edgeTypes);
        }
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
     * {@inheritDoc}
     */
    public Set<Integer> vertices() {
        return Collections.unmodifiableSet(vertices);
    }

    /**
     * A graph that imposes no restriction on the types of edges that may connect
     * its vertices.
     */
    class UndirectedTypedGraph<T> extends AbstractGraph<TypedEdge<T>,
                                                SparseTypedEdgeSet<T>> {
        
        private static final long serialVersionUID = 1L;
        
        public UndirectedTypedGraph() { }

        /**
         * {@inheritDoc}
         */
        @Override public Graph<TypedEdge<T>> 
                copy(Set<Integer> vertices) {
            // special case for If the called is requesting a copy of the entire
            // graph, which is more easily handled with the copy constructor
            UndirectedTypedGraph<T> g = new UndirectedTypedGraph<T>();
            if (vertices.size() == order() && vertices.equals(vertices())) {
                for (int v : vertices)
                    g.add(v);
                for (TypedEdge<T> e : edges())
                    g.add(e);
            }
            else {
                for (int v : vertices) {
                    if (!contains(v))
                        throw new IllegalArgumentException(
                            "Requested copy with non-existant vertex: " + v);
                    g.add(v);
                    for (TypedEdge<T> e : getAdjacencyList(v))
                        if (vertices.contains(e.from())
                                && vertices.contains(e.to()))
                            g.add(e);
                }
            }
            return g;
        }
                
        @Override protected SparseTypedEdgeSet<T> 
                createEdgeSet(int vertex) {
            return new SparseTypedEdgeSet<T>(vertex);
        }        
    }

    /**
     * The {@code EdgeView} class encapsulates all the of the edge sets from the
     * different edge types.  Because {@link CombinedSet} does not allow for
     * addition, we add an {@code add} method that handles adding new edges to
     * the graph via this set.
     */
    class EdgeView extends AbstractSet<TypedEdge<T>> {

        public EdgeView() {
        }

        public boolean add(TypedEdge<T> e) {
            // Calling the backing graph's add will route the edge to the
            // type-appropriate set
            return UndirectedMultigraph.this.add(e);
        }

        public boolean contains(Object o) {
            return (o instanceof TypedEdge) 
                && UndirectedMultigraph.this.contains((TypedEdge<?>)o);
        }

        public Iterator<TypedEdge<T>> iterator() {
            List<Iterator<TypedEdge<T>>> iters = 
                new ArrayList<Iterator<TypedEdge<T>>>(typeToEdges.size());
            for (Graph<TypedEdge<T>> g : typeToEdges.values())
                iters.add(g.edges().iterator());
            return new CombinedIterator<TypedEdge<T>>(iters);
        }
        
        @SuppressWarnings("unchecked")
        public boolean remove(Object o) {
            if (o instanceof TypedEdge) {
                TypedEdge<?> e = (TypedEdge<?>)o;
                return UndirectedMultigraph.this.typeToEdges.
                           containsKey(e.edgeType())
                    && UndirectedMultigraph.this.remove((TypedEdge<T>)o);
            }
            return false;
        }

        public int size() {
            return UndirectedMultigraph.this.size();
        }
    }

    /**
     * An implementation for handling the subgraph behavior.
     */
     class Subgraph extends UndirectedMultigraph<T> {

        private static final long serialVersionUID = 1L;

        /**
         * The set of types in this subgraph
         */
        private final Set<T> validTypes;

        /**
         * The set of vertices in this subgraph
         */
        private final Set<Integer> vertexSubset;

        public Subgraph(Set<T> validTypes, Set<Integer> vertexSubset) {
            this.validTypes = validTypes;
            this.vertexSubset = new OpenIntSet(vertexSubset);
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
        public boolean add(TypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                throw new UnsupportedOperationException(
                    "Cannot add new vertex to subgraph");
            return UndirectedMultigraph.this.add(e);
        }

        /**
         * {@inheritDoc}
         */        
        public void clear() {
            for (Integer v : vertexSubset)
                UndirectedMultigraph.this.remove(v);
            vertexSubset.clear();
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges() { 
            for (Integer v : vertexSubset) {
                // Only consider removing edges of the valid types
                for (T type : validTypes) {
                    // Get all the edges for the current vertex for that type
                    Graph<TypedEdge<T>> g = typeToEdges.get(type);
                    // Check whether each of the adjacent edges points to
                    // another vertex in this subgraph.  If so, remove it
                    for (TypedEdge<T> e : g.getAdjacencyList(v)) {
                        int from = e.from();
                        int to = e.to();
                        // Check the other vertex to be in this subgraph
                        if ((from == v && vertexSubset.contains(to))
                                || (to == v && vertexSubset.contains(from)))
                            g.remove(e);
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clearEdges(T edgeType) { 
            // Get all the edges for the current vertex for the type
            Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
            for (Integer v : vertexSubset) {
                // Check whether each of the adjacent edges points to another vertex
                // in this subgraph.  If so, remove it
                for (TypedEdge<T> e : g.getAdjacencyList(v)) {
                    int from = e.from();
                    int to = e.to();
                    // Check the other vertex to be in this subgraph
                    if ((from == v && vertexSubset.contains(to))
                            || (to == v && vertexSubset.contains(from)))
                        g.remove(e);
                }
            }
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
            if (e instanceof TypedEdge) {
                TypedEdge<?> te = (TypedEdge<?>)e;
                return vertexSubset.contains(e.from())
                    && vertexSubset.contains(e.to())
                    && validTypes.contains(te.edgeType())
                    && UndirectedMultigraph.this.contains(e);
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

            Set<TypedEdge<T>> edges = 
                UndirectedMultigraph.this.getEdges(vertex1, vertex2);
            if (edges != null) {
                // If there were edges between the two vertices, ensure that at
                // least one of them has the type represented in this subgraph
                for (TypedEdge<T> e : edges)
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

            Graph<TypedEdge<T>> g = typeToEdges.get(edgeType);
            return g.contains(vertex1, vertex2);
        }

        /**
         * {@inheritDoc}
         */
        public int degree(int vertex) {
            int degree = 0;
            for (T type : validTypes) {
                Graph<TypedEdge<T>> g = typeToEdges.get(type);
                degree += g.degree(vertex);
            }
            return degree;
        }

        /**
         * Returns the set of typed edges in the graph
         */
        public Set<TypedEdge<T>> edges() {
            return new SubgraphEdgeView();
        }

        /**
         * {@inheritDoc}
         */
        public Set<TypedEdge<T>> edges(T t) {
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
        public Set<TypedEdge<T>> getAdjacencyList(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<TypedEdge<T>>emptySet();
            Set<TypedEdge<T>> adj = 
                UndirectedMultigraph.this.getAdjacencyList(vertex);
            return (adj.isEmpty()) 
                ? Collections.<TypedEdge<T>>emptySet()
                : new SubgraphAdjacencyListView(vertex, adj);
        }

        /**
         * {@inheritDoc}
         */
        public Set<TypedEdge<T>> getEdges(int vertex1, int vertex2) {
            if (!vertexSubset.contains(vertex1) 
                    || !vertexSubset.contains(vertex2))
                return Collections.<TypedEdge<T>>emptySet();

            List<Set<TypedEdge<T>>> sets = new ArrayList<Set<TypedEdge<T>>>();
            for (T type : validTypes) {
                Set<TypedEdge<T>> edges = 
                    UndirectedMultigraph.this.getEdges(vertex1, vertex2, type);
                if (!edges.isEmpty())
                    sets.add(edges);
            }
            return (sets.isEmpty()) 
                ? Collections.<TypedEdge<T>>emptySet() 
                : new CombinedSet<TypedEdge<T>>(sets);
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> getNeighbors(int vertex) {
            if (!vertexSubset.contains(vertex))
                return Collections.<Integer>emptySet();
            Set<Integer> neighbors = 
                UndirectedMultigraph.this.getNeighbors(vertex);
            // If the set of neighbors is larger than the number of vertices in
            // this subgraph, then just use the set of vertices
            return new SubgraphNeighborsView(vertex, vertexSubset);
            // (neighbors.size() > vertexSubset.size())
            //                 ? new SubgraphNeighborsView(vertex, vertexSubset)
            //                 : new SubgraphNeighborsView(vertex, neighbors);
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasCycles() {
            throw new Error();
        }

        /**
         * Returns {@code true} if the two vertices are contained within this
         * subgraph and have an edge that is valid within this subgraph's type
         * constraints.
         */
        private boolean hasEdge(int v1, int v2) {
            if (vertexSubset.contains(v1) && vertexSubset.contains(v2)) {
                Set<TypedEdge<T>> edges = getEdges(v1, v2);
                return edges != null;
            }
            return false;            
        }

        /**
         * {@inheritDoc}
         */
        @Override public int hashCode() {
            return vertices().hashCode();
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
        public boolean remove(int vertex) {
            throw new UnsupportedOperationException(
                "Cannot remove vertices from a subgraph");
        }

        /**
         * {@inheritDoc}
         */
        public boolean remove(TypedEdge<T> e) {
            if (!vertexSubset.contains(e.from())
                    || !vertexSubset.contains(e.to())
                    || !validTypes.contains(e.edgeType()))
                return false;
            return UndirectedMultigraph.this.remove(e);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            int size = 0;
            for (TypedEdge<T> e : edges()) {
                size++;
            }
            return size;
        }

        /**
         * {@inheritDoc}
         */
        public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> verts) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            return UndirectedMultigraph.this.subgraph(verts, validTypes);
        }

        /**
         * {@inheritDoc}
         */
        public Multigraph<T,TypedEdge<T>> subgraph(Set<Integer> verts, 
                                                   Set<T> edgeTypes) {
            if (!vertexSubset.containsAll(verts)) 
                throw new IllegalArgumentException("provided set is not a " +
                    "subset of the vertices of this graph");
            if (!validTypes.containsAll(edgeTypes))
                throw new IllegalArgumentException("provided types is not a " +
                    "subset of the edge types of this graph");
            return UndirectedMultigraph.this.subgraph(verts, edgeTypes);
        }
     
        /**
         * Returns a description of the graph as the sequence of its edges.
         */
        public String toString() {
            // REMINDER: make this more efficient with a StringBuilder
            return "{ vertices: " + vertices() + ", edges: " + edges() + "}";
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            // Check that the vertices are up to date with the backing graph
            return Collections.unmodifiableSet(vertexSubset);
        }

        /**
         * A view for the {@code Edge} adjacent edges of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView 
                extends AbstractSet<TypedEdge<T>> {

            /**
             * The adjacency list of edges in the backing graph.
             */
            private final Set<TypedEdge<T>> adjacencyList;
            
            /**
             * The root vertex in the subgraph.
             */ 
            private final int root;

            public SubgraphAdjacencyListView(int root,
                    Set<TypedEdge<T>> adjacencyList) {
                this.root = root;
                this.adjacencyList = adjacencyList;                
            }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(TypedEdge<T> edge) {
                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.add(edge);
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                Edge e = (Edge)o;
                return vertexSubset.contains(e.to()) 
                    && vertexSubset.contains(e.from())
                    && adjacencyList.contains(e);
            }
            
            public Iterator<TypedEdge<T>> iterator() {
                return new SubgraphAdjacencyListIterator();
            }
            
            /**
             * Removes the edge, if present in this subgraph
             */
            public boolean remove(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;

                return (edge.from() == root 
                        || edge.to() == root) 
                    && Subgraph.this.remove(edge);
            }

            public int size() {
                int sz = 0;
                Iterator<TypedEdge<T>> it = iterator();
                while (it.hasNext()) {
                    it.next();
                    sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class SubgraphAdjacencyListIterator 
                    implements Iterator<TypedEdge<T>> {
                
                private final Iterator<TypedEdge<T>> edges;
                
                private TypedEdge<T> next;
                private TypedEdge<T> cur;

                public SubgraphAdjacencyListIterator() {
                    edges = adjacencyList.iterator();
                    advance();
                }
                
                private void advance() {
                    next = null;
                    while (edges.hasNext()) {
                        TypedEdge<T> e = edges.next();

                        // Skip edges between vertices not in this subgraph
                        if (!vertexSubset.contains(e.from()) 
                                || !vertexSubset.contains(e.to())
                                || !validTypes.contains(e.edgeType()))
                            continue;

                        next = e;
                        break;
                    }
                }
                
                public boolean hasNext() {
                    return next != null;
                }
                
                public TypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    cur = next;
                    advance();
                    return cur;
                }
                
                /**
                 * {@inheritDoc}
                 */                
                public void remove() {
                    if (null == null)
                        throw new IllegalStateException();
                    Subgraph.this.remove(cur);
                    cur = null;
                }            
            }
        }

        /**
         * A view-based class over the edges in a {@link Subgraph}.  This class
         * masks the state of the edges that are in the backing graph but not in
         * the subgraph.
         */
        private class SubgraphEdgeView extends AbstractSet<TypedEdge<T>> {           

            public SubgraphEdgeView() { }

            /**
             * Throws an {@link UnsupportedOperationException} if called.
             */
            public boolean add(TypedEdge<T> e) {
                return Subgraph.this.add(e);
            }

            public boolean contains(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;
                return Subgraph.this.contains(edge);
            }

            public Iterator<TypedEdge<T>> iterator() {
                return new SubgraphEdgeIterator();
            }

            /**
             * {@inheritDoc}
             */
            public boolean remove(Object o) {
                if (!(o instanceof TypedEdge))
                    return false;
                @SuppressWarnings("unchecked")
                TypedEdge<T> edge = (TypedEdge<T>)o;
                return Subgraph.this.remove(edge);
            }

            public int size() {
                return Subgraph.this.size();
            }

            private class SubgraphEdgeIterator implements Iterator<TypedEdge<T>> {

                private final Queue<TypedEdge<T>> edgesToReturn;

                private final Queue<Integer> remainingVertices;
                
                private Iterator<Integer> possibleNeighbors;
                
                private Integer curVertex;

                public SubgraphEdgeIterator() {
                    remainingVertices = 
                        new ArrayDeque<Integer>(Subgraph.this.vertices());
                    edgesToReturn = new ArrayDeque<TypedEdge<T>>();
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

                public TypedEdge<T> next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    TypedEdge<T> n = edgesToReturn.poll();
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
        private class SubgraphNeighborsView extends AbstractSet<Integer> {

            /**
             * The set of adjacent vertices to a vertex.  This set is itself a view
             * to the data and is updated by the {@link EdgeList} for a vertex.
             */
            private Set<Integer> allPossibleNeighbors;

            private int root;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphNeighborsView(int root, Set<Integer> adjacent) {
                this.root = root;
                this.allPossibleNeighbors = adjacent;
            }
            
            /**
             * Adds an edge to this vertex and adds the vertex to the graph if it
             * was not present before.
             */
            public boolean add(Integer vertex) {
                throw new UnsupportedOperationException(
                    "Cannot add vertices to subgraph");
            }
            
            public boolean contains(Object o) {
                if (!(o instanceof Integer))
                    return false;
                Integer i = (Integer)o;
                return checkVertex(i);
            }
            
            private boolean checkVertex(Integer i) {
                return allPossibleNeighbors.contains(i)
                    && !getEdges(root, i).isEmpty();
            }

            public Iterator<Integer> iterator() {
                return new SubgraphNeighborsIterator();
            }
            
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }
            
            public int size() {
                int sz = 0;
                for (Integer v : allPossibleNeighbors) {
                    if (checkVertex(v))
                        sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for a subgraphs's neighboring
             * vertices set, which keeps track of which neighboring vertices are
             * actually in this subview.
             */
            private class SubgraphNeighborsIterator implements Iterator<Integer> {

                private final Iterator<Integer> iter;

                private Integer next;

                public SubgraphNeighborsIterator() {
                    iter = allPossibleNeighbors.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subview.
                 */
                private void advance() {
                    next = null;
                    while (iter.hasNext() && next == null) {
                        Integer v = iter.next();
                        if (checkVertex(v))
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
