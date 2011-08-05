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
 * An abstract adaptor class that wraps an existing {@link Graph}.  All methods
 * are pass through to the backing graph.  Extend this class to provide custom
 * functionality for specific methods for a backing graph, or to represents the
 * graph's data using a different interface (e.g. wrapping a {@code Graph} with
 * an extension that implements {@link DirectedGraph}).
 *
 * @author David Jurgens
 */
public abstract class GraphAdaptor<T extends Edge> implements Graph<T> {

    /**
     * The backing {@link Graph} instance to which all method calls will be
     * made.
     */
    private final Graph<T> g;

    public GraphAdaptor(Graph<T> g) {
        if (g == null)
            throw new NullPointerException("Cannot wrap null graph");
        this.g = g;
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(int i) {
        return g.add(i);
    }

    /**
     * {@inheritDoc}
     */
    public boolean add(T edge) {
        return g.add(edge);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        g.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() {
        g.clearEdges();
    }

    /**
     * {@inheritDoc}
     */
    public Graph<T> copy(Set<Integer> vertices) {
        return g.copy(vertices);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex) {
        return g.contains(vertex);
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean contains(int from, int to) {
        return g.contains(from, to);
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(Edge e) {
        return g.contains(e);
    }

    /**
     * {@inheritDoc}
     */
    public int degree(int vertex) {
        return g.degree(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> edges() {
        return g.edges();
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getAdjacencyList(int vertex) {
        return g.getAdjacencyList(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getNeighbors(int vertex) {
        return g.getNeighbors(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getEdges(int vertex1, int vertex2) {
        return g.getEdges(vertex1, vertex2);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCycles() {
        return g.hasCycles();
    }

    /**
     * {@inheritDoc}
     */
    public int order() {
        return g.order();
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(T e) {
        return g.remove(e);
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int vertex) {
        return g.remove(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return g.size();
    }
    
    /**
     * {@inheritDoc}
     */
    public Graph<T> subgraph(Set<Integer> vertices) {
        return g.subgraph(vertices);
    }

//     /**
//      * {@inheritDoc}
//      */
//     public Graph<T> subview(Set<Integer> vertices) {
//         return g.subview(vertices);
//     }

    public String toString() {
        return g.toString();
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<Integer> vertices() {
        return g.vertices();
    }
    
}
