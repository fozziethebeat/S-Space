/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.dependency;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Deque;


/**
 * A traversal class for iterating over a dependency tree of {@link Relation}s.
 * Given the tree and a starting index, the traverser will find all paths that
 * satisfy several different criteria: length of the path and accepted relations
 * in the path.  
 *
 * </p>
 *
 * Note that this class is <b>NOT</b> thread safe.
 */
public class FilteredDependencyIterator implements Iterator<DependencyPath> {

    /**
     * The maximum length of the returned paths.  The length is considedered to
     * not include the first term.
     */
    private final int maxPathLength;

    /**
     * The {@link DependencyPathAcceptor} that validates each link before it is
     * traversed and returned as part of a {@link DependencyPath}.
     */
    private final DependencyPathAcceptor acceptor;
    
    /**
     * The underlying iterator that performs the path expansion in a
     * breadth-first order.
     */
    private final BreadthFirstPathIterator iterator;

    /**
     * The next path to return or {@code null} if there are no further paths to
     * return.
     */
    private DependencyPath next;

    /**
     * Creates a new {@link DependencyIterator} that will return all {@link
     * DependencyPath} instances rooted at {@code startNode} that are validated
     * by the provided acceptor.
     *
     * @param startNode the node that will start all the paths to be generated.
     * @param acceptor The {@link DependencyPathAcceptor} that will validate
     *        each link the a path
     * 
     * @throws IllegalArgumentException if {@code maxPathLength} is less than 1
     */
    public FilteredDependencyIterator(DependencyTreeNode startNode,
                                      DependencyPathAcceptor acceptor) {
        this(startNode, acceptor, Integer.MAX_VALUE);
    }

    /**
     * Creates a new {@link DependencyIterator} that will return all {@link
     * DependencyPath} instances rooted at {@code startNode} that are validated
     * by the provided acceptor and whose length are under the maximum length
     *
     * @param startNode the node that will start all the paths to be generated.
     * @param acceptor The {@link DependencyPathAcceptor} that will validate
     *        the paths returned by this iterator
     * @param maxPathLength the maximum number of nodes in any path
     * 
     * @throws IllegalArgumentException if {@code maxPathLength} is less than 1
     */
    public FilteredDependencyIterator(DependencyTreeNode startNode,
                                      DependencyPathAcceptor acceptor,
                                      int maxPathLength) {
        if (maxPathLength < 1)
            throw new IllegalArgumentException(
                "Must specify a path length greater than 1");
        this.iterator = new BreadthFirstPathIterator(startNode);
        this.acceptor = acceptor;
        this.maxPathLength = maxPathLength;
        advance();
    }

    /**
     * Advances the {@link #next} reference to the next path to return or sets
     * the value to {@code null} if no further paths exist.
     */
    private void advance() {
        DependencyPath p = null;

        // While the underlying iterator has paths, check whether any are
        // accepted by the filter.  If a path is over the maximum path length,
        // break, since no further returned paths will be smaller.
        while (iterator.hasNext()) {
            p = iterator.next();
            if (p.length() > maxPathLength) {
                p = null;
                break;
            } else if (acceptor.accepts(p))
                break;
        }        
        next = p;
    }

    /**
     * Returns {@code true} if there are more paths to return that meet the
     * acceptor and path length requirements.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next path that meets the requirements.  
     *
     * @throws NoSuchElementException if called when no further paths exist
     */
    public DependencyPath next() {
        if (next == null)
            throw new NoSuchElementException("No further paths to return");
        DependencyPath p = next;
        advance();
        return p;
    }
    
    /**
     * Throws {@code UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException("Removal is not supported");
    }
}
