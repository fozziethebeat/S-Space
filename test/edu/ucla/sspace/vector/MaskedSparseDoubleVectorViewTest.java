/*
 * Copyright 2010 Keith Stevens 
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

import edu.ucla.sspace.util.HashBiMap;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;


/**
 * Tests for the {@link MaskedSparseDoubleVectorView} class.
 */
public class MaskedSparseDoubleVectorViewTest {

    @Test public void testScaledCreate() {
        double[] values = new double[] {1, 0, 3, 4, 5, 6, 7, 8, 9, 10};
        SparseDoubleVector v = new SparseHashDoubleVector(10);
        for (int i = 0; i < values.length; ++i)
            if (values[i] != 0)
                v.set(i, values[i]);

        int[] mask = new int[3];
        mask[0] = 5;
        mask[1] = 9;
        mask[2] = 1;
        Map<Integer, Integer> reverseMask = new HashMap<Integer, Integer>();
        reverseMask.put(5, 0);
        reverseMask.put(0, 1);
        reverseMask.put(1, 2);
        SparseDoubleVector masked = new MaskedSparseDoubleVectorView(
                v, mask, reverseMask);

        assertEquals(mask.length, masked.length());
        assertEquals(values[5], masked.get(0), .00001);
        assertEquals(values[9], masked.get(1), .00001);
        assertEquals(values[1], masked.get(2), .00001);

        int[] nonZeroIndices = masked.getNonZeroIndices();
        System.out.println(VectorIO.toString(nonZeroIndices));
    }
}
