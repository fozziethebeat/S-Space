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

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SimpleDependencyTreeNodeTest {

    @Test public void testGettersNoLemma() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat", "n");

        assertEquals("cat", node1.word());
        assertEquals("n", node1.pos());
        assertEquals("cat", node1.lemma());
    }

    @Test public void testGetters() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");

        assertEquals("cat", node1.word());
        assertEquals("n", node1.pos());
        assertEquals("c", node1.lemma());
    }

    @Test public void testAddNeighbor() {
        SimpleDependencyTreeNode node1 = new SimpleDependencyTreeNode(
                "cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("dog","n","c");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);
        node1.addNeighbor(rel);

        List<DependencyRelation> relations = node1.neighbors();
        assertEquals(1, relations.size());
        assertEquals(rel, relations.get(0));
    }

    @Test public void testNotEqualsWord() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("dog","n","c");
        assertFalse(node1.equals(node2));
    }

    @Test public void testNotEqualsPos() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","v","c");
        assertFalse(node1.equals(node2));
    }

    @Test public void testNotEqualsLemma() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","t");
        assertFalse(node1.equals(node2));
    }

    @Test public void testEquals() {
        DependencyTreeNode node1 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","c");
        assertEquals(node1, node2);
    }

    @Test public void testEqualsWithRelation() {
        SimpleDependencyTreeNode node1 = new SimpleDependencyTreeNode(
                "cat","n","c");
        DependencyTreeNode node2 = new SimpleDependencyTreeNode("cat","n","c");
        DependencyRelation rel =
            new SimpleDependencyRelation(node1, "c", node2);
        node1.addNeighbor(rel);

        assertEquals(node1, node2);
    }
}
