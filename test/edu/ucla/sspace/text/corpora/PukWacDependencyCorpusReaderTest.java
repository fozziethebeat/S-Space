/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.text.corpora;

import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.Document;

import java.io.StringReader;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PukWacDependencyCorpusReaderTest {

    public static final String FIRST_SENTENCE = 
        "1   Mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "2   Holt    _   NNP NNP _   3   SBJ _   _\n" +
        "3   is  _   VBZ VBZ _   0   ROOT    _   _\n";

    public static final String SECOND_SENTENCE = 
        "4   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "5   columnist   _   NN  NN  _   3   PRD _   _\n" +
        "6   for _   IN  IN  _   5   NMOD    _   _\n" +
        "7   the _   DT  DT  _   9   NMOD    _   _\n" +
        "8   Literary    _   NNP NNP _   9   NMOD    _   _\n";

    public static final String THIRD_SENTENCE = 
        "9   Review  _   NNP NNP _   6   PMOD    _   _\n" +
        "10  in  _   IN  IN  _   9   ADV _   _\n" +
        "11  London  _   NNP NNP _   10  PMOD    _   _\n" +
        "12  .   _   .   .   _   3   P   _   _\n";

    public static final String TEST_TEXT =
        "<text>\n" +
        "<s>\n" +
        FIRST_SENTENCE +
        "</s>\n" +
        "<s>\n" +
        SECOND_SENTENCE +
        "</s>\n" +
        "</text>\n" +
        "<text>\n" +
        "<s>\n" +
        THIRD_SENTENCE +
        "</s>\n" +
        "</text>\n";

    @Test public void testIterator() throws Exception {
        CorpusReader<Document> reader = new PukWacDependencyCorpusReader();
        Iterator<Document> docIter = reader.read(new StringReader(TEST_TEXT));
        assertTrue(docIter.hasNext());
        assertEquals(FIRST_SENTENCE, readAll(docIter.next()));
        assertTrue(docIter.hasNext());
        assertEquals(SECOND_SENTENCE, readAll(docIter.next()));
        assertTrue(docIter.hasNext());
        assertEquals(THIRD_SENTENCE, readAll(docIter.next()));
        assertFalse(docIter.hasNext());
    }

    private static String readAll(Document doc) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line = null; (line = doc.reader().readLine()) != null; )
            sb.append(line).append("\n");
        return sb.toString();
    }
}
