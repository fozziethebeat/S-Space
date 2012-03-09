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

package edu.ucla.sspace.wordsi.psd;

import edu.ucla.sspace.vector.*;

import edu.ucla.sspace.wordsi.*;

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
public class PseudoWordContextExtractorTest {

    SparseDoubleVector testVector;
    @Test public void testProcessDocument() {
        Map<String, String> termMap = new HashMap<String, String>();
        ContextExtractor extractor = new PseudoWordContextExtractor(
                new MockGenerator(), 3, termMap);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        termMap.put("cat", "catdog");

        String text = "the brown foxes cat jumped over a cats";

        testVector = new CompactSparseVector(new double[] {0,0,0,1});
        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testEmptyProcessDocument() {
        Map<String, String> termMap = new HashMap<String, String>();
        ContextExtractor extractor = new PseudoWordContextExtractor(
                new MockGenerator(), 3, termMap);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "the brown foxes cat jumped over a cats";

        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertFalse(wordsi.called);
    }


    @Test public void testVectorLength() {
        ContextExtractor extractor = new PseudoWordContextExtractor(
                new MockGenerator(), 5, new HashMap<String, String>());
        assertEquals(4, extractor.getVectorLength());
    }

    class MockGenerator implements ContextGenerator {
        public SparseDoubleVector generateContext(Queue<String> prev,
                                                  Queue<String> next) {
            assertEquals("the", prev.remove());
            assertEquals("brown", prev.remove());
            assertEquals("foxes", prev.remove());
            assertEquals("jumped", next.remove());
            assertEquals("over", next.remove());
            assertEquals("a", next.remove());
            return testVector;
        }

        public int getVectorLength() {
            return 4;
        }

        public void setReadOnly(boolean r) {
        }
    }

    class MockWordsi extends BaseWordsi {

        boolean called;

        public MockWordsi(Set<String> acceptedWords,
                          ContextExtractor extractor) {
            super(acceptedWords, extractor);
            called = false;
        }
                          
        public void processSpace(Properties props) {
        }

        public void handleContextVector(String primaryKey,
                                        String secondaryKey,
                                        SparseDoubleVector v) {
            called = true;
            assertEquals("cat", secondaryKey);
            assertEquals("catdog", primaryKey);
            assertEquals(testVector, v);
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }
}
