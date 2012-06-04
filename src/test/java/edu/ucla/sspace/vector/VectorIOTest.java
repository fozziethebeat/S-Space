/*
 * Copyright 2009 Keith Stevens 
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class VectorIOTest {

    @Test public void testReadSparseVector() throws IOException {
        String testInput = "4 10\n" +
                           "0:21.4 9:123.4\n" +
                           "1:0.4 8:1E-4\n" +
                           "3:1.0 4:2.0 5:3.4\n" +
                           "5:2.0 6:1.4\n";
        List<SparseDoubleVector> output = VectorIO.readSparseVectors(
                new StringReader(testInput));
        assertEquals(4, output.size());

        SparseDoubleVector v1 = output.get(0);
        assertEquals(10, v1.length());
        assertEquals(2, v1.getNonZeroIndices().length);
        assertEquals(21.4, v1.get(0), 0.0000000001);
        assertEquals(123.4, v1.get(9), 0.0000000001);

        SparseDoubleVector v2 = output.get(1);
        assertEquals(10, v2.length());
        assertEquals(2, v2.getNonZeroIndices().length);
        assertEquals(0.4, v2.get(1), 0.0000000001);
        assertEquals(1E-4, v2.get(8), 0.0000000001);

        SparseDoubleVector v3 = output.get(2);
        assertEquals(10, v3.length());
        assertEquals(3, v3.getNonZeroIndices().length);
        assertEquals(1.0, v3.get(3), 0.0000000001);
        assertEquals(2.0, v3.get(4), 0.0000000001);
        assertEquals(3.4, v3.get(5), 0.0000000001);

        SparseDoubleVector v4 = output.get(3);
        assertEquals(10, v4.length());
        assertEquals(2, v4.getNonZeroIndices().length);
        assertEquals(2.0, v4.get(5), 0.0000000001);
        assertEquals(1.4, v4.get(6), 0.0000000001);
    }

    @Test public void testWriteSparseVectors() {
        List<SparseDoubleVector> vectors =
            new ArrayList<SparseDoubleVector>();

        SparseDoubleVector v = new CompactSparseVector(4);
        v.set(0, 1.0);
        v.set(3, 1.9);
        vectors.add(v);

        v = new CompactSparseVector(4);
        v.set(2, 1.4);
        v.set(1, 0.9);
        vectors.add(v);

        v = new CompactSparseVector(4);
        v.set(0, 1.4);
        v.set(1, 1.4);
        v.set(2, 1.4);
        v.set(3, 0.9);
        vectors.add(v);

        ByteArrayOutputStream resultStream = 
            new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(resultStream);
        VectorIO.writeVectors(vectors, ps);
        ps.close();

        String result = resultStream.toString();
        String[] lines = result.split("\\n");
        assertEquals("3 4", lines[0]);
        for (int i = 1; i < lines.length; ++i) 
            for (String line : lines[i].split("\\s+")) {
                String[] parts = line.split(":");
                int col = Integer.parseInt(parts[0]);
                double val = Double.parseDouble(parts[1]);
                assertEquals(vectors.get(i-1).get(col), val, 0.0000001);
            }
    }

    @Test public void testSparseToString() {
        Vector vector = new CompactSparseVector(new double[]{0, 0, 0, 1});
        assertEquals("3,1.0", VectorIO.toString(vector));
    }

    @Test public void testSparseToString2() {
        Vector vector = new CompactSparseVector(new double[]{0, 1, 0, 5});
        assertEquals("1,1.0;3,5.0", VectorIO.toString(vector));
    }

    @Test public void testDenseToString() {
        Vector vector = new DenseVector(new double[]{0, 1, 0, 5});
        assertEquals("0.0 1.0 0.0 5.0", VectorIO.toString(vector));
    }
}
