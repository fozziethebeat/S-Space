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

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;


/**
 * An extension of the VF2 algorithm for detecting isomorphic graphs that tests
 * for edge type equivalance.  The VF2 algorithm may be found in: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif"> Luigi P. Cordella,
 *      Pasquale Foggia, Carlo Sansone, and Mario Vento.  A (Sub)Graph
 *      Isomorphism Algorithm for Matching Large Graphs.  <i>IEEE Transactions
 *      on Pattern Analysis and Machine Intelligence,</i> <b>26:10</b>.  2004.
 *      Available <a
 *      href="http://ieeexplore.ieee.org/xpls/abs_all.jsp?arnumber=1323804">here</a>
 *
 * </ul> This class checks for both structural and edge type equivalence between
 * two graphs.  If the graphs are not instances of {@link Multigraph} or have a
 * single edge type, this class is equivalent in functionality to the original
 * VF2 algorithm.
 *
 * <p>This implementation is an adaptation of the VFLib implementation.
 *
 * <p>This class is thread-safe.
 *
 * @author David Jurgens
 */
public class TypedVF2IsomorphismTester extends AbstractIsomorphismTester {

    private static final long SerialVersionUID = 1L;

    /**
     * Creates a new {@code TypedVF2IsomorphismTester} instance
     */
    public TypedVF2IsomorphismTester() { }

    /**
     * Returns a new {@code State} for running the VF2 algorithm.
     */ 
    protected State makeInitialState(Graph<? extends Edge> g1, 
                                     Graph<? extends Edge> g2) {
        return new TypedVF2State(g1, g2);
    }
}