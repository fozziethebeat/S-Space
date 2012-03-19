/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.*;

import edu.ucla.sspace.vector.*;

import java.io.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class BaseWordsiTest {

    @Test public void testSetAcceptWord() {
        Set<String> words = new HashSet<String>();
        words.add("cat");
        Wordsi mock = new MockWordsi(words, new MockExtractor());

        assertFalse(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testEmptyAcceptWord() {
        Set<String> words = new HashSet<String>();
        Wordsi mock = new MockWordsi(words, new MockExtractor());

        assertTrue(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testNullAcceptWord() {
        Wordsi mock = new MockWordsi(null, new MockExtractor());

        assertTrue(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testVectorLength() {
        ContextExtractor extractor = new MockExtractor();
        SemanticSpace mock = new MockWordsi(null, extractor);
        assertEquals(extractor.getVectorLength(), mock.getVectorLength());
    }

    @Test public void testProcessDocument() throws Exception {
        MockExtractor extractor = new MockExtractor();
        SemanticSpace mock = new MockWordsi(null, extractor);
        mock.processDocument(null);
        assertTrue(extractor.calledProcessDocument);
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
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }

    class MockExtractor implements ContextExtractor {

        public boolean calledProcessDocument;

        public void processDocument(BufferedReader br, Wordsi wordsi) {
            calledProcessDocument = true;
        }

        public int getVectorLength() {
            return 10;
        }
    }
}
