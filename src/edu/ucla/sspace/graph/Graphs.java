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

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * A collection of static utility methods for interacting with {@link Graph}
 * instances.  This class is modeled after the {@link Collections} class.
 *
 * <p> Unless otherwise noted, all methods will throw an {@link
 * NullPointerException} if passed a {@code null} graph.
 */
public final class Graphs {

    private Graphs() { }

    public static <E extends DirectedEdge> DirectedGraph<E> asDirectedGraph(Graph<E> g) {
        if (g == null)
            throw new NullPointerException();
        return (g instanceof DirectedGraph)
            ? (DirectedGraph<E>)g
            : new DirectedGraphAdaptor<E>(g);
    }

    public static <E extends WeightedEdge> WeightedGraph<E> asWeightedGraph(Graph<E> g) {
        throw new Error();
    }

    public static <T,E extends TypedEdge<T>> Multigraph<T,E> asMultigraph(Graph<E> g) {
        if (g == null)
            throw new NullPointerException();
        if (g instanceof Multigraph) {
            @SuppressWarnings("unchecked")
            Multigraph<T,E> m = (Multigraph<T,E>)g;
            return m;
        }
        else
            return new MultigraphAdaptor<T,E>(g);
    }

    @SuppressWarnings("unchecked")
    public static <E extends Edge> Graph<?> newInstance(Graph<E> g) {
        if (g instanceof Multigraph) {
            if (g instanceof DirectedGraph) {
                if (g instanceof WeightedGraph)
                    throw new Error("missing graph type");
                else
                    return new DirectedMultigraph();
            }
            else if (g instanceof WeightedGraph) 
                throw new Error("missing graph type");
            else
                return new UndirectedMultigraph<E>();
        }
        else if (g instanceof DirectedGraph) {
            if (g instanceof WeightedGraph)
                throw new Error("missing graph type");
            else
                return new SparseDirectedGraph();
                
        }
        else if (g instanceof WeightedGraph) {
            throw new Error("missing graph type");
        }
        else return new SparseUndirectedGraph();
    }
    
    /**
     * Shuffles the edges of {@code g} while still preserving the <a
     * href="http://en.wikipedia.org/wiki/Degree_sequence#Degree_sequence">degree
     * sequence</a> of the graph.  Each edge in the graph will attempted to be
     * conflated with another edge in the graph the specified number of times.
     * If the edge cannot be swapped (possible due to the new version of the
     * edge already existing), the attempt fails.
     *
     * @param g the graph whose elemets will be shuffled
     * @param shufflesPerEdge the number of swaps to attempt per edge.
     *
     * @return the total number of times an edge's endpoint was swapped with
     *         another edge's endpoint.  At its maximum value, this will be
     *         {@code shufflesPerEdge * g.size()} assuming that each swap was
     *         successful.  For dense graphs, this return value will be much
     *         less.
     *
     * @throws IllegalArgumentException if {@code shufflesPerEdge} is
     *         non-positive
     */
    public static <E extends Edge> int shufflePreserve(Graph<E> g, 
                                                       int shufflesPerEdge) {
        if (shufflesPerEdge < 1)
            throw new IllegalArgumentException("must shuffle at least once");
        return shuffleInternal(g, g.edges(), shufflesPerEdge);
    }

