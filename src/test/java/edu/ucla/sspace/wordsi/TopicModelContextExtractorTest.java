/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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


package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.*;

import edu.ucla.sspace.vector.*;

import java.io.*;

import java.util.HashMap;
import java.util.Queue;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class TopicModelContextExtractorTest {

    private Map <String, SparseDoubleVector> termMap;

    @Test public void testProcessDocument() {
        ContextExtractor extractor = new TopicModelContextExtractor();
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "foxes.n.123 1 1.0 5 2.0 2 4.0 3 4.5 4 0.0 0 0.0";

        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertEquals(6, extractor.getVectorLength());
    }

    class MockWordsi extends BaseWordsi {

        public MockWordsi(Set<String> acceptedWords,
                          ContextExtractor extractor) {
            super(acceptedWords, extractor);
        }
                          
        public void processSpace(Properties props) {
        }

        public void handleContextVector(String primaryKey,
                                        String secondaryKey,
                                        SparseDoubleVector v) {
            assertEquals("foxes.n.123", secondaryKey);
            assertEquals("foxes", primaryKey);
            assertEquals(0, v.get(0), .001);
            assertEquals(1.0, v.get(1), .001);
            assertEquals(4.0, v.get(2), .001);
            assertEquals(4.5, v.get(3), .001);
            assertEquals(0, v.get(4), .001);
            assertEquals(2, v.get(5), .001);
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }
}
