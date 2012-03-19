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
 * An interface specification for interacting with <a
 * href="http://en.wikipedia.org/wiki/Multigraph">MultiGraph</a> objects.  A
 * multigraph contains a set of vertices and a list of edges between vertices,
 * where multiple, parallel edges may exist between two vertices.
 *
 * <p> The edge type parameter for a multigraph is used to distinguish between
 * different edges between the same two nodes.  For example, a multigraph may
 * represents cities with edges indicating the different types of transportation
 * (e.g. car, train, bus) between two cities.  Edge type may additionally have
 * their own subtypes (e.g., airline carrier, bus company, etc.).  This
 * multigraph class only considers the value of the edge type to determine if
 * two edges are equal, i.e. two edges between the same nodes are equal if for
 * their edge types t1 and t2, {@code t1.equals(t2)}.

 *
 * @param T a class type whose values are used to distinguish between edge types
 * 
 * @author David Jurgens
 */
public interface Multigraph<T,E extends TypedEdge<T>> extends Graph<E> {

    /**
     * Removes all the edges in the graph with the specified edge type.
     */
    void clearEdges(T edgeType);

    /**
     * Returns {@code true} if there exists an edge between {@code vertex1} and
     * {@code vertex2} of the specified type.
     */
    boolean contains(int vertex1, int vertex2, T edgeType);

    /**
     * {@inheritDoc}
     */
    Multigraph<T,E> copy(Set<Integer> vertices);

    /**
     * Creates a copy of this graph containing only the specified number of
     * vertices and all edges between those vertices with the specified type.
     * If {@code vertices} is empty a new, empty graph of this instance's type
     * is returned.  Any changes made to this graph will not be reflected in
     * returned copy or vice-versa.
     */
    //Multigraph<T,E> copy(Set<Integer> vertices, T edgeType);

    /**
     * Returns the set of typed edges in the graph
     */
    Set<E> edges();

    /**
     * Returns the set of edges with the corresponding type or the empty set if
     * no edges of that exist.
     */
    Set<E> edges(T type);

    /**
     * Returns the set of edge types currently present in this graph.
     */
    Set<T> edgeTypes();

    /**
     * Returns the set of typed edges connected to the vertex.
     */
    Set<E> getAdjacencyList(int vertex);

    /**
     * Returns the set of {@link TypedEdge} instances that connect the two
     * vertices.  If no edges connect the vertices, the set will be empty but
     * non-{@code null}.
     */
    Set<E> getEdges(int vertex1, int vertex2);

    /**
     * {@inheritDoc}
     */
    Multigraph<T,E> subgraph(Set<Integer> vertices);

    /**
     * Returns a subgraph of this graph containing only the specified vertices
     * and edges of the specified types.  Note that if all of the vertices are
     * specified, this method can be used to extract edge-type-specific versions
     * of this multigraph.  Any attempt subsequent attempt to add an edge of a
     * type <i>other</i> than the specified types to this subgraph will fail.
     * Implementations are free to specify whether this will be an exception
     */
    Multigraph<T,E> subgraph(Set<Integer> vertices, Set<T> edgeTypes);        
}
