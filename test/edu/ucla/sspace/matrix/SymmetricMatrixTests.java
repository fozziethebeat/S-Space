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

import edu.ucla.sspace.vector.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SymmetricMatrixTests {

    @Test public void testSet() {
        SymmetricMatrix ssm = new SymmetricMatrix(new SparseHashMatrix(10, 10));
        ssm.set(5,1,2);
        assertEquals(2, ssm.get(5,1), .001);
        assertEquals(2, ssm.get(1,5), .001);

        ssm.set(1,5,3);
        assertEquals(3, ssm.get(5,1), .001);
        assertEquals(3, ssm.get(1,5), .001);
    }

    @Test public void testRowVector() {
        SymmetricMatrix ssm = new SymmetricMatrix(new SparseHashMatrix(10, 10));
        ssm.set(5,1,2);
        DoubleVector v = ssm.getRowVector(5);
        assertEquals(2, v.get(1), .001);
        v = ssm.getRowVector(1);
        assertEquals(2, v.get(5), .001);

    }

    @Test public void testColVector() {
        SymmetricMatrix ssm = new SymmetricMatrix(new SparseHashMatrix(10, 10));
        ssm.set(5,1,2);
        DoubleVector v = ssm.getColumnVector(5);
        assertEquals(2, v.get(1), .001);
        v = ssm.getColumnVector(1);
        assertEquals(2, v.get(5), .001);
    }

}
