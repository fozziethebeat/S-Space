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
public class SemEvalCorpusReaderTest {

    private final String TRAIN_TEXT =
        "<cat.n.train><cat.n.1>chicken cat bar</cat.n.1></cat.n.train>";

    private final String TEST_TEXT=
        "<cat.n.test><cat.n.1>chicken <TargetSentence>cat</TargetSentence> bar</cat.n.1></cat.n.test>";

    @Test public void testTrainReader() throws Exception {
        CorpusReader<Document> reader = new SemEvalCorpusReader();
        Iterator<Document> iter = reader.read(new StringReader(TRAIN_TEXT));

        assertTrue(iter.hasNext());
        assertEquals("cat.n.1 chicken |||| cat bar",
                     iter.next().reader().readLine().trim());

        assertFalse(iter.hasNext());
    }

    @Test public void testTestReader() throws Exception {
        CorpusReader<Document> reader = new SemEvalCorpusReader();
        Iterator<Document> iter = reader.read(new StringReader(TEST_TEXT));

        assertTrue(iter.hasNext());
        assertEquals("cat.n.1 chicken |||| cat bar",
                     iter.next().reader().readLine().trim());

        assertFalse(iter.hasNext());
    }
}