    /**
     * Shuffles the edges in {@code edges} using the provided graph {@code g} to
     * check whether the permuted edges created by the shuffling process already
     * exist.
     */
    private static <E extends Edge> int shuffleInternal(
                              Graph<E> g, Set<E> edges, int shufflesPerEdge) {
        int totalShuffles = 0;
        int origSize = g.size();
        int numEdges = edges.size();
        if (numEdges < 2)
            return 0;

        // Copy the edges into an array so that we can easily swap them and
        // perform random access on them without needing to do O(n) traversal to
        // access an arbitrary edge.  Because the edge type is a generic, we
        // have to reflectively create an array for its type.
        E tmp = edges.iterator().next();
        @SuppressWarnings("unchecked")
        E[] edgeArray = (E[])Array.newInstance(tmp.getClass(), 0);
        edgeArray = edges.toArray(edgeArray);
        Random rand = new Random();

        for (int i = 0; i < numEdges; ++i) {
            
            for (int swap = 0; swap < shufflesPerEdge; ++swap) {
                // Pick another vertex to conflate with i that is not i
                int j = i; 
                while (i == j)
                    j = rand.nextInt(numEdges);

                E e1 = edgeArray[i];
                E e2 = edgeArray[j];
                
                // For non-directed graphs, we should randomly flip the edge
                // orientation to guard against situations where some vertices
                // only appear on either to() or from().
                if (!(e1 instanceof DirectedEdge) && rand.nextDouble() < .5)
                    e1 = e1.<E>flip();
                if (!(e2 instanceof DirectedEdge) && rand.nextDouble() < .5)
                    e2 = e2.<E>flip();

                // Swap their end points
                E swapped1 = e1.<E>clone(e1.from(), e2.to());
                E swapped2 = e2.<E>clone(e2.from(), e1.to());
            
                // Check that the new edges do not already exist in the graph
                // and that they are not self edges
                if (g.contains(swapped1) || g.contains(swapped2))
                    continue;
                else if (swapped1.from() == swapped1.to()
                         || swapped2.from() == swapped2.to()) {
                    continue;
                }
                totalShuffles++;
            
                // Remove the old edges
                boolean r1 = g.remove(edgeArray[i]);
                boolean r2 = g.remove(edgeArray[j]);
                
                // Put in the swapped-end-point edges
                g.add(swapped1);
                g.add(swapped2);
                
                // Update the in-memory edges set so that if these edges are drawn
                // again, they don't point to old edges
                edgeArray[i] = swapped1;
                edgeArray[j] = swapped2;
                assert g.size() == origSize : "Added an extra edge of either "
                    + swapped1 + " or " + swapped2;
            }
        }
        return totalShuffles;
    }

    /**
     * Shuffles the edges of {@code g} while still preserving the <a
     * href="http://en.wikipedia.org/wiki/Degree_sequence#Degree_sequence">degree
     * sequence</a> of the graph and that edges are only swapped with those of
     * the same type.  Each edge in the graph will attempted to be conflated
     * with another edge in the graph the specified number of times.  If the
     * edge cannot be swapped (possible due to the new version of the edge
     * already existing), the attempt fails.
     *
     * <p> Note that the {@link Multigraph#subview(Set,Set)} method makes it
     * possilble to shuffle the edges for only a subset of the types in the
     * multigraph.
     *
     * @param g the graph whose elemets will be shuffled
     * @param shufflesPerEdge the number of swaps to attempt per edge.
     *
     * @return the total number of times an edge's endpoint was swapped with
     *         another edge's endpoint.  At its maximum value, this will be
     *         {@code shufflesPerEdge * g.size()} assuming that each swap was
     *         successful.  For dense graphs, this return value will be much
     *         less.
     *
     * @throws IllegalArgumentException if {@code shufflesPerEdge} is
     *         non-positive
     */
    public static <T,E extends TypedEdge<T>> int
              shufflePreserveType(Multigraph<T,E> g, int shufflesPerEdge) {

        if (shufflesPerEdge < 1)
            throw new IllegalArgumentException("must shuffle at least once");

        int totalShuffles = 0;
        int order = g.order();
        int size = g.size();
        
        // Iterate through all of the types in the graph, shuffling only edges
        // of that type.  The shuffling process per type could potentially alter
        // the state of the map's type mapping.  Therefore, copy the set of edge
        // types prior to mutating the map in order to avoid a
        // ConcurrentModificationException
        Set<T> types = new HashSet<T>(g.edgeTypes());
        for (T type : types) {
            // Shuffle the edges of the current type only 
            totalShuffles += shuffleInternal(g, g.edges(type), shufflesPerEdge);
        }

        assert order == g.order() : "Changed the number of vertices";
        assert size == g.size() : "Changed the number of edges";
        return totalShuffles;
    }

    /**
     * To-do
     */
    public static <T extends Edge> Graph<T> synchronizedGraph(Graph<T> g) {
        throw new Error();
    }

    /**
     * Returns a pretty-print string version of the graph as an adjacency matrix
     * where a 1 indicates an edge and a 0 indicates no edge.
     */
    public static String toAdjacencyMatrixString(Graph<?> g) {
        StringBuilder sb = new StringBuilder(g.order() * (g.order() + 1));
        for (Integer from : g.vertices()) {
            for (Integer to : g.vertices()) {
                Edge e = new SimpleDirectedEdge(from, to);
                if (g.contains(e))
                    sb.append('1');
                else
                    sb.append('0');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * To-do
     */
    public static <T extends Edge> Graph<T> unmodifiable(Graph<T> g) {
         throw new Error();
    }   
}