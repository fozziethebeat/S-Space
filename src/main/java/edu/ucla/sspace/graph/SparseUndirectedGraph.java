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

import java.util.Set;


/**
 * An undirected implementation of {@link Graph} that uses a sparse backing
 * representation.  This implementation assumes that the total number of edges
 * is less than the {@code n}<sup>2</sup> possible, where {@code n} is the
 * number of vertices.
 *
 * <p> This class supports all optional {@link Graph}.  All returned {@link
 * Edge}-based collections will reflect the state of the graph and may be
 * modified to change the graph; i.e., adding an edge to the adjacency list of a
 * vertex or edge list of the graph will add that edge in the backing graph.
 * Adding vertices by adding to the {@link #vertices()} set is supported.
 * Adding edges by adding a vertex to the set of adjacent vertex is <i>not</i>
 * supported.
 *
 * @author David Jurgens
 */
public class SparseUndirectedGraph extends AbstractGraph<Edge,SparseUndirectedEdgeSet> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty undirected graph
     */
    public SparseUndirectedGraph() { }

    /**
     * Creates an undirected graph with no edges and the provided set of vertices
     */
    public SparseUndirectedGraph(Set<Integer> vertices) {
        super(vertices);
    }

    /**
     * Creates an undirected graph with a copy of all the vertices and edges in
     * {@code g}.
     */
    public SparseUndirectedGraph(Graph<? extends Edge> g) {
        for (Integer v : g.vertices())
            add(v);
        for (Edge e : g.edges())
            add(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override public Graph<Edge> copy(Set<Integer> vertices) {
        // special case for If the called is requesting a copy of the entire
        // graph, which is more easily handled with the copy constructor
        if (vertices.size() == order() && vertices.equals(vertices()))
            return new SparseUndirectedGraph(this);
        SparseUndirectedGraph g = new SparseUndirectedGraph();
        for (int v : vertices) {
            if (!contains(v))
                throw new IllegalArgumentException(
                    "Requested copy with non-existant vertex: " + v);
            g.add(v);
            for (Edge e : getAdjacencyList(v))
                if (vertices.contains(e.from()) && vertices.contains(e.to()))
                    g.add(e);
        }
        return g;
    }
    
    /**
     * Creates a sparse edge set that treats all edges as symmetric.
     */
    @Override protected SparseUndirectedEdgeSet createEdgeSet(int vertex) {
        return new SparseUndirectedEdgeSet(vertex);
    }
}
