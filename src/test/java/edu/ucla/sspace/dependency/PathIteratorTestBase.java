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

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PathIteratorTestBase {

    public static DependencyTreeNode[] makeTree(String[][] treeData,
                                                int[][] treeLinks) {
        SimpleDependencyTreeNode[] tree =
            new SimpleDependencyTreeNode[treeData.length];
        for (int i = 0; i < tree.length; ++i)
            tree[i] = new SimpleDependencyTreeNode(
                    treeData[i][0], treeData[i][1]);

        for (int i = 0; i < tree.length; ++i)
            for (int n : treeLinks[i])
                tree[i].addNeighbor(new SimpleDependencyRelation(
                            tree[i], treeData[i][2], tree[n]));
        return tree;
    }

    public static void testPath(DependencyPath path,
                                int expectedLength,
                                String expectedRelation,
                                String expectedFirst,
                                String expectedLast) {
        assertEquals(expectedLength, path.length());
        assertEquals(expectedRelation, path.getRelation(0));
        assertEquals(expectedFirst, path.first().word());
        assertEquals(expectedLast, path.last().word());
    }
}

