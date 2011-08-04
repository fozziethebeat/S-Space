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

import edu.ucla.sspace.vector.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.util.Arrays;


/**
 * Unit tests for the {@link AtomicGrowingSparseHashMatrix} class
 */
public class AtomicGrowingSparseHashMatrixTests {

    @Test public void testMultithreaded() throws Exception {
        final AtomicMatrix m = new AtomicGrowingSparseHashMatrix();
        int numThreads = 10;
        final int updates = 100;
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < threads.length; ++i) 
            threads[i] = new Thread() {
                    public void run() {
                        for (int i = 0; i < updates; ++i)
                            m.addAndGet(1, 1, 1);
                    }
                };
        for (Thread t : threads)
            t.start();
        for (Thread t : threads)
            t.join();
        assertEquals(2, m.rows());
        assertEquals(2, m.columns());
        assertEquals(numThreads * updates, m.get(1, 1), 0.1d);
    }

    @Test public void testResize() {
        Matrix m = new AtomicGrowingSparseHashMatrix();
        assertEquals(0, m.rows());
        assertEquals(0, m.columns());
        m.set(1, 1, 1);
        assertEquals(2, m.rows());
        assertEquals(2, m.columns());
        m.set(1, 2, 1);
        assertEquals(2, m.rows());
        assertEquals(3, m.columns());
        m.set(3, 2, 1);
        assertEquals(4, m.rows());
        assertEquals(3, m.columns());
        m.set(4, 4, 0);
        assertEquals(5, m.rows());
        assertEquals(5, m.columns());
    }

    @Test public void testSet() {
        Matrix m = new AtomicGrowingSparseHashMatrix();
        m.set(100, 100, 0);
        GenericMatrixUtil.testSet(m);
    }

    @Test public void testGet() {
        Matrix m = new AtomicGrowingSparseHashMatrix();
        m.set(100, 100, 0);
        GenericMatrixUtil.testGet(m);
    }

    @Test public void testRowVector() {
        Matrix m = new AtomicGrowingSparseHashMatrix();
        m.set(99, 99, 0);
        for (int i = 0; i < 100; i += 10) {
            for (int j = 0; j < 100; j += 10) {
                System.out.println(i +", " +j);
                m.set(i,j,10);
            }
        }
        assertEquals(100, m.rows());
        assertEquals(100, m.columns());
        for (int i = 0; i < 100; i += 10) {
            DoubleVector v = m.getRowVector(i);
            assertEquals(100, v.length());
            assertEquals(10, v.get(0), 0.01d);
            assertEquals(0, v.get(1), 0.01d);
            System.out.println(v);
            assertEquals(10, v.get(10), 0.01d);
        }
        // check the unset case
        DoubleVector v = m.getRowVector(2);
        assertEquals(100, v.length());
        for (int i = 0; i < 100; ++i)
            assertEquals(0, v.get(i), 0.01d);            
    }

    @Test public void testColumnVector() {
        Matrix m = new AtomicGrowingSparseHashMatrix();
        m.set(99, 99, 0);
        for (int i = 0; i < 100; i += 10) {
            for (int j = 0; j < 100; j += 10) {
                System.out.println(i +", " +j);
                m.set(i,j,10);
            }
        }
        assertEquals(100, m.rows());
        assertEquals(100, m.columns());
        for (int i = 0; i < 100; i += 10) {
            DoubleVector v = m.getColumnVector(i);
            assertEquals(100, v.length());
            assertEquals(10, v.get(0), 0.01d);
            assertEquals(0, v.get(1), 0.01d);
            System.out.println(v);
            assertEquals(10, v.get(10), 0.01d);
        }
        // check the unset case
        DoubleVector v = m.getColumnVector(2);
        assertEquals(100, v.length());
        for (int i = 0; i < 100; ++i)
            assertEquals(0, v.get(i), 0.01d);            
    }

}
