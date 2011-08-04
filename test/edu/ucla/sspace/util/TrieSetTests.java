/*
 * Copyright 2009 David Jurgens
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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link TrieSet} 
 */
public class TrieSetTests {

    @Test public void testConstructor() {
 	Set<String> m = new TrieSet();
    }

    @Test public void testCollectionConstructor() {
        Set<String> control = new HashSet<String>();
        for (int i = 0; i < 10; ++i)
            control.add(String.valueOf(i));
 	Set<String> m = new TrieSet(control);
        assertTrue(control.equals(m));
    }

    @Test public void testAdd() {
 	Set<String> m = new TrieSet();
        assertFalse(m.contains("foo"));
        m.add("foo");
        assertTrue(m.contains("foo"));
    }

    @Test public void testRemove() {
 	Set<String> m = new TrieSet();
        assertFalse(m.contains("foo"));
        m.add("foo");
        assertTrue(m.contains("foo"));
        m.remove("foo");
        assertFalse(m.contains("foo"));
    }

    @Test public void testIterator() {
        Set<String> control = new HashSet<String>();
 	Set<String> test = new TrieSet();
        for (int i = 0; i < 10; ++i)
            test.add(String.valueOf(i));
        for (String s : test)
            control.add(s);
        assertTrue(control.equals(test));
    }


}
