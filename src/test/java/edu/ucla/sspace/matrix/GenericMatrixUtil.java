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

package edu.ucla.sspace.matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * A general purpose class for testing {@link Matrix} instances.
 */
public class GenericMatrixUtil {

    public static void testSet(Matrix m) {
        int rows = m.rows();
        int cols = m.columns();
        for (int trials = 0; trials < 100; ++trials) {
            int i = (int)(rows * Math.random());
            int j = (int)(cols * Math.random());
            double v = Math.random();
            m.set(i, j, v);
            assertEquals(v, m.get(i, j), 0.01);
      }      
    }
    
    public static void testGet(Matrix m) {
        int rows = m.rows();
        int cols = m.columns();
        for (int trials = 0; trials < 100; ++trials) {
            int i = (int)(rows * Math.random());
            int j = (int)(cols * Math.random());
            double v = m.get(i, j);
            double r = Math.random();
            m.set(i, j, r);
            m.set(i, j, v);
            assertEquals(v, m.get(i, j), 0.01);
        }
    }
}
