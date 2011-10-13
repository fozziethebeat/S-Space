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

import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.ObjectIndexer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * A decorator around all graph types that allows vertices to take on arbitrary
 * labels.  This class does not directly implement {@link Graph} but rather
 * exposes the backing graph through the {@link #graph()} method, which ensures
 * that the backing graph is of the appropriate type (or subtype).
 *
 * <p> The view returned by {@link #graph()} is read-only with respect to
 * vertices (i.e., edges may be added or removed).  This ensures that all vertex
 * additions or removals to the graph are made through this class.
 */
public class LabeledGraph<L,E extends Edge> 
        extends GraphAdaptor<E> implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
    private final Graph<E> graph;

    private final Indexer<L> vertexLabels;

    public LabeledGraph(Graph<E> graph) {
        this(graph, new ObjectIndexer<L>());
    }

    public LabeledGraph(Graph<E> graph, Indexer<L> vertexLabels) {
        super(graph);
        this.graph = graph;
        this.vertexLabels = vertexLabels;
    }

    public boolean add(L vertexLabel) {
        return add(vertexLabels.index(vertexLabel));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if adding a vertex that has not
     *         previously been assigned a label
     */
    public boolean add(int vertex) {
        if (vertexLabels.lookup(vertex) == null)
            throw new IllegalArgumentException("Cannot add a vertex without a label");
        // The indexer may have already had the mapping without the graph having
        // vertex, so check that the graph has the vertex
        return super.add(vertex);
    }

    /**
     * {@inheritDoc}
     */
    @Override public LabeledGraph<L,E> copy(Set<Integer> vertices) {
        Graph<E> g = super.copy(vertices);
        // Create a copy of the labels.  
        // NOTE: this includes labels for vertices that may not be present in
        //       the new graph.  Not sure if it's the correct behavior yet.
        Indexer<L> labels = new ObjectIndexer<L>(vertexLabels);
        return new LabeledGraph<L,E>(g, labels);
    }

    public boolean contains(L vertexLabel) {
        return contains(vertexLabels.index(vertexLabel));
    }

    public boolean remove(L vertexLabel) {
        return remove(vertexLabels.index(vertexLabel));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(order() * 4 + size() * 10);
        sb.append("vertices: [");
        for (int v : vertices()) {
            sb.append(vertexLabels.lookup(v)).append(',');
        }
        sb.setCharAt(sb.length() - 1, ']');
        sb.append(" edges: [");
        for (E e : edges()) {
            L from = vertexLabels.lookup(e.from());
            L to = vertexLabels.lookup(e.to());
            String edge = (e instanceof DirectedEdge) ? "->" : "--";
            sb.append('(').append(from).append(edge).append(to);
            if (e instanceof TypedEdge) {
                TypedEdge<?> t = (TypedEdge<?>)e;
                sb.append(':').append(t.edgeType());
            }
            if (e instanceof WeightedEdge) {
                WeightedEdge w = (WeightedEdge)e;
                sb.append(", ").append(w.weight());
            }
            sb.append("), ");
        }
        sb.setCharAt(sb.length() - 2, ']');
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
    
}