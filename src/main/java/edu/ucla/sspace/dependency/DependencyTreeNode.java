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

import java.util.List;


/**
 * The interface for a word in a dependency parse tree.
 */
public interface DependencyTreeNode {

    /**
     * Returns the list of neighbors to the current node.  Note that this list
     * include both relations where the current node is the head node and
     * relations where the current node is the dependent.
     */
    List<DependencyRelation> neighbors();

    /**
     * Returns the link from this {@link DependencyTreeNode} to it's parent or
     * {@code null} if no parent exists.
     */
    DependencyRelation parentLink();

    /**
     * Returns the word stored in this node.
     */
    String word();

    /**
     * The lemmatized version of the word, if there is any.
     */
    String lemma();

    /**
     * Returns the part of speech tag for this node.
     */
    String pos();

    /**
     * Returns the index used by this {@link DependencyTreeNode} in an array.
     */
    int index();
}
