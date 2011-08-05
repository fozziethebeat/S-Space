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
public class SimpleDependencyPathTest extends AbstractPathTestBase {

    @Test public void testLength() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        assertEquals(3, path.length());
    }

    @Test public void testFirst() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        assertEquals("cat", path.first().word());
        assertEquals("n", path.first().pos());
    }

    @Test public void testFirstRelation() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        DependencyRelation rel = path.firstRelation();
        assertEquals("SBJ", rel.relation());
        assertEquals("cat", rel.headNode().word());
        assertEquals("dog", rel.dependentNode().word());
    }

    @Test public void testLast() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        assertEquals("pig", path.last().word());
        assertEquals("n", path.last().pos());
    }

    @Test public void testFirstRelationRelation() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        DependencyRelation rel = path.lastRelation();
        assertEquals("noarelation", rel.relation());
        assertEquals("whale", rel.headNode().word());
        assertEquals("pig", rel.dependentNode().word());
    }

    @Test public void testGetRelation() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        assertEquals("SBJ", path.getRelation(0));
        assertEquals("OBJ", path.getRelation(1));
        assertEquals("noarelation", path.getRelation(2));
    }

    @Test public void testGetNode() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        assertEquals("cat", path.getNode(0).word());
        assertEquals("dog", path.getNode(1).word());
        assertEquals("whale", path.getNode(2).word());
        assertEquals("pig", path.getNode(3).word());
    }

    @Test public void testIterator() {
        String[][] pathString = {{"cat", "n", "SBJ", "dog", "n"},
                                 {"dog", "n", "OBJ", "whale", "n"},
                                 {"whale", "n", "noarelation", "pig", "n"}};
        DependencyPath path = makePath(pathString);
        Iterator<DependencyRelation> relIter = path.iterator();
        assertTrue(relIter.hasNext());
        assertEquals("SBJ", relIter.next().relation());
        assertEquals("OBJ", relIter.next().relation());
        assertEquals("noarelation", relIter.next().relation());
        assertFalse(relIter.hasNext());
    }
}
