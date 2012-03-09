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


/**
 * A simple {@link DependencyRelation} implementation holds both related nodes
 * and their relation.
 */
public class SimpleDependencyRelation implements DependencyRelation {

    /**
     * The dependent pnode in the relation.
     */
    private final DependencyTreeNode dependent;

    /**
     * The head node in the relation.
     */
    private final DependencyTreeNode headNode;

    /**
     * The relation string.
     */
    private final String relation;

    /**
     * Creates a {@link SimpleDependencyRelation}.
     */
    public SimpleDependencyRelation(DependencyTreeNode headNode,
                                    String relation,
                                    DependencyTreeNode dependent) {
        this.headNode = headNode;
        this.relation = relation;
        this.dependent = dependent;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode dependentNode() {
        return dependent;
    }

    public boolean equals(Object o) {
        if (o instanceof SimpleDependencyRelation) {
            SimpleDependencyRelation r = (SimpleDependencyRelation)o;
            return headNode.equals(r.headNode) 
                && relation.equals(r.relation)
                && dependent.equals(r.dependent);            
        } 
        return false;
    }

    public int hashCode() {
        return relation.hashCode() ^ headNode.hashCode() ^ dependent.hashCode();
    }
    
    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode headNode() {
        return headNode;
    }

    /**
     * {@inheritDoc}
     */
    public String relation() {
        return relation;
    }

    public String toString() {
        return headNode + "<-" + relation + "--" + dependent;
    }
}
