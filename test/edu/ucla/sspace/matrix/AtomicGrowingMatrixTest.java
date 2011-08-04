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

package edu.ucla.sspace.matrix;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;

public class AtomicGrowingMatrixTest {


    public AtomicGrowingMatrixTest() { }

//     @Test public void testReplaceWithZero() {
// 	double[][] testData = {{0, 0, 0, 1},
// 			       {1, 0, 2, 0},
// 			       {0, 0, 0, 0},
// 			       {1, 2, 4, 5},
// 			       {0, 0, 1, 0}};
// 	AtomicGrowingMatrix testMatrix = new AtomicGrowingMatrix();
// 	for (int i = 0; i < 5; ++i) {
// 	    for (int j = 0; j < 4; ++j) {
// 		testMatrix.set(i, j, testData[i][j]);
// 	    }
// 	}
// 	testData[1][1] = 4;
// 	testMatrix.set(1, 1, 4);
// 	testData[1][0] = 0;
// 	testMatrix.set(1, 0, 0);
// 	for (int i = 0; i < 5; ++i) {
// 	    for (int j = 0; j < 4; ++j) {
// 		assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
// 	    }
// 	}
//     }

//     @Test public void testMatrixSet() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	m.set(0,0,1);
// 	assertEquals(1, m.get(0,0), 0.001);
//     }

//     @Test public void testMatrixSetRepeated() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	m.set(0,0,1);
// 	assertEquals(1, m.get(0,0), 0.001);
// 	m.set(0,0,2);
// 	assertEquals(2, m.get(0,0), 0.001);
//     }

//     @Test public void testMatrixSetExpanded() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	m.set(0,0,1);
// 	assertEquals(1, m.get(0,0), 0.001);
// 	m.set(1,1,2);
// 	assertEquals(2, m.get(1,1), 0.001);
//     }

//     @Test public void testMatrixSetExpandedWithSkip() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	m.set(1,1,1);
// 	assertEquals(1, m.get(1,1), 0.001);
// 	m.set(0,0,2);
// 	assertEquals(2, m.get(0,0), 0.001);
//     }

//     @Test public void testMatrixSetZero() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	m.set(1,1,1);
// 	assertEquals(1, m.get(1,1), 0.001);
// 	m.set(1,1,0);
// 	assertEquals(0, m.get(0,0), 0.001);
//     }

//     @Test public void testMatrixSetZeroIntermediate() {
// 	AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	double[] d = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9};
// 	m.setRow(0, d);
// 	for (int i = 0; i < d.length; ++i) {
// 	    assertEquals(d[i], m.get(0,i), 0.001);
// 	}
// 	for (int i = 0; i < d.length; i += 2) {
// 	    d[i] = 0;
// 	    m.set(0,i,0);
// 	}
// 	for (int i = 0; i < d.length; ++i) {
// 	    assertEquals(d[i], m.get(0,i), 0.001);
// 	}
//     }    

//     @Test public void testMatrix() {
// 	double[][] testData = {{0, 0, 0, 1},
// 			       {1, 0, 2, 0},
// 			       {0, 0, 0, 0},
// 			       {1, 2, 4, 5},
// 			       {0, 0, 1, 0}};
// 	AtomicGrowingMatrix testMatrix = new AtomicGrowingMatrix();
// 	for (int i = 0; i < 5; ++i) {
// 	    for (int j = 0; j < 4; ++j) {
// 		testMatrix.set(i, j, testData[i][j]);
// 	    }
// 	}
// 	for (int i = 0; i < 5; ++i) {
// 	    for (int j = 0; j < 4; ++j) {
// 		assertEquals(testData[i][j], testMatrix.get(i, j), .0001);
// 	    }
// 	}
//     }

//     @Test public void testConcurrentSetRow() {
// 	final AtomicGrowingMatrix m = new AtomicGrowingMatrix();
// 	final double[] d1 = new double[9];
// 	final double[] d2 = new double[9];
// 	for (int i = 0; i < 9; ++i) {
// 	    d1[i] = i;
// 	    d2[i] = -i;
// 	}
// 	Thread t1 = new Thread() {
// 		public void run() {
// 		    for (int i = 0; i < 1000; ++i)
// 			m.setRow(0, d1);
// 		}
// 	    };
// 	Thread t2 = new Thread() {
// 		public void run() {
// 		    for (int i = 0; i < 1000; ++i)
// 			m.setRow(0, d2);
// 		}
// 	    };

// 	t1.start();
// 	t2.start();

// 	// must equal one or the other, but never a mix of the two;
// 	assertTrue(Arrays.equals(d1, m.getRow(0)) ||
// 		   Arrays.equals(d2, m.getRow(0)));
//     }

    @Test public void testConcurrentAddAndGet() throws Exception {
	final AtomicGrowingMatrix m = new AtomicGrowingMatrix();

	Thread t1 = new Thread() {
		public void run() {
		    for (int i = 0; i < 100; ++i)
			m.addAndGet(0,0,1);
		}
	    };
	Thread t2 = new Thread() {
		public void run() {
		    for (int i = 0; i < 100; ++i)
			m.addAndGet(0,0,1);
		}
	    };

	t1.start();
	t2.start();

	t1.join();
	t2.join();
	// must equal one or the other, but never a mix of the two;
	assertEquals(200, m.get(0,0), .001);
    }

    @Test public void testConcurrentGetAndAdd() throws Exception {
	final AtomicGrowingMatrix m = new AtomicGrowingMatrix();

	Thread t1 = new Thread() {
		public void run() {
		    for (int i = 0; i < 100; ++i)
			m.getAndAdd(0,0,1);
		}
	    };
	Thread t2 = new Thread() {
		public void run() {
		    for (int i = 0; i < 100; ++i)
			m.getAndAdd(0,0,1);
		}
	    };

	t1.start();
	t2.start();

	t1.join();
	t2.join();
	// must equal one or the other, but never a mix of the two;
	assertEquals(200, m.get(0,0), .001);
    }
}
