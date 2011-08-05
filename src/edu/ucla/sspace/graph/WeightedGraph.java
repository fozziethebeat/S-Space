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
 * An interface specification for interacting with weighted <a
 * href="http://en.wikipedia.org/wiki/Graph_(mathematics)">Graph</a> objects.
 * This interface refines the {@link Graph} interface to associate each edge
 * with a numeric value reflecting the strength of connection between the two
 * vertices.  
 *
 * <p> This interface permits having 0 or negative edge weights.
 *
 * @author David Jurgens
 */
public interface WeightedGraph<E extends WeightedEdge> extends Graph<E> {

    /**
     * {@inheritDoc}
     */
    @Override WeightedGraph<E> copy(Set<Integer> vertices);

    /**
     * Returns the set of edges contained in this graph.
     */
    @Override Set<E> edges();

    /**
     * Returns the set of edges connected to the provided vertex.
     */
    @Override Set<E> getAdjacencyList(int vertex);
    
    /**
     * {@inheritDoc}
     */
    @Override Set<E> getEdges(int vertex1, int vertex2);

    /**
     * Returns the sum of the weights for all edges connected to this vertex.
     * If this vertex is not in the graph, {@code 0} is returned.
     */
    double strength(int vertex);

    /**
     * {@inheritDoc}
     */
    @Override WeightedGraph<E> subgraph(Set<Integer> vertices);    
}
