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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
public class DependencyIterator implements Iterator<DependencyPath> {

    /**
     * The maximum length of the returned paths.  The length is considedered to
     * not include the first term.
     */
    private final int maxPathLength;

    /**
     * The paths that have been expanded from the starting node but have not yet
     * been returned.
     */
    protected final Queue<SimpleDependencyPath> frontier;

    /**
     * The {@link DependencyRelationAcceptor} that validates each link before it is
     * traversed and returned as part of a {@link DependencyPath}.
     */
    private DependencyRelationAcceptor acceptor;

    /**
     * Creates a new {@link DependencyIterator} that will return all {@link
     * DependencyPath}s rooted at the term with index {@code startTerm}.  Each
     * link in the path will be valied with {@code acceptor} and weighted with
     * {@code weighter}.  Each path will have length 1 + {@code maxPathLength}.
     *
     * @param startNode the node that will start all the paths to be generated.
     * @param acceptor The {@link DependencyRelationAcceptor} that will validate
     *        each link the a path
     * @param maxPathLength the maximum length of any path returned
     * 
     * @throws IllegalArgumentException if {@code maxPathLength} is less than 1
     */
    public DependencyIterator(DependencyTreeNode startNode,
                              DependencyRelationAcceptor acceptor,
                              int maxPathLength) {
        if (maxPathLength < 1)
            throw new IllegalArgumentException(
                "Must specify a path length greater than or equal to 1");
        this.maxPathLength = maxPathLength;
        this.acceptor = acceptor;
        frontier = new ArrayDeque<SimpleDependencyPath>();

        // Base-case: find all the paths of length 1
        for (DependencyRelation rel : startNode.neighbors()) {
            // Orient the path depending on whether the root was the head of the
            // relationship or not.  This ensures that the root is always the
            // first node in the path and any expansion will continue away from
            // the root.
            if (acceptor.accept(rel)) {
                frontier.offer(new SimpleDependencyPath(
                               rel, rel.headNode().equals(startNode)));
            }
        }
    }

    /**
     * Extends the path in its growth direction and adds to the frontier those
     * relations that are shorter than the maximum path length and that are
     * accepted by the {@code DependencyRelationAcceptor} used by this instance.
     */
    void advance(SimpleDependencyPath path) {
        if (path.length() >= maxPathLength)
            return;

        // Get the last node and last relation to decide how to expand.
        DependencyRelation lastRelation = path.lastRelation();
        DependencyTreeNode last = path.last(); 
        
        // Expand all of the possible relations from the last node, creating a
        // new path for each, except if the relation is the one that generated
        // this path.
        for (DependencyRelation rel : last.neighbors()) {
            // Skip re-adding the current relation
            if (lastRelation.equals(rel) || !acceptor.accept(rel))
                continue;
            SimpleDependencyPath extended = path.extend(rel);
            frontier.offer(extended);
        }
    }

    /**
     * Returns {@code true} if there are still paths to return for the tree.
     */
    public boolean hasNext() {
        return !frontier.isEmpty();
    }

    /**
     * Returns the next {@code DependencyPath} in the tree whose length is equal
     * or greater than the previously returned path.
     */
    public DependencyPath next() {
        SimpleDependencyPath p = frontier.remove();
        // Expand the frontier 1 link starting from the current path
        advance(p);
        return p;
    }

    /**
     * Throws an {@code UnsupportedOperationException} if called
     */
    public void remove() {
        throw new UnsupportedOperationException("Removal is not possible");
    }
}
