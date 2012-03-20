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
 * An interface for directed edges in multigraphs.  This interface refines the
 * {@link #equals(Object) equals} method of {@link TypedEdge} to include edge
 * orientation.  Two {@code DirectedTypedEdge} must share the same edge
 * orientation to be equivalent.
 *
 * @see Multigraph
 */
public interface DirectedTypedEdge<T> extends DirectedEdge, TypedEdge<T> {

    /**
     * Returns {@code true} if {@code o} connects the same vertices, has the
     * same edge orientation, and has edge type information that is equivalent.
     */
    boolean equals(Object o);
}