/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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
 * Transforms the dependency tree by adding conjunction relations between head
 * words and a list of words joined by a conjunction.  For example, consider the
 * sentence "There are many types of mammals, such as cats, dogs, and dolphins."
 * If "such as" is the dominate node over the list of animals, starting with
 * "cats", and each animal node is connected to proceeding node with "conj"
 * link, then this method will add a link from "such as" to each of the animal
 * nodes.  Essentially, this bubbles up the conjunction link so that elements in
 * a list are directly connected to the head node.
 *
 * @author Keith Stevens
 */
public class ConjunctionTransform implements DependencyTreeTransform {

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode[] transform(DependencyTreeNode[] dependencyTree) {
        // Add in extra relations for conjunction relations
        for (DependencyTreeNode treeNode : dependencyTree) {
            // Determine whether or not there is a conjunction link connected to
            // this node.  For simplicty, only consider relations where the
            // current node is the parent in a conjunction relation.  Also find
            // the parent relation for this node.
            boolean hasConj = false;
            DependencyRelation parentLink = null;
            for (DependencyRelation link : treeNode.neighbors()) {
                if (link.relation().equals("conj") && 
                    link.headNode().equals(treeNode))
                    hasConj = true;
                if (!link.headNode().equals(treeNode))
                    parentLink = link;
            }

            // Skip any nodes that have no conjunction links or have a missing
            // parent node.
            if (!hasConj || parentLink == null)
                continue;

            for (DependencyRelation link : treeNode.neighbors()) {
                // Find any nodes that are connected through a conjunction
                // with this node.  Add an artifical link between the parent
                // of this node and the "conj" child of this node.
                if (link.relation().equals("conj") &&
                    link.headNode().equals(treeNode)) {
                    DependencyRelation newLink = new SimpleDependencyRelation(
                            parentLink.headNode(), parentLink.relation(), 
                            link.dependentNode());

                    parentLink.headNode().neighbors().add(newLink);
                }
            }
        }

        return dependencyTree;
    }
}
