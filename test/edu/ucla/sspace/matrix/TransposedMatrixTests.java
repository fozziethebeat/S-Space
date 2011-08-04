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

package edu.ucla.sspace.matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TransposedMatrixTests {

    @Test public void testSize() {
        Matrix m = new ArrayMatrix(
            new double[][] {
            { 0.89453775,  0.27084422,  0.20789784, 1.3732798, 1.5811776 },
            {-1.4221323, -0.8210684, -0.52053654, 0.8210684, 0.3005319}});
        
        assertEquals(2, m.rows());
        assertEquals(5, m.columns());

        Matrix t = new TransposedMatrix(m);
        assertEquals(5, t.rows());
        assertEquals(2, t.columns());        
    }

    @Test public void testGet() {
        Matrix m = new ArrayMatrix(
            new double[][] {
            { 0.89453775,  0.27084422,  0.20789784, 1.3732798, 1.5811776 },
            {-1.4221323, -0.8210684, -0.52053654, 0.8210684, 0.3005319}});
        
        assertEquals(2, m.rows());
        assertEquals(5, m.columns());

        Matrix t = new TransposedMatrix(m);
        assertEquals(m.get(0,0), t.get(0,0), 0.001);
        assertEquals(m.get(1,1), t.get(1,1), 0.001);
        assertEquals(m.get(0,3), t.get(3,0), 0.001);
        assertEquals(m.get(1,3), t.get(3,1), 0.001);
        assertEquals(m.get(1,4), t.get(4,1), 0.001);
    }
}
