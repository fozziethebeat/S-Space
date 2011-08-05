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

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import edu.ucla.sspace.util.Pair;


/**
 * An iterator over the different permutations of parallel edges within a
 * multigraph that result in connected <a
 * href="http://en.wikipedia.org/wiki/Simple_graph#Simple_graph">simple
 * graphs</a> of a specified size.  Note that due to the combinatorial nature,
 * this iterator should be used with caution, as the complexity is <a
 * href="http://en.wikipedia.org/wiki/Sharp-P">#P</a>.  
 * 
 * @see SubgraphIterator
 * @author David Jurgens
 */
public class SimpleGraphIterator<T, E extends TypedEdge<T>> 
       implements Iterator<Multigraph<T,E>>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * An interator over the (non-simple) subgraphs of the provided graphs.
     */
    private final Iterator<Multigraph<T,E>> subgraphIterator;

    /**
     * The next simple graphs to return.  When a subgraph is analyzed, it will
     * be converted into one more simple graphs, which are then enqueued in this
     * queue.
     */
    private Queue<Multigraph<T,E>> next;
    
    /**
     * Constructs an iterator over all the subgraphs of {@code g} with the
     * specified subgraph size.
     *
     * @param g a multigraph
     * @param subgraphSize the size of the subgraphs to return
     *
     * @throws IllegalArgumentException if subgraphSize is less than 1 or is
     *         greater than the number of vertices of {@code g}
     * @throws NullPointerException if {@code g} is {@code null}
     */
    public SimpleGraphIterator(Multigraph<T,E> g, int subgraphSize) {
        subgraphIterator = //null;
            new SubgraphIterator<E,Multigraph<T,E>>(g, subgraphSize);
        next = new ArrayDeque<Multigraph<T,E>>();
        advance();
    }

    /**
     * Addes the next batch of simple graphs to the {@link #next} queue.
     */
    private void advance() {
        // If we don't have any simple graphs to return, and there are still
        // more subgraphs, then pick off the next subgraph and decompose it
        if (next.isEmpty() && subgraphIterator.hasNext()) {
            Multigraph<T,E> g = subgraphIterator.next();

            // Find all pairs of edges, checking whether the graph is a simple
            // graph already
            boolean isSimpleAlready = true;
            List<Pair<Integer>> connected = new ArrayList<Pair<Integer>>();
            for (int i : g.vertices()) {
                for (int j : g.vertices()) {
                    if (i == j)
                        break;
                    Set<E> edges = null;
                    try {
                        edges = g.getEdges(i, j);
                    } catch (NullPointerException npe) {
                        System.out.println("Bad graph? : " + g);
                        throw npe;
                    }
                    int size = edges.size();
                    if (size > 0)
                        connected.add(new Pair<Integer>(i, j));
                    if (size > 1)
                        isSimpleAlready = false;
                }
            }
            // If the subgraph was already a simple graph, then just append it
            // to the list
            if (isSimpleAlready) {
                // System.out.println("next graph was already simple: " + g);
                next.add(g);
            }
            // Otherwise, generate a recursive call to enumerate all the
            // simple graphs from this graph
            else {
                // Create an empty graph which will be populated with a new edge
                // and then copied 
                Multigraph<T,E> m = g.copy(Collections.<Integer>emptySet()); //Graphs.asMultigraph(new GenericGraph<E>());
                next.addAll(enumerateSimpleGraphs(g, connected, 0, m));
            }
        }
    }

    /**
     * Recursively enumerates the parallel edge permutations of the input graph,
     * building up the graphs and returning the entire set of graphs.
     *
     * @param input the base graph from which edges are selected
     * @param connected the list of vertex pairs that are connected by one or
     *        more edges
     * @param curPair the index into {@code connected} which specifies the two
     *        vertices that need to have one of their connected edges added to
     *        the graph
     * @param toCopy the base graph to which edges for this recursive call will
     *        be added.  This graph will be copied prior to modifying it.
     */
    private Collection<Multigraph<T,E>> enumerateSimpleGraphs(
            Multigraph<T,E> input, List<Pair<Integer>> connected,
            int curPair, Multigraph<T,E> toCopy) {
        
        List<Multigraph<T,E>> simpleGraphs = new LinkedList<Multigraph<T,E>>();
        Pair<Integer> p = connected.get(curPair);
        // Get the set of edges between the current vertex pair
        Set<E> edges = input.getEdges(p.x, p.y);
        // Pick one of the edges and generate a graph from the remaining pairs
        for (E e : edges) {
            // Make a copy of the input graph and add this edge to the graph
            Multigraph<T,E> m = toCopy.copy(toCopy.vertices());
            // Graphs.asMultigraph(new GenericGraph<E>(toCopy));
            // Add one of the edges that connects the current pair
            m.add(e);
            // If there are more pairs to connect, then make the recursive call,
            // passing in this graph
            if (curPair + 1 < connected.size()) {
                simpleGraphs.addAll(
                    enumerateSimpleGraphs(input, connected, curPair + 1, m));
            }
            // Otherwise, this is the last vertex pair for which an edge is to
            // be selected, so add it to the current list
            else {
                // System.out.println("Constructed next enumeration: " + m);
                simpleGraphs.add(m);
            }                    
        }
        return simpleGraphs;
    }

    /**
     * Returns true if there is at least one more simple graph to return.
     */
    public boolean hasNext() {
        return !next.isEmpty();
    }

    /**
     * Returns the next simple graph from the multigraph.
     */
    public Multigraph<T,E> next() {
        if (!hasNext())
            throw new NoSuchElementException();
        Multigraph<T,E> cur = next.poll();
        if (next.isEmpty())
            advance();
        return cur;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
}