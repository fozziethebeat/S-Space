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

public class MatlabSparseFileIteratorTests {

    private static final double[][] testMatrix = {
        {2.3, 0, 4.2},
        {0, 1.3, 2.2},
        {3.8, 0, 0.5}};

    @Test(expected=UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        File f = getMatlabFile();
        Iterator<MatrixEntry> it = new MatlabSparseFileIterator(f);
        it.remove();
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyNext() throws Exception {
        File f = getMatlabFile();
        Iterator<MatrixEntry> it = new MatlabSparseFileIterator(f);

        while (it.hasNext())
            it.next();
        it.next();
    }

    @Test public void testIterator() throws Exception {
        File f = getMatlabFile();
        Iterator<MatrixEntry> it = new MatlabSparseFileIterator(f);

        while (it.hasNext()) {
            MatrixEntry me = it.next();
            int row = me.row();
            int column = me.column();
            assertEquals(testMatrix[row][column], me.value(), .01);
        }
        assertFalse(it.hasNext());
    }
    
    public static File getMatlabFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        PrintWriter pw = new PrintWriter(f);
        for (int r = 0; r < testMatrix.length; ++r)
            for (int c = 0; c < testMatrix[0].length; ++c)
                pw.printf("%d %d %f\n", r+1, c+1, testMatrix[r][c]);
        pw.close();
        return f;
    }
}
