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
 * A subinterface of {@link Set} that provides an abstraction over the set of
 * edges that are connected to a single vertex.  In addition, the interface
 * provides utility methods for interacting with the {@link Edge} objects stored
 * within the set on the basis of their vertex indices.
 *
 * @param <T> the type of {@link Edge} being stored within this set.
 */
public interface EdgeSet<T extends Edge> extends Set<T> {  

    /**
     * Adds the edge to the set only if the edge is connected to the root
     * vertex.
     *
     * @return {@code true} if the edge was added, {@code false} if the edge was
     *         already present, or if it could not be added to this edge set due
     *         to the root vertex not being connected to the edge
     */
    boolean add(T edge);

    /**
     * Returns the set of vertices connected to the root edges.  Implementations
     * are left free to decide whether modifications to this set are allowed.
     */
    Set<Integer> connected();

    /**
     * Returns true if the root vertex is connected to the provided vertex.
     */
    boolean connects(int vertex);

    /**
     * Returns the set of {@link Edge} instances that connect the root vertex
     * with this vertex or an empty set if no such edges exist.
     */
    Set<T> getEdges(int vertex);

    /**
     * Returns the vertex to which all edges in this set are connected.
     */
    int getRoot();
}