/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.common;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.text.*; 
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.util.BitSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link Statistics} class.
 */
public class StatisticsTests {

    @Test public void testRandomDistributionUnderHalfSet() {
        for (int size = 10; size < 100; ++size) {
            for (int set = 1; set < size / 2; ++set) {
                BitSet b = Statistics.randomDistribution(set, size);
                assertEquals(set, b.cardinality());
            }
        }        
    }

    @Test public void testRandomDistributionOverHalfSet() {
        for (int size = 10; size < 100; ++size) {
            for (int set = size / 2; set < ((3 * size) / 4); ++set) {
                BitSet b = Statistics.randomDistribution(set, size);
                assertEquals(set, b.cardinality());
            }
        }        
    }

    @Test(expected=IllegalArgumentException.class)
    public void randomDistWrongSize() {
        Statistics.randomDistribution(10, 5);
    }

}