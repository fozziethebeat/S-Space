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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;


/**
 * A {@link Set} of {@link Graph} instances that maintains the invariant that
 * all graphs in the set are non-isomorphisms of each other.  In essenence, two
 * graphs are treated as being equal if they are isomorphic, rather than if
 * their Object {@link Object#equals(Object) equality} is {@code true}.  
 *
 * <p> By default, this class only considers two graphs isomorphic if they are
 * structurally isomorphic (which includes edge directions).  However, this
 * class provides an overloaded constructor to specify a custom {@link
 * IsomorphismTester}, which allows further refinements to which graphs are
 * considered isomorphic.
 */
public class IsomorphicSet<G extends Graph<? extends Edge>> 
        extends AbstractSet<G> 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The list of graphis in this set.  Note that this is a list and not a set,
     * since this class relies on isomorphism for equality, rather than object
     * equivalence.
     */
    private final List<G> graphs;

    /**
     * The tester used to measure isomorphism
     */
    private final IsomorphismTester tester;

    /**
     * Creates an empty set of structurally isomorphic graphs
     */
    public IsomorphicSet() {
        this(new VF2IsomorphismTester());
    }

    /**
     * Creates an empty set of isomorphic graphs whose isomorphism is measured
     * according to the provided tester's isomorphic constraints.
     */
    public IsomorphicSet(IsomorphismTester tester) {
        graphs = new ArrayList<G>();
        this.tester = tester;
    }

    /**
     * Creates a set of structurally isomorphic graphs from those in the
     * provided collection.
     */
    public IsomorphicSet(Collection<? extends G> graphs) {
        this();
        addAll(graphs);
    }

    /**
     * Creates an empty set of isomorphic graphs from those in the collection
     * using the provided tester's isomorphic constraints.
     */
    public IsomorphicSet(IsomorphismTester tester, 
                         Collection<? extends G> graphs) {
        this(tester);
        addAll(graphs);
    }

    public boolean add(G graph) {
        for (G g : graphs)
            if (tester.areIsomorphic(g, graph))
                return false;
        graphs.add(graph);
        return true;
    }

    public boolean contains(Object o) {
        if (!(o instanceof Graph))
            return false;
        Graph<? extends Edge> graph = (Graph<? extends Edge>)o;
        for (G g : graphs)
            if (tester.areIsomorphic(g, graph))
                return true;
        return false;
    }

    public Iterator<G> iterator() {
        return graphs.iterator();
    }

    public boolean remove(Object o) {
        if (!(o instanceof Graph))
            return false;
        Graph<? extends Edge> graph = (Graph<? extends Edge>)o;
        Iterator<G> iter = graphs.iterator();
        while (iter.hasNext()) {
            G g = iter.next();
            if (tester.areIsomorphic(g, graph)) {
                iter.remove();
                return true;
            }
        }
        return false;
    }

    public int size() {
        return graphs.size();
    }
}