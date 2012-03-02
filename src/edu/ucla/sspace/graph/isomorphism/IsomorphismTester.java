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


/**
 * An interface for algorithms that detect whether two graphs are <a
 * href="http://en.wikipedia.org/wiki/Graph_isomorphism">isomorphic</a>.
 * Implementations are open to define any further refinements beyond structural
 * isomorphism, which may include: <ul> <li> node types <li> edge types <li>
 * edge weights <li> edge directions </ul>.
 */
public interface IsomorphismTester {

    /**
     * Returns {@code true} if the graphs are isomorphism of each other.
     */
    boolean areIsomorphic(Graph<? extends Edge> g1, Graph<? extends Edge> g2);

    /**
     * Returns an isomorphic mapping from the vertices in {@code g1} to the
     * vertices in {@code g2}, or an empty {@code Map} if no such mapping
     * exists.
     */
    Map<Integer,Integer> findIsomorphism(Graph<? extends Edge> g1, 
                                         Graph<? extends Edge> g2);
}