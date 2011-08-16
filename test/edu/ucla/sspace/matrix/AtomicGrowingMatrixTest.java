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
