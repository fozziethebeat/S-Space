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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

// Logger helper methods
import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;


/**
 * An implementation of the EnumerateSubgraphs (ESU) method from Wernicke
 * (2006), which enumerates all possible <i>k</i>-vertex subgraphs of a given
 * graph.  For full details see: <ul>
 *
 * <li style="font-family:Garamond, Georgia, serif"> Sebastian
 *   Wernicke. Efficient detection of network motifs. <i>in</i> IEEE/ACM
 *   Transactions on Computational Biology and Bioinformatics.
 * </li>
 * </ul>
 *
 * In summary, this iterator returns all possible size-<i>k</i> subgraphs of the
 * input graph through an efficient traversal.
 *
 * <p> This implementation does not store the entire set of possible subgraphs
 * in memory at one time, but may hold some arbitrary number of them in memory
 * and compute the rest as needed.
 *
 * <p> This class is not thread-safe and does not support the {@link #remove()}
 * method.
 *
 * @author David Jurgens
 */
public class SubgraphIterator<E extends Edge,G extends Graph<E>> 
    implements Iterator<G>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = 
        Logger.getLogger(SubgraphIterator.class.getName());
    
    /**
     * The graph whose subgraphs are being iterated
     */
    private final G g;

    /**
     * The size of the subgraph to return
     */
    private final int subgraphSize;

    /**
     * An interator over the vertices of {@code g}, which are each used to a
     * seed to generate sequences of subgraphs
     */
    private Iterator<Integer> vertexIter;

    /**
     * The current pseudo-iterator for all the subgraphs that can be generated
     * from a particular vertex
     */
    private Extension ext;

    /**
     * The next subgraph to return or {@code null} if no further subgraphs
     * remain
     */
    private G next;    

    /**
     * Constructs an iterator over all the subgraphs of {@code g} with the
     * specified subgraph size.
     *
     * @param g a graph
     * @param subgraphSize the size of the subgraphs to return
     *
     * @throws IllegalArgumentException if subgraphSize is less than 1 or is
     *         greater than the number of vertices of {@code g}
     * @throws NullPointerException if {@code g} is {@code null}
     */
    public SubgraphIterator(G g, int subgraphSize) {
        this.g = g;
        this.subgraphSize = subgraphSize;
        if (g == null)
            throw new NullPointerException();
        if (subgraphSize < 1)
            throw new IllegalArgumentException("size must be positive");
        if (subgraphSize > g.order())
            throw new IllegalArgumentException("size must not be greater " 
                + "than the number of vertices in the graph");
        vertexIter = g.vertices().iterator();
        advance();
    }

    /**
     * Advances the state of this iterating, updating {@code next} to be the
     * next graph to return or {@code null} if no further subgraphs remain.
     */
    private void advance() {
        next = null;
        while (next == null) {
            // If 
            if (ext != null) {
                next = ext.next();
                if (next != null)
                    return;
            }
            if (!vertexIter.hasNext())
                return;

            // Otherwise the extension is null or there are no more remaining
            // subgraphs in it, so create a new 
            Integer nextVertex = vertexIter.next();
            veryVerbose(LOGGER, "Loading next round of subgraphs starting " +
                        "from vertex %d", nextVertex);
            ext = new Extension(nextVertex);
        }
    }

    public boolean hasNext() { 
        return next != null; 
    }

    public G next() { 
        if (!hasNext()) 
            throw new NoSuchElementException();
        G n = next;
        advance();
        return n;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException(
            "Cannot remove subgraphs during iteration");
    }

    /**
     * This class is a pseudo-iterator for generate a sequence of subgraph
     * extensions from a seed vertex in the input graph.  Interally the class
     * represents the enumeration state using a stack of possible subgraphs,
     * which avoids the memory intensive recursive implementation.
     */
    class Extension {
        
        /**
         * The vertex in the graph from which subgraph extensions are generated
         */
        private final int v;

        /**
         * The current set of vertices in the subgraph.  Note that although this
         * is a {@code Deque}, it is maintained as a set.  A deque is used to
         * quickly push and pop vertices out of the set based on insertion order.
         */
        private final Deque<Integer> vertsInSubgraph;

        /**
         * A stack of sets of vertices that can be potentially combined to
         * generate new subgraphs.
         */
        private final Deque<TIntHashSet> extensionStack;

        /**
         * Creates a new extension for the provided vertex in the backing graph
         */
        public Extension(int v) {
            this.v = v;
            vertsInSubgraph = new ArrayDeque<Integer>();
            extensionStack = new ArrayDeque<TIntHashSet>();

            Set<Integer> neighbors = g.getNeighbors(v);
            TIntHashSet extension = new TIntHashSet();
            for (Integer u : neighbors)
                if (u > v)
                    extension.add(u);

            vertsInSubgraph.push(v);
            extensionStack.push(extension);
        }
        
        /**
         * Returns the next graph in enumeration or {@code null} if no further
         * graphs can be generated from this {@code Extension}'s seed vertex.
         */
        public G next() {
            // Load the next set of extensions to the current subgraph
            TIntHashSet curExtension = extensionStack.peek();
            if (curExtension == null)
                return null;
            else {
                while (extensionStack.size() < subgraphSize - 1 
                       && !extensionStack.isEmpty()) {
                    loadNextExtension();

                    curExtension = extensionStack.peek();
                    if (curExtension.isEmpty())
                        extensionStack.pop();
                }
            }

            // Some checking here
            curExtension = extensionStack.peek();
            if (curExtension == null)
                return null;

            TIntIterator iter = curExtension.iterator();
            Integer w = iter.next();
            iter.remove();
            
            vertsInSubgraph.push(w);
            // The return type of copy() isn't parameterized on the type of the
            // graph itself.  However, we know that all the current interfaces
            // confirm to the convention that the type is refined (narrowed,
            // really), so we perform the cast here to give the user back the
            // more specific type.
            @SuppressWarnings("unchecked")
                G next = (G)g.copy(new HashSet<Integer>(vertsInSubgraph));

            // Remove the most recently added vertex from the set of vertices so
            // that the next call to next() can add its vertex
            vertsInSubgraph.pop();

            // If that was the last vertex in the current extension, then pop it
            // off the stack to signify the next extension shouldbe loaded.
            if (curExtension.isEmpty()) {
                extensionStack.pop();
                vertsInSubgraph.pop();
            }
            
            return next;            
        }

        /**
         * Loads the next set of vertices that can be potentially in the
         * subgraph, updating the {@code extensionStack} as necessary.  This
         * method is somewhat equivalent to a recursive call of
         * enumerateSubgraph, but minimizes the state that needs to be
         * maintained.  If no possible extensions remain, the {@code
         * extensionStack} will be empty upon this method's return.
         */
        private void loadNextExtension() {
            // Get the set of vertices that are on the top of the stack
            // currently
            TIntHashSet extension = extensionStack.peek();
            if (extension == null)
                throw new IllegalStateException();

            if (extension.isEmpty())
                return;

            // Choose and remove an aribitrary vertex from the extension            
            TIntIterator iter = extension.iterator();
            Integer w = iter.next();
            iter.remove();

            // The next extension is formed from all edges to vertices whose
            // indices are greater than the currently selected vertex, w,
            // and that point to a vertex in the exclusive neighborhood of
            // w.  The exclusive neighborhood is defined relative to a set
            // of vertices N: all vertices that are adjacent to w but are
            // not in N or the neighbors of N.  In this case, N is the
            // current subgraph's vertices
            TIntHashSet nextExtension = new TIntHashSet(extension);
            next_vertex:
            for (Integer n : g.getNeighbors(w)) {
                // Perform the fast vertex value test and check for whether
                // the vertex is currently in the subgraph
                if (n > v && !vertsInSubgraph.contains(n)) {
                    // Then perform the most expensive
                    // exclusive-neighborhood test that looks at the
                    // neighbors of the vertices in the current subgraph
                    Iterator<Integer> subIter = vertsInSubgraph.iterator();
                    while (subIter.hasNext()) {
                        int inCur = subIter.next();
                        // If we find n within the neighbors of a vertex in
                        // the current subgraph, then skip the remaining
                        // checks and examine another vertex adjacent to w.
                        if (g.contains(inCur, n))
                            continue next_vertex;
                    }
                    // Otherwise, n is in the exclusive neighborhood of w,
                    // so add it to the future extension.
                    nextExtension.add(n);
                }
            }
            vertsInSubgraph.push(w);
            extensionStack.push(nextExtension);
        }
    }
}