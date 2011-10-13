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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;

import edu.ucla.sspace.util.Indexer;

/**
 *
 */
public class TypedIsomorphicGraphIndexer<T,G extends Multigraph<T,? extends TypedEdge<T>>>
        implements Indexer<G>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The isomorphism tester used to find multigraph equality
     */
    private final IsomorphismTester isoTest;

    private final Map<G,Integer> graphIndices;

    public TypedIsomorphicGraphIndexer() {
        isoTest = new TypedVF2IsomorphismTester();
        graphIndices = new HashMap<G,Integer>();
    }

    public TypedIsomorphicGraphIndexer(Collection<? extends G> c) {
        this();
        for (G g : c)
            index(g);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        graphIndices.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(G g) {
        for (Map.Entry<G,Integer> e : graphIndices.entrySet()) {
            if (isoTest.areIsomorphic(g, e.getKey())) 
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int find(G g) {
        for (Map.Entry<G,Integer> e : graphIndices.entrySet()) {
            if (isoTest.areIsomorphic(g, e.getKey())) 
                return e.getValue();
        }
        return -1;
    }
    
    /**
     * {@inheritDoc}
     */
    public int highestIndex() {
        return graphIndices.size() - 1;
    }

    /**
     * {@inheritDoc}
     */
    public int index(G g) {
        for (Map.Entry<G,Integer> e : graphIndices.entrySet()) {
            if (isoTest.areIsomorphic(g, e.getKey())) 
                return e.getValue();
        }
        // No index assigned, so create a new one
        int index = graphIndices.size();
        graphIndices.put(g, index);
        return index;
    }

    /**
     * {@inheritDoc}
     */
    public Set<G> items() {
        return Collections.unmodifiableSet(graphIndices.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Map.Entry<G,Integer>> iterator() {
        return graphIndices.entrySet().iterator();
    }

    /**
     * {@inheritDoc}
     */
    public G lookup(int index) {
        throw new Error();
    }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,G> mapping() {
        throw new Error();
    }
    
    /**
     * Returns the number of isomorphic graphs that are mapped to indices.
     */
    public int size() {
        return graphIndices.size();
    }
}