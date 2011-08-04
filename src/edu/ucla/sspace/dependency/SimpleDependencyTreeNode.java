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

import java.util.LinkedList;
import java.util.List;

/**
 * A default implementation of a {@link DependencyTreeNode} that allows
 * mutating access to the list of neighbors.
 */
public class SimpleDependencyTreeNode implements DependencyTreeNode {

    /**
     * The node's token.
     */
    private String word;

    /**
     * The node's part of speech tag.
     */
    private String pos;

    /**
     * The list of neighbors of this node.
     */
    private List<DependencyRelation> neighbors;

    /**
     * Creates a new {@link SimpleDependencyTreeNode} node for the provided
     * word, with the provided parent link.  Initially the children list is
     * empty.
     */
    public SimpleDependencyTreeNode(String word, String pos) {
        neighbors = new LinkedList<DependencyRelation>();
        if (word == null || pos == null)
            throw new NullPointerException("Arguments must be non-null");
        this.word = word;
        this.pos = pos;
    }


    /**
     * Adds a relation from node to another node.
     *
     * @param relation a relation connecting this node to another
     */
    public void addNeighbor(DependencyRelation relation) {
        // NB: this method is currently package-private to ensure that a user
        // doesn't add a relation that isn't connected to the current node
        // NB: Changed to being public, since this is currently the only way for
        // other libraries to build up dependency trees.
        neighbors.add(relation);
    }

    public boolean equals(Object o) {
        if (o instanceof SimpleDependencyTreeNode) {
            SimpleDependencyTreeNode n = (SimpleDependencyTreeNode)o;
            return pos.equals(n.pos)
                && word.equals(n.word);
            // NOTE: testing for neighbor equality is important, i.e.
            // neighbors.equals(n.neighbors); however, both classes .equal()
            // method call the others, resulting in mutual recursion and an
            // infinite loop.  Therefore, we don't test for neighbor equality.
        }
        return false;
    }

    public int hashCode() {
        return pos.hashCode() ^ word.hashCode();
    }

    public List<DependencyRelation> neighbors() {
        return neighbors;
    }

    /**
     * {@inheritDoc}
     */
    public String pos() {
        return pos;
    }

    /**
     * Sets the word contained by this node.  This method is provided for
     * updating the parse tree's node contents after construction.
     */ 
    void setWord(String word) {
        this.word = word;
    }

    public String toString() {
        return word + ":" + pos;
    }
    
    /**
     * {@inheritDoc}
     */
    public String word() {
        return word;
    }
}
