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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SimpleDependencyRelationTest {

    @Test public void testGetters() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);

        assertEquals(node1, rel.headNode());
        assertEquals(node2, rel.dependentNode());
        assertEquals("c", rel.relation());
    }

    @Test public void testEquals() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "c", node2);
        assertEquals(rel1, rel2);
    }

    @Test public void testNotEqualsRelation() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "b", node2);
        assertFalse(rel1.equals(rel2));
    }

    @Test public void testNotEqualsNode2() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyTreeNode node3 = new SimpleDependencyTreeNode("dog", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node1, "c", node3);

        assertFalse(rel1.equals(rel2));
    }

    @Test public void testNotEqualsNode1() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("doc", "n");
        DependencyTreeNode node3 = new SimpleDependencyTreeNode("dog", "n");
        DependencyRelation rel1 =
            new SimpleDependencyRelation(node1, "c", node2);
        DependencyRelation rel2 =
            new SimpleDependencyRelation(node3, "c", node2);

        assertFalse(rel1.equals(rel2));
    }
}

