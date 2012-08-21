/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.vector;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests for the {@link SparseHashDoubleVector} class.
 */
public class AbstractDoubleVectorTest {

    @Test public void testEquals() {
        DoubleVector v1 = new AbstractDoubleVector() {
                public int length() { return 10; }
                public double get(int i) { return 1; }
            };
        DoubleVector v2 = new AbstractDoubleVector() {
                public int length() { return 10; }
                public double get(int i) { return 1; }
            };
        DoubleVector v3 = new AbstractDoubleVector() {
                public int length() { return 11; }
                public double get(int i) { return 1; }
            };
        DoubleVector v4 = new AbstractDoubleVector() {
                public int length() { return 10; }
                public double get(int i) { return 2; }
            };

        assertEquals(v1, v2);
        assertFalse(v1.equals(v3));
        assertFalse(v1.equals(v4));
        assertFalse(v3.equals(v4));
    }
}