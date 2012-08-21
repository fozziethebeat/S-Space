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
public class SparseWeightedGraph extends AbstractGraph<WeightedEdge,SparseWeightedEdgeSet>
        implements WeightedGraph<WeightedEdge> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty directed graph
     */
    public SparseWeightedGraph() { }

    /**
     * Creates a directed graph with the provided vertices
     */
    public SparseWeightedGraph(Set<Integer> vertices) {
        super(vertices);
    }

    /**
     * Creates a directed graph with a copy of all the vertices and edges in
     * {@code g}.
     */
    public SparseWeightedGraph(Graph<? extends WeightedEdge> g) {
        for (Integer v : g.vertices())
            add(v);
        for (WeightedEdge e : g.edges())
            add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override public WeightedGraph<WeightedEdge> copy(Set<Integer> toCopy) {
        // Special case for if the caller is requesting a copy of the entire
        // graph, which is more easily handled with the copy constructor
        if (toCopy.size() == order() && toCopy.equals(vertices()))
            return new SparseWeightedGraph(this);
        SparseWeightedGraph g = new SparseWeightedGraph();
        for (int v : toCopy) {
            if (!contains(v))
                throw new IllegalArgumentException(
                    "Requested copy with non-existant vertex: " + v);
            g.add(v);
            SparseWeightedEdgeSet edges = getEdgeSet(v);
            for (int v2 : toCopy) {
                if (v == v2)
                    break;
                if (edges.connects(v2)) {
                    for (WeightedEdge e : edges.getEdges(v2))
                        g.add(e);
                }
            }
//             for (WeightedEdge e : getAdjacencyList(v))
//                 if (toCopy.contains(e.from()) && toCopy.contains(e.to()))
//                     g.add(e);
        }
        return g;
    }
    
    /**
     * Creates an {@link EdgeSet} for storing {@link WeightedEdge} instances for
     * the specified vertex.
     */
    @Override protected SparseWeightedEdgeSet createEdgeSet(int vertex) {
        return new SparseWeightedEdgeSet(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public double strength(int vertex) {
        SparseWeightedEdgeSet e = getEdgeSet(vertex);
        return (e == null) ? 0 : e.sum();
    }

    /**
     * {@inheritDoc}
     */
    public WeightedGraph subgraph(Set<Integer> vertices) {
        // Get a subgraph from the parent graph (which is just a Graph) and then
        // wrap it with an adaptor to fulfill the WeightedGraph API
        Graph<WeightedEdge> g = super.subgraph(vertices);
        return new SubgraphAdaptor(g);
    }

    /**
     * A decorator over the {@link Graph} returned by {@link #subgraph(Set)}
     * that extends the functionality to support the {@link WeightedGraph}
     * interface.
     */ 
    private class SubgraphAdaptor extends GraphAdaptor<WeightedEdge> 
            implements WeightedGraph<WeightedEdge>, java.io.Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Constructs a new adaptor over the provided subgraph.
         */
        public SubgraphAdaptor(Graph<WeightedEdge> subgraph) {
            super(subgraph);
        }

        /**
         * {@inheritDoc}
         */
        @Override public WeightedGraph<WeightedEdge> copy(Set<Integer> vertices) {
            // Special case for if the caller is requesting a copy of the entire
            // graph, which is more easily handled with the copy constructor
            if (vertices.size() == order() && vertices.equals(vertices()))
                return new SparseWeightedGraph(this);
            SparseWeightedGraph g = new SparseWeightedGraph();
            for (int v : vertices) {
                if (!contains(v))
                    throw new IllegalArgumentException(
                        "Requested copy with non-existant vertex: " + v);
                g.add(v);
                for (WeightedEdge e : getAdjacencyList(v))
                    if (vertices.contains(e.from())
                            && vertices.contains(e.to()))
                        g.add(e);
            }
            return g;
        }

        /**
         * {@inheritDoc}
         */
        public double strength(int vertex) {
            double sum = 0;
            for (WeightedEdge e : getAdjacencyList(vertex))
                sum += e.weight();
            return sum;
        }

        /**
         * {@inheritDoc}
         */
        public WeightedGraph subgraph(Set<Integer> vertices) {
            Graph<WeightedEdge> g = SparseWeightedGraph.this.subgraph(vertices);
            return new SubgraphAdaptor(g);
        }
    }
}
