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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A decorator over a {@link Graph} with {@link TypedEdge} edges that extends
 * the functionality to support the {@link Multigraph} interface.
 */ 
class MultigraphAdaptor<T,E extends TypedEdge<T>> extends GraphAdaptor<E> 
        implements Multigraph<T,E>, java.io.Serializable {

    private static final long serialVersionUID = 1L;    
    
    /**
     * Constructs a new adaptor over the graph.
     */
    public MultigraphAdaptor(Graph<E> g) {
        super(g);
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges(T edgeType) {
        Iterator<E> it = edges().iterator();
        while (it.hasNext()) {
            if (it.next().edgeType().equals(edgeType))
                it.remove();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public Multigraph<T,E> copy(Set<Integer> vertices) {
        Graph<E> g = super.copy(vertices);
        return new MultigraphAdaptor<T,E>(g);
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,E> copy(Set<Integer> vertices, T type) {
        throw new Error("to do");
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(int vertex1, int vertex2, T edgeType) {
        Set<E> edges = getEdges(vertex1, vertex2);
        for (E e : edges) {
            if (e.edgeType().equals(edgeType))
                return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> edgeTypes() {
        Set<T> types = new HashSet<T>();
        for (E e : edges())
            types.add(e.edgeType());
        return types;
    }

    /**
     * {@inheritDoc}
     */
    public Set<E> edges(T t) {
        throw new Error("TODO");
    }

    /**
     * {@inheritDoc}
     */
    public Multigraph<T,E> subgraph(Set<Integer> vertices) {
        return new MultigraphAdaptor<T,E>(super.subgraph(vertices));
    }

    /**
     * {@inheritDoc}
     */
   public Multigraph<T,E> subgraph(Set<Integer> vertices, Set<T> edgeTypes) {
        throw new Error("TODO");
    }
}