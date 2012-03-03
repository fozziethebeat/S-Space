/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.util;

import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link CombinedSet} class
 */
public class CombinedSetTests { 

    @Test public void testConstructor() {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> combined = new CombinedSet<Integer>(Collections.singleton(set1));
    }

    @Test public void testContainsOnDisjointSets() {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> set2 = new HashSet<Integer>();
        for (int i = 0; i < 5; ++i) {
            set1.add(i);
            set2.add(i+5);
        }
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(set1);
        sets.add(set2);
        Set<Integer> combined = new CombinedSet<Integer>(sets);
        
        for (int i = 0; i < 10; ++i)
            assertTrue(combined.contains(i));
    }

    @Test public void testContainsOnOverlappingSets() {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> set2 = new HashSet<Integer>();
        for (int i = 0; i < 5; ++i) {
            set1.add(i);
            set2.add(i+1);
        }
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(set1);
        sets.add(set2);
        Set<Integer> combined = new CombinedSet<Integer>(sets);
        
        for (int i = 0; i < 6; ++i)
            assertTrue(combined.contains(i));
    }


    @Test public void testSizeOnDisjointSets() {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> set2 = new HashSet<Integer>();
        for (int i = 0; i < 5; ++i) {
            set1.add(i);
            set2.add(i+5);
        }
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(set1);
        sets.add(set2);
        Set<Integer> combined = new CombinedSet<Integer>(sets);
        
        assertEquals(10, combined.size());
    }

    @Test public void testSizeOnOverlappingSets() {
        Set<Integer> set1 = new HashSet<Integer>();
        Set<Integer> set2 = new HashSet<Integer>();
        for (int i = 0; i < 5; ++i) {
            set1.add(i);
            set2.add(i+1);
        }
        List<Set<Integer>> sets = new ArrayList<Set<Integer>>();
        sets.add(set1);
        sets.add(set2);
        Set<Integer> combined = new CombinedSet<Integer>(sets);
        
        assertEquals(6, combined.size());
    }

}
