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


/**
 * An interface for edges in multigraphs.  In a multigraph, two vertices
 * v<sub>1</sub> and v<sub>2</sub> may have mulitple edges between then provided
 * that the for any two edges {@code !e1.equals(e2)}.
 *
 * <p> This interface allows for a multigraph to have mutliple <i>types</i> of
 * edges that extend from a common type.  For example, a graph that represents
 * cities may contain edges indicating the different types of transportation
 * (e.g. car, train, bus) between two cities, where those types each have their
 * own subtypes (e.g., airline carrier, bus company, etc.)
 *
 * @see Multigraph
 */
public interface TypedEdge<T> extends Edge {

    /**
     * Returns the type of information conveyed by this edge.
     */
    T edgeType();

    /**
     * Returns {@code true} if the other edge is considered equivalent to this
     * edge in a multigrpah.
     */
    boolean equals(Object o);

}