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

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SvdlibcSparseBinaryFileRowIteratorTests {

    @Test public void testIterator() throws Exception {
        File f = getSparseBinarySVDLIBCFile();
        Iterator<SparseDoubleVector> it =
            new SvdlibcSparseBinaryFileRowIterator(f);

        // Check row 1.
        SparseDoubleVector vector = it.next();
        assertEquals(2, vector.getNonZeroIndices().length);
        assertEquals(2.3, vector.get(0), .0001);
        assertEquals(3.8, vector.get(2), .0001);

        // Check row 2.
        vector = it.next();
        assertEquals(1, vector.getNonZeroIndices().length);
        assertEquals(1.3, vector.get(1), .0001);

        // Check row 3.
        vector = it.next();
        assertEquals(3, vector.getNonZeroIndices().length);
        assertEquals(4.2, vector.get(0), .0001);
        assertEquals(2.2, vector.get(1), .0001);
        assertEquals(0.5, vector.get(2), .0001);

        // Check that there are no more rows.
        assertFalse(it.hasNext());
    }

    public static File getSparseBinarySVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        DataOutputStream pw = new DataOutputStream(new FileOutputStream(f));

        // Write the header data.
        pw.writeInt(3);
        pw.writeInt(3);
        pw.writeInt(6);

        // Write row 1.
        pw.writeInt(2);
        pw.writeInt(0);
        pw.writeFloat(2.3f);
        pw.writeInt(2);
        pw.writeFloat(3.8f);

        // Write row 2.
        pw.writeInt(1);
        pw.writeInt(1);
        pw.writeFloat(1.3f);

        // Write row 3.
        pw.writeInt(3);
        pw.writeInt(0);
        pw.writeFloat(4.2f);
        pw.writeInt(1);
        pw.writeFloat(2.2f);
        pw.writeInt(2);
        pw.writeFloat(0.5f);

        pw.close();
        return f;
    }


}
