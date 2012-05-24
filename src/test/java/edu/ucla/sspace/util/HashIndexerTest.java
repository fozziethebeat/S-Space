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

import java.io.*;

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
 * A collection of unit tests for {@link HashIndexer} 
 */
public class HashIndexerTest {

    @Test public void testIndex() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        assertEquals(0, h.size());
        for (int i = 0; i < 10; ++i) {
            h.index(i);
            assertEquals(i + 1, h.size());
            assertEquals(i, h.index(i));
        }
    }

    @Test public void testFind() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        assertEquals(0, h.size());
        for (int i = 0; i < 10; ++i) {
            h.index(i);
            assertEquals(i + 1, h.size());
            assertEquals(i, h.index(i));
        }
        assertTrue(h.find(10) < 0);
        assertEquals(10, h.size());        
    }

    @Test public void testLookup() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        assertEquals(0, h.size());
        for (int i = 0; i < 10; ++i) {
            h.index(i);
            assertEquals(i + 1, h.size());
            assertEquals(i, h.index(i));
        }
        
        for (int i = 0; i < 10; ++i) {
            assertEquals(i, h.lookup(i).intValue());
        }
    }

    @Test public void testLookupRecomputed() {
        HashIndexer<Integer> h = new HashIndexer<Integer>();
        assertEquals(0, h.size());
        for (int i = 0; i < 5; ++i) {
            h.index(i);
            assertEquals(i + 1, h.size());
            assertEquals(i, h.index(i));
        }
        
        for (int i = 0; i < 5; ++i) {
            assertEquals(i, h.lookup(i).intValue());
        }

        for (int i = 5; i < 10; ++i) {
            h.index(i);
            assertEquals(i + 1, h.size());
            assertEquals(i, h.index(i));
        }
        
        for (int i = 0; i < 5; ++i) {
            assertEquals(i, h.lookup(i).intValue());
        }
    }
}