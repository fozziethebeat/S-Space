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

package edu.ucla.sspace.graph.isomorphism;

import java.util.Map;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;

import edu.ucla.sspace.util.Pair;

/**
 * An interface for representing the partial or complete state of an a attempted
 * isomorphic mapping between two graphs.  This interface exposes as set of
 * common operations used by {@link AbstractIsomorphismTester} to detect
 * isomorphism in a general manner while leaving the algorithm specifics
 * isolated to a {@link State} implementation.
 */
public interface State  {

    /**
     * The node marker to be used when indicating that that no node is being
     * matched. 
     */
    public static final int NULL_NODE = -1;

    /**
     * Returns the first graph being matched 
     */
    Graph<? extends Edge> getGraph1();

    /**
     * Returns the first graph being matched 
     */
    Graph<? extends Edge> getGraph2();

    /**
     * Returns the next candidate for isomorphic matching given these prior two
     * vertices that were matched.  If {@code prevN1} and {@code prevN1} are
     * {@code NULL_NODE}, this should return the initial candidate.
     */
    Pair<Integer> nextPair(int prevN1, int prevN2);

    /**
     * Adds the two vertices to this {@code State}'s vertex mapping.
     */ 
    void addPair(int n1, int n2);

    /**
     * Returns {@code true} if mapping {@code node1} to {@code node2} would
     * preseve the isomorphism between the graphs to the extend that their
     * vertices have been mapped thusfar.
     */
    boolean isFeasiblePair(int node1, int node2);

    /**
     * Returns {@code true} if all the vertices have been mapped.  Equivalently,
     * returns {@code true} if the graphs are isomorphic.
     */
    boolean isGoal();

    /**
     * Returns {@code true} if the current state of mapping cannot proceed
     * because some invalid mapping has occurred and no further pairs would
     * result in an isomorphic match.
     */
    boolean isDead();

    /**
     * Returns the current mapping between vertices.
     */
    Map<Integer,Integer> getVertexMapping();

    /**
     * Makes a shallow copy of the content of this state.
     */
    State copy();

    /**
     * Undoes the mapping added in the prior call to {@code addPair}.
     */
    void backTrack();
}
