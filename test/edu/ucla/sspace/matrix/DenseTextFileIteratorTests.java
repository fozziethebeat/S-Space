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

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class DenseTextFileIteratorTests {

    private static final double[][] testMatrix = {
        {1.5, 0.0, 2.5},
        {0.0, 1.1, 3.1},
        {1.1, 3.3, 4.1}};
    
    @Test(expected=UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        File f = getDenseTextFile();
        Iterator<MatrixEntry> it = new DenseTextFileIterator(f);
        it.remove();
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyNext() throws Exception {
        File f = getDenseTextFile();
        Iterator<MatrixEntry> it = new DenseTextFileIterator(f);

        while (it.hasNext())
            it.next();
        it.next();
    }

    @Test public void testIterator() throws Exception {
        File f = getDenseTextFile();
        Iterator<MatrixEntry> it = new DenseTextFileIterator(f);

        int count = 0;
        while (it.hasNext()) {
            MatrixEntry entry = it.next();
            int row = entry.row();
            int col = entry.column();
            assertEquals(testMatrix[row][col], entry.value(), .01);
            count++;
        }

        assertEquals(testMatrix.length * testMatrix[0].length, count);
    }

    public static File getDenseTextFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        
        for (int r = 0; r < testMatrix.length; ++r) {
            for (int c = 0; c < testMatrix[0].length; ++c)
                pw.printf("%f ", (float)testMatrix[r][c]);
            pw.println();
        }

        pw.close();
        return f;
    }
}
