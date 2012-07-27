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
 * A collection of unit tests for {@link HashBiMap} 
 */
public class HashBiMapTest {

    @Test public void testPut() {
        BiMap<Integer,Integer> m = new HashBiMap<Integer,Integer>();
        for (int i = 0; i < 10; ++i) {
            m.put(i, 10-i);
        }
        for (int i = 0; i < 10; ++i) {
            assertEquals(Integer.valueOf(10 - i), m.get(i));
        }
    }

    @Test public void testInverse() {
        BiMap<Integer,Integer> m = new HashBiMap<Integer,Integer>();
        for (int i = 0; i < 10; ++i) {
            m.put(i, 10-i);
        }
        m = m.inverse();
        for (int i = 0; i < 10; ++i) {
            assertEquals(Integer.valueOf(i), m.get(10 - i));
        }
    }

    @Test public void testSerialize() throws Exception {
        BiMap<Integer,Integer> m = new HashBiMap<Integer,Integer>();
        for (int i = 0; i < 10; ++i) {
            m.put(i, 10-i);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(m);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream ois = new ObjectInputStream(bais);
        @SuppressWarnings("unchecked")
        BiMap<Integer,Integer> test =
            (BiMap<Integer,Integer>)(ois.readObject());

        assertEquals(10, m.size());

        for (int i = 0; i < 10; ++i) {
            assertEquals(Integer.valueOf(10 - i), m.get(i));
        }
        m = m.inverse();

        for (int i = 0; i < 10; ++i) {
            assertEquals(Integer.valueOf(i), m.get(10 - i));
        }
    }

}