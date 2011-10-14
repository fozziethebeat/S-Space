/*
 * Copyright 2010 David Jurgens
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

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TreeMultiMap;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Deque;


/**
 * A class for performing a breadth-first iteration of the links in a dependency
 * tree starting at any arbitrary node.  The paths returned will always begin at
 * the specified start node.
 *
 * </p>
 *
 * This class is not thread safe.
 */
public class BreadthFirstPathIterator implements Iterator<DependencyPath> {
    
    /**
     * The paths that have been expanded from the starting node but have not yet
     * been returned.
     */
    protected final Queue<DependencyPath> frontier;
    
    /**
     * Creates a new iterator over all the paths starting at the provided index.
     *
     * @param startNode the node that will start all the paths to be generated.
     */
    public BreadthFirstPathIterator(DependencyTreeNode startNode) {
        frontier = new ArrayDeque<DependencyPath>();

        // Base-case: find all the paths of length 1
        for (DependencyRelation rel : startNode.neighbors()) {
            // Orient the path depending on whether the root was the head of the
            // relationship or not.  This ensures that the root is always the
            // first node in the path and any expansion will continue away from
            // the root.
            SimpleDependencyPath p = new SimpleDependencyPath(
                Collections.singletonList(rel), 
                rel.headNode().equals(startNode));
            frontier.offer(p);
        }
    }

    /**
     * Expands the breadth-first frontier by adding all the new paths one link
     * away to the end of {@code frontier}.
     */
    /* package-private */ void advance(DependencyPath path) {
        // Get the last node and last relation to decide how to expand.
        DependencyTreeNode last = path.last();
        DependencyRelation lastRel = path.lastRelation();
        
        // Expand all of the possible relations from the last node, creating a
        // new path for each, except if the relation is the one that generated
        // this path.
        for (DependencyRelation rel : last.neighbors()) {
            // Skip re-adding the current relation
            if (lastRel.equals(rel))
                continue;
            // Use an extension of the path, rather than having to copy all of
            // the nodes again.  This just creates a view of path with rel as
            // the last relation in path
            DependencyPath extended = new ExtendedPathView(path, rel);
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
        DependencyPath p = frontier.remove();
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