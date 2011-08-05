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
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * An implementation of {@link DirectedGraph} that uses a sparse backing
 * representation.  This implementation assumes that the total number of edges
 * is less than the {@code n}<sup>2</sup> possible, where {@code n} is the
 * number of vertices.
 *
 * <p> This class supports all optional {@link Graph} and {@link DirectedGraph}
 * methods.  All returned {@link DirectedEdge}-based collections will reflect
 * the state of the graph and may be modified to change the graph; i.e., adding
 * an edge to the adjacency list of a vertex or edge list of the graph will add
 * that edge in the backing graph.  Adding vertices by adding to the {@link
 * #vertices()} set is supported.  Adding edges by adding a vertex to the set of
 * adjacent vertex is <i>not</i> supported.
 *
 * @author David Jurgens
 */
@SuppressWarnings("unchecked")
public class SparseDirectedGraph extends AbstractGraph<DirectedEdge,SparseDirectedEdgeSet>
        implements DirectedGraph<DirectedEdge> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty directed graph
     */
    public SparseDirectedGraph() { }

    /**
     * Creates a directed graph with the provided vertices
     */
    public SparseDirectedGraph(Set<Integer> vertices) {
        super(vertices);
    }

    /**
     * Creates a directed graph with a copy of all the vertices and edges in
     * {@code g}.
     */
    public SparseDirectedGraph(Graph<? extends DirectedEdge> g) {
        for (Integer v : g.vertices())
            add(v);
        for (DirectedEdge e : g.edges())
            add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override public DirectedGraph<DirectedEdge> copy(Set<Integer> vertices) {
        // special case for If the called is requesting a copy of the entire
        // graph, which is more easily handled with the copy constructor
        if (vertices.size() == order() && vertices.equals(vertices()))
            return new SparseDirectedGraph(this);
        SparseDirectedGraph g = new SparseDirectedGraph();
        for (int v : vertices) {
            if (!contains(v))
                throw new IllegalArgumentException(
                    "Requested copy with non-existant vertex: " + v);
            g.add(v);
            for (DirectedEdge e : getAdjacencyList(v))
                if (vertices.contains(e.from()) && vertices.contains(e.to()))
                    g.add(e);
        }
        return g;
    }
    
    /**
     * Creates an {@link EdgeSet} for storing {@link DirectedEdge} instances for
     * the specified vertex.
     */
    @Override protected SparseDirectedEdgeSet createEdgeSet(int vertex) {
        return new SparseDirectedEdgeSet(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public int inDegree(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? 0 : edges.inEdges().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedEdge> inEdges(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) 
            ? Collections.<DirectedEdge>emptySet()
            : new EdgeSetDecorator(edges.inEdges());
    }

    /**
     * {@inheritDoc}
     */
    public int outDegree(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) ? 0 : edges.outEdges().size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<DirectedEdge> outEdges(int vertex) {
        SparseDirectedEdgeSet edges = getEdgeSet(vertex);
        return (edges == null) 
            ? Collections.<DirectedEdge>emptySet() 
            : new EdgeSetDecorator(edges.outEdges());
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> predecessors(int vertex) {
        Set<Integer> preds = new HashSet<Integer>();
        for (DirectedEdge e : inEdges(vertex))
            preds.add(e.from());
        return preds;
    }

    /**
     * {@inheritDoc}
     */
    public DirectedGraph subgraph(Set<Integer> vertices) {
        Graph<DirectedEdge> subgraph = super.subgraph(vertices);
        return new SubgraphAdaptor(subgraph);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> successors(int vertex) {
        Set<Integer> succs = new HashSet<Integer>();
        for (DirectedEdge e : outEdges(vertex))
            succs.add(e.to());
        return succs;
    }

    /***
     * 
     */
    private class EdgeSetDecorator extends AbstractSet<DirectedEdge> {

        private final Set<DirectedEdge> edges;
        
        public EdgeSetDecorator(Set<DirectedEdge> edges) {
            this.edges = edges;
        }

        @Override public boolean add(DirectedEdge e) {
            // Rather than add the edge to the set directly, add it to the
            // graph, which will propagate the edge to the appropriate EdgeSet
            // instances.
            return add(e);
        }

        @Override public boolean contains(Object o) {
            return edges.contains(o);
        }

        @Override public Iterator<DirectedEdge> iterator() {
            return new EdgeSetIteratorDecorator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof DirectedEdge))
                return false;
            // Rather than removing the edge to the set directly, removing it
            // from the graph, which will remove the edge froma the appropriate
            // EdgeSet instances.
            return remove((DirectedEdge)o);
        }

        @Override public int size() {
            return edges.size();
        }

        private class EdgeSetIteratorDecorator 
                implements Iterator<DirectedEdge> {

            private final Iterator<DirectedEdge> iter;

            private boolean alreadyRemoved;

            public EdgeSetIteratorDecorator() {
                iter = edges.iterator();
                alreadyRemoved = true;
            }

            public boolean hasNext() {
                return iter.hasNext();
            }

            public DirectedEdge next() {
                alreadyRemoved = false;
                return iter.next();
            }

            public void remove() {
                // REMINDER: I think this method would be extremely problematic
                // to actually implement.  The call to iter.remove() would leave
                // the symmetric edge in place in the AbstractGraph, while
                // calling remove() would likely result in a concurrent
                // modification to the EdgeSet being iterated over, which may
                // have unpredictable results. Therefore, we just throw an
                // exception to indicate it's not supported until we can
                // identify a better implementation solution.  -david
                throw new UnsupportedOperationException();
            }
        }
    }

    /**
     * A decorator over the {@link Graph} returned by {@link #subgraph(Set)}
     * that extends the functionality to support the {@link DirectedGraph}
     * interface.
     */ 
    private class SubgraphAdaptor extends GraphAdaptor<DirectedEdge> 
            implements DirectedGraph<DirectedEdge>, java.io.Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new adaptor over the provided subgraph.
         */
        public SubgraphAdaptor(Graph<DirectedEdge> subgraph) {
            super(subgraph);
        }

        /**
         * {@inheritDoc}
         */
        @Override public DirectedGraph<DirectedEdge> copy(Set<Integer> vertices) {
            // special case for If the called is requesting a copy of the entire
            // graph, which is more easily handled with the copy constructor
            if (vertices.size() == order() && vertices.equals(vertices()))
                return new SparseDirectedGraph(this);
            SparseDirectedGraph g = new SparseDirectedGraph();
            for (int v : vertices) {
                if (!contains(v))
                    throw new IllegalArgumentException(
                        "Requested copy with non-existant vertex: " + v);
                g.add(v);
                for (DirectedEdge e : getAdjacencyList(v))
                    if (vertices.contains(e.from())
                            && vertices.contains(e.to()))
                        g.add(e);
            }
            return g;
        }

        /**
         * {@inheritDoc}
         */
        public int inDegree(int vertex) {
            int degree = 0;
            Set<DirectedEdge> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return 0;
            for (DirectedEdge e : edges) {
                if (e.to() == vertex)
                    degree++;
            }
            return degree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedEdge> inEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedEdge> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedEdge>emptySet();

            Set<DirectedEdge> in = new HashSet<DirectedEdge>();
            for (DirectedEdge e : edges) {
                if (e.to() == vertex)
                    in.add(e);
            }
            return in;
        }

        /**
         * {@inheritDoc}
         */
        public int outDegree(int vertex) {
            int degree = 0;
            Set<DirectedEdge> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return 0;
            for (DirectedEdge e : edges) {
                if (e.from() == vertex)
                    degree++;
            }
            return degree;
        }

        /**
         * {@inheritDoc}
         */
        public Set<DirectedEdge> outEdges(int vertex) {
            // REMINDER: this is probably best wrapped with yet another
            // decorator class to avoid the O(n) penality of iteration over all
            // the edges
            Set<DirectedEdge> edges = getAdjacencyList(vertex);
            if (edges.isEmpty())
                return Collections.<DirectedEdge>emptySet();
            Set<DirectedEdge> out = new HashSet<DirectedEdge>();
            for (DirectedEdge e : edges) {
                if (e.from() == vertex)
                    out.add(e);
            }
            return out;
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> predecessors(int vertex) {
            Set<Integer> preds = new HashSet<Integer>();
            for (DirectedEdge e : inEdges(vertex))
                preds.add(e.from());
            return preds;
        }

        /**
         * {@inheritDoc}
         */
        public Set<Integer> successors(int vertex) {
            Set<Integer> succs = new HashSet<Integer>();
            for (DirectedEdge e : outEdges(vertex))
                succs.add(e.to());
            return succs;
        }

        /**
         * {@inheritDoc}
         */
        public DirectedGraph subgraph(Set<Integer> vertices) {
            Graph<DirectedEdge> g = super.subgraph(vertices);
            return new SubgraphAdaptor(g);
        }
    }
}
