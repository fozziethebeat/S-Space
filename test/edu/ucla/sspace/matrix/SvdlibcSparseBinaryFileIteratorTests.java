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

public class SvdlibcSparseBinaryFileIteratorTests {

   @Test(expected=UnsupportedOperationException.class)
    public void testRemove() throws Exception {
        File f = getSparseBinarySVDLIBCFile();
        Iterator<MatrixEntry> it = new SvdlibcSparseBinaryFileIterator(f);
        it.remove();
    }

    @Test(expected=NoSuchElementException.class)
    public void testEmptyNext() throws Exception {
        File f = getSparseBinarySVDLIBCFile();
        Iterator<MatrixEntry> it = new SvdlibcSparseBinaryFileIterator(f);

        while (it.hasNext())
            it.next();
        it.next();
    }

    @Test public void testIterator() throws Exception {
        File f = getSparseBinarySVDLIBCFile();
        Iterator<MatrixEntry> it = new SvdlibcSparseBinaryFileIterator(f);
        MatrixEntry me = it.next();
        // Col 0
        assertEquals(0, me.column());
        assertEquals(0, me.row());
        me = it.next();
        assertEquals(0, me.column());
        assertEquals(2, me.row());
        me = it.next();
        // Col 1
        assertEquals(1, me.column());
        assertEquals(1, me.row());
        me = it.next();
        // Col 2
        assertEquals(2, me.column());
        assertEquals(0, me.row());
        me = it.next();
        assertEquals(2, me.column());
        assertEquals(1, me.row());
        me = it.next();
        assertEquals(2, me.column());
        assertEquals(2, me.row());

        assertFalse(it.hasNext());
    }
    

    public static File getSparseBinarySVDLIBCFile() throws Exception {
        File f = File.createTempFile("unit-test",".dat");
        DataOutputStream pw = new DataOutputStream(new FileOutputStream(f));
        pw.writeInt(3);
        pw.writeInt(3);
        pw.writeInt(6);

        pw.writeInt(2);
        pw.writeInt(0);
        pw.writeFloat(2.3f);
        pw.writeInt(2);
        pw.writeFloat(3.8f);

        pw.writeInt(1);
        pw.writeInt(1);
        pw.writeFloat(1.3f);

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
