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

package edu.ucla.sspace.basis;

import java.util.Set;

import org.junit.*;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class StringBasisMappingTest {

    @Test public void testGetDimension() {
        StringBasisMapping basis = new StringBasisMapping();
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("dog"));
        assertEquals(2, basis.getDimension("c"));
        assertEquals(0, basis.getDimension("cat"));
    }

    @Test public void testNumDimensions() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");
        assertEquals(3, basis.numDimensions());
    }

    @Test public void testKeySet() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");
        Set<String> keySet = basis.keySet();
        assertTrue(keySet.contains("cat"));
        assertTrue(keySet.contains("c"));
        assertTrue(keySet.contains("at"));
    }

    @Test public void testReadOnly() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("c");
        basis.getDimension("at");

        basis.setReadOnly(true);
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("c"));
        assertEquals(2, basis.getDimension("at"));
        assertEquals(-1, basis.getDimension("blah"));
        assertTrue(basis.isReadOnly());

        basis.setReadOnly(false);
        assertEquals(0, basis.getDimension("cat"));
        assertEquals(1, basis.getDimension("c"));
        assertEquals(2, basis.getDimension("at"));
        assertEquals(3, basis.getDimension("blah"));
        assertFalse(basis.isReadOnly());
    }

    @Test public void testGetDimensionDescription() {
        StringBasisMapping basis = new StringBasisMapping();
        basis.getDimension("cat");
        basis.getDimension("ca");
        basis.getDimension("dog");

        assertEquals("cat", basis.getDimensionDescription(0));
        assertEquals("ca", basis.getDimensionDescription(1));
        assertEquals("dog", basis.getDimensionDescription(2));
    }
}
