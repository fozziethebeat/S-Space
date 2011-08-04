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


/**
 * An interface for representing the nodes and their relations in a dependency
 * path.
 */
public interface DependencyPath extends Iterable<DependencyRelation> {

    /**
     * Returns the first node in the path, which is closest to the root.
     */
    DependencyTreeNode first();

    /**
     * Returns the relation that connects the first and second nodes in the
     * path, which are closest to the root.
     */
    DependencyRelation firstRelation();

    /**
     * Returns the node location at the specified position along the path.
     */
    DependencyTreeNode getNode(int position);

    /**
     * Returns the relation connecting the node at the specified position to the
     * next node.  Note that for the last node in the path, no relation exists,
     * so there are at most {@code length() - 1} relations in the path.
     *
     * @throws IllegalArgumentException if {@code position} is less than 0 or
     * {@code position} is greater than {@code length() - 1}.
     */
    String getRelation(int position);

    /**
     * Returns an iterator over all the relations in the path in order from
     * closest to the root to furthest.
     */
    Iterator<DependencyRelation> iterator();

    /**
     * Returns the last node in the path, which is furthest from the root.
     */
    DependencyTreeNode last();

    /**
     * Returns the relation that connects the last and second to last nodes in
     * the path, which are furthest from the root.
     */
    DependencyRelation lastRelation();
    
    /**
     * Returns the number of nodes in the dependency path.  The number of
     * relations in the path will be {@code length() - 1}.
     */
    int length();
}
