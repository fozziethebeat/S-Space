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

import java.util.Calendar;
import java.util.Date;
import java.util.Set;


/**
 *
 *
 * @author David Jurgens
 */
public interface DynamicGraph extends Graph {

    /**
     * Adds a vertex with the provided index to the graph, returning {@code true}
     * if the vertex was not previously present.
     *
     * @param i a non-negative index for a vertex.  If the graph has size bounds
     *        (i.e. a limited number of vertices), the implementation may throw
     *        an exception if this index exceeds those bounds.
     *
     * @return {@true} if the vertex was not previously present
     *
     * @throws IllegalArgumentException if {@code i} is negative or beyond the
     *         number of representable vertices in the current instance
     */
    boolean addVertex(int i);

    boolean addVertex(int i, Calendar start, Calendar end);
    boolean addVertex(int i, Date start, Date end);
    boolean addVertex(int i, long start, long end);

    /**
     * Adds an edge between the two vertices, returning {@code true} if the edge
     * was not previously present.  Implemenations are free to decide the
     * behavior for cases where one or both of the vertices are not currently in
     * the graph, and whether self-edges are allowed (i.e. {@code vertex1 ==
     * vertex2}).
     */
    boolean addEdge(int vertex1, int vertex2);
    boolean addEdge(int vertex1, int vertex2, Calendar start, Calendar end);
    boolean addEdge(int vertex1, int vertex2, Date start, Date end);
    boolean addEdge(int vertex1, int vertex2, long start, long end);

    /**
     * Removes all the edges and vertices from this graph
     */
    void clear();

    /**
     * Removes all the edges in this graph, retaining all the vertices.
     */
    void clearEdges();

    /**
     * Returns {@code true} if this graph contains an edge between {@code
     * vertex1} and {@code vertex2}.
     */
    boolean containsEdge(int vertex1, int vertex2);

    /**
     * Returns {@code true} if this graph contains an edge between {@code
     * vertex1} and {@code vertex2}.
     */
    boolean containsEdge(int vertex1, int vertex2, Calendar atTime);
    boolean containsEdge(int vertex1, int vertex2, Date atTime);
    boolean containsEdge(int vertex1, int vertex2, long atTime);

    /**
     * Returns the set of edges contained in this graph.
     */
    Set<? extends TemporalEdge> edges();

    /**
     * Returns the set of edges connected to the provided vertex.
     */
    Set<? extends TemporalEdge> getAdjacencyList(int vertex);

    /**
     * Returns the {@code Edge} instance connecting the two vertices or {@code
     * null} if the vertices are not connected.
     */
    TemporalEdge getEdge(int vertex1, int vertex2);

    /**
     * Returns the number of edges in this graph.
     */
    int numEdges();

    /**
     * Returns the number of vertices in this graph.
     */
    int numVertices();

    /**
     * Removes the edge from {@code vertex1} to {@code vertex2}, returning
     * {@code true} if the edge existed and was removed.
     */
    boolean removeEdge(int vertex1, int vertex2);

    /**
     * Removes the vertex and all of its connected edges from the graph.
     */
    boolean removeVertex(int vertex);    
    
    /**
     * Returns a view of this graph containing only the specified vertices.
     * Only edges connecting two vertices in the provided set will be viewable
     * in the subgraph.  Any changes to the subgraph will be reflected in this
     * graph and vice versa.
     *
     * @param vertices the vertices to include in the subgraph
     *
     * @throws IllegalArgumentException if {@code vertices} contains vertices
     *         not present in this graph
     */
    Graph subgraph(Set<Integer> vertices);

    Graph subgraph(Set<Integer> vertices, Calendar atTime);
    Graph subgraph(Set<Integer> vertices, Date atTime);
    Graph subgraph(Set<Integer> vertices, long atTime);

    Graph subgraph(Set<Integer> vertices, Calendar startTime, Calendar endTime);
    Graph subgraph(Set<Integer> vertices, Date startTime, Date endTime);
    Graph subgraph(Set<Integer> vertices, long startTime, long endTime);

    /**
     * Returns the set of vertices in this graph.
     */
    Set<Integer> vertices();
    
}
