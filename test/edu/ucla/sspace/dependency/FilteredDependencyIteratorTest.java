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

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class FilteredDependencyIteratorTest extends PathIteratorTestBase {

    @Test public void testIteratorUniversalAcceptor() {
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {}
        };

        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        DependencyPathAcceptor acceptor = new UniversalPathAcceptor();
        Iterator<DependencyPath> pathIter = new FilteredDependencyIterator(
                tree[1], acceptor);

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 3, "obj", "is", "chicken");

        assertFalse(pathIter.hasNext());
    }

    @Test public void testIteratorWithInfPath() {
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {3}
        };

        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        DependencyPathAcceptor acceptor = new UniversalPathAcceptor();
        Iterator<DependencyPath> pathIter = new FilteredDependencyIterator(
                tree[1], acceptor, 2);
        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertFalse(pathIter.hasNext());
    }

    @Test public void testIteratorUniversalAcceptorWithMaxLength() {
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {}
        };

        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        DependencyPathAcceptor acceptor = new UniversalPathAcceptor();
        Iterator<DependencyPath> pathIter = new FilteredDependencyIterator(
                tree[1], acceptor, 2);

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertFalse(pathIter.hasNext());
    }

    @Test public void testIteratorWithMockAcceptor() {
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"},
            {"band", "conj", "blah"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {5},
            {3}
        };

        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        DependencyPathAcceptor acceptor = new MockAcceptor();
        Iterator<DependencyPath> pathIter = new FilteredDependencyIterator(
                tree[1], acceptor, 3);

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "band");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 3, "obj", "is", "and");

        assertFalse(pathIter.hasNext());
    }

    private class MockAcceptor implements DependencyPathAcceptor {

        public boolean accepts(DependencyPath path) {
            for (DependencyRelation rel : path)
                if (rel.relation().equals("2"))
                    return false;
            return true;
        }

        public int maxPathLength() {
            return Integer.MAX_VALUE;
        }
    }
}
