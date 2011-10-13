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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.sspace.graph.*; // FIX

import edu.ucla.sspace.util.Pair;

import gnu.trove.TDecorators;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import static edu.ucla.sspace.graph.isomorphism.State.NULL_NODE;


/**
 * An abstraction of an {@link IsomorphismTester} that relies on
 * algorithm-specific {@link State} implementations to check for edge and vertex
 * contraints when performing isomorphism testing.  
 *
 * <p> This class is a an adaptation of parts of the VFLib library.
 */
public abstract class AbstractIsomorphismTester implements IsomorphismTester {

    /**
     * {@inheritDoc}
     */
    public boolean areIsomorphic(Graph<? extends Edge> g1, 
                                 Graph<? extends Edge> g2) {
        Graph<? extends Edge> g1mapped = remap(g1, null);
        Graph<? extends Edge> g2mapped = remap(g2, null);
        State state = makeInitialState(g1mapped, g2mapped);
        return match(state);
    }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,Integer> findIsomorphism(Graph<? extends Edge> g1, 
                                                Graph<? extends Edge> g2) {
        // Since the user has asked to find the mapping itself, we'll need to
        // keep track of how the vertices are remapped.  Note that if the graphs
        // are already in canonical order, then these maps will be empty.
        TIntIntHashMap g1vMap = new TIntIntHashMap(g1.order());
        TIntIntHashMap g2vMap = new TIntIntHashMap(g2.order());

        // Remap the graphs so that the vertices are contiguous, filling in a
        // backwards mapping so we can recover the isomorphism later.
        Graph<? extends Edge> g1mapped = remap(g1, g1vMap);
        Graph<? extends Edge> g2mapped = remap(g2, g2vMap);
        State state = makeInitialState(g1mapped, g2mapped);

        Map<Integer,Integer> isoMapping = new HashMap<Integer,Integer>();
        // If we mapped the vertices, we have to unmap the vertices in order
        // to identify the isometric pairs.
        if (match(state, isoMapping)) {
            // Create a new isometric map that will contain the unmapped
            // mappings
            TIntIntMap fixedIsoMapping = new TIntIntHashMap(isoMapping.size());
            for (Map.Entry<Integer,Integer> e : isoMapping.entrySet()) {
                int v1 = e.getKey();
                v1 = (g1vMap.isEmpty()) ? v1 : g1vMap.get(v1);
                int v2 = e.getValue();
                v2 = (g2vMap.isEmpty()) ? v2 : g2vMap.get(v2);
                fixedIsoMapping.put(v1, v2);
            }
            return TDecorators.wrap(fixedIsoMapping);
        }
        else
            return Collections.<Integer,Integer>emptyMap();
    }

    /**
     * Creates an empty {@link State} for mapping the vertices of {@code g1} to
     * {@code g2}.
     */
    protected abstract State makeInitialState(Graph<? extends Edge> g1, 
                                              Graph<? extends Edge> g2);

    /**
     * Returns {@code true} if the graphs being matched by this state are
     * isomorphic.
     */
    private boolean match(State s) {
        if (s.isGoal())
            return true;        

        if (s.isDead()) 
            return false;

        int n1 = NULL_NODE, n2 = NULL_NODE;
        Pair<Integer> next = null;
        boolean found = false;
        while (!found && (next = s.nextPair(n1, n2)) != null) {
            n1 = next.x;
            n2 = next.y;
            if (s.isFeasiblePair(n1, n2)) {
                State copy = s.copy();
                copy.addPair(n1, n2);
                found = match(copy);
                // If we found a match, then don't bother backtracking as it
                // would be wasted effort.
                if (!found)
                    copy.backTrack();
            }
        }
        return found;
    }

    /**
     * Returns {@code true} if the graphs being matched by this state are
     * isomorphic.
     */
    private boolean match(State s, Map<Integer,Integer> isoMap) {
        if (s.isGoal())
            return true;        

        if (s.isDead()) 
            return false;

        int n1 = NULL_NODE, n2 = NULL_NODE;
        Pair<Integer> next = null;
        boolean found = false;
        while (!found && (next = s.nextPair(n1, n2)) != null) {
            n1 = next.x;
            n2 = next.y;
            if (s.isFeasiblePair(n1, n2)) {
                State copy = s.copy();
                copy.addPair(n1, n2);
                found = match(copy, isoMap);
                // If we found a mapping, fill the vertex mapping state
                if (found) 
                    isoMap.putAll(copy.getVertexMapping());
                // Otherwise, back track and try again
                else
                    copy.backTrack();
            }
        }
        return found;
    }

    /**
     * Remaps the graph's vertices so that they are in a contiguous range from 0
     * to {@code g.order()}-1.  If the graph's vertices are already contiguous,
     * returns the original graph.
     *
     * @param rvMap an optional parameter for a map that is filled with the
     *        reverse mapping from the vertex indices in the return map to the
     *        indices of {@code g}
     */
    private <E extends Edge> Graph<E> remap(Graph<E> g, TIntIntHashMap rvMap) {
        int order = g.order();
        boolean isContiguous = true;
        for (int i : g.vertices()) {
            if (i >= order) {
                isContiguous = false;
                break;
            }
        }
        if (isContiguous) 
            return g;        

        // Map the vertices to a contiguous range
        TIntIntMap vMap = new TIntIntHashMap(g.order());
        int j = 0;
        for (int i : g.vertices()) {
            vMap.put(i, j++);
        }
        
        if (rvMap != null) {
            // Assumes a stable iteration ordering of the vertices, which is
            // potentially unsafe... However, this avoids putting a redundant
            // null check in the prior loop.
            int k = 0;
            for (int i : g.vertices()) {
                rvMap.put(k++, i);
            }
        }
        
        Graph<E> copy = g.copy(Collections.<Integer>emptySet());
        for (int i = 0; i < order; ++i)
            copy.add(i);
        for (E e : g.edges()) 
            copy.add(e.<E>clone(vMap.get(e.from()), vMap.get(e.to())));
        
        return copy;
    }    
}