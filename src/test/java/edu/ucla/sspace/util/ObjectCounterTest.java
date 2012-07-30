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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of unit tests for {@link ObjectCounter} 
 */
public class ObjectCounterTest {
   
    @Test public void testCount() {
        Counter<Integer> c = new ObjectCounter<Integer>();
        c.count(1);
        assertEquals(1, c.sum());
        assertEquals(1, c.items().size());
        assertEquals(1, c.getCount(1));

        c.count(1);
        assertEquals(2, c.sum());
        assertEquals(1, c.items().size());
        assertEquals(2, c.getCount(1));

        c.count(2);
        assertEquals(3, c.sum());
        assertEquals(2, c.items().size());
        assertEquals(1, c.getCount(2));
    }

    @Test public void testMax() {
        Counter<Integer> c = new ObjectCounter<Integer>();
        c.count(5);
        c.count(5);
        c.count(3);
        assertEquals(5, c.max().intValue());
    }
}