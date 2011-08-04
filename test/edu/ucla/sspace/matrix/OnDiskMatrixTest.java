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

public class OnDiskMatrixTest {

    @Test public void testConstructor() {
	new OnDiskMatrix(1,1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstructorIllegalSize() {
	new OnDiskMatrix(0, 0);
    }

    @Test public void testSet() {
	Matrix m = new OnDiskMatrix(1,1);
	assertEquals(0, m.get(0,0), 0);
	m.set(0,0,1);
	assertEquals(1, m.get(0,0), 0);
	m.set(0,0,2);
	assertEquals(2, m.get(0,0), 0);
    }

    @Test public void testSetOn2D() {
	Matrix m = new OnDiskMatrix(3,3);
	
	for (int i = 0; i < 3; ++i) {
	    for (int j = 0; j < 3; ++j) {
		m.set(i, j, (i+1) * (j+1));
	    }
	}

	for (int i = 0; i < 3; ++i) {
	    for (int j = 0; j < 3; ++j) {
		assertEquals((i+1) * (j+1), m.get(i, j), 0);
	    }
	}
    }

    @Test public void getRow1D() {
	int length = 16;

	Matrix m = new OnDiskMatrix(1,length);
	
	for (int i = 0; i < length; ++i) {
	    m.set(0, i, i);
	}

	double[] row = m.getRow(0);
	assertEquals(length, row.length);
	
	for (int j = 0; j < row.length; ++j) {
	    assertEquals(j, row[j], 0);
	}
    }
    

    @Test public void getRow() {
	Matrix m = new OnDiskMatrix(3,3);
	
	for (int i = 0; i < 3; ++i) {
	    for (int j = 0; j < 3; ++j) {
		m.set(i, j, (i+1) * (j+1));
	    }
	}

	for (int i = 0; i < 3; ++i) {
	    double[] row = m.getRow(i);	
	    assertEquals(3, row.length);

	    for (int j = 0; j < 3; ++j) {
		assertEquals((i+1) * (j+1), row[j], 0);
	    }
	}
    }

    @Test public void setRow2D() {
	OnDiskMatrix m = new OnDiskMatrix(3,3);
	
	for (int i = 0; i < 3; ++i) {
	    for (int j = 0; j < 3; ++j) {
		m.set(i, j, (i+1) * (j+1));
	    }
	}
	
	double[] d = new double[] { 9, 9, 9 };
	m.setRow(1, d);
	double[] row = m.getRow(1);	
	assertEquals(3, row.length);

	for (int j = 0; j < 3; ++j) {
	    assertEquals(9, row[j], 0);
	}	
    }

    @Test public void setRow1D() {
	int length = 16;

	OnDiskMatrix m = new OnDiskMatrix(1,length);
	
	for (int i = 0; i < length; ++i) {
	    m.set(0, i, i);
	}

	double[] d = new double[length];
	for (int i = 0; i < length; ++i) {
	    d[i] = 99;
	}
	m.setRow(0, d);

	double[] row = m.getRow(0);
	assertEquals(length, row.length);
	
	for (int j = 0; j < row.length; ++j) {
	    assertEquals(99, row[j], 0);
	}
    }


}