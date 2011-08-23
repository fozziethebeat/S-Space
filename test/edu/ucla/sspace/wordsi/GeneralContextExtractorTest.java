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
public class GeneralContextExtractorTest {

    private Map <String, SparseDoubleVector> termMap;

    protected void setupTermMap() {
        termMap = new HashMap<String, SparseDoubleVector>();
        termMap.put("the",
                    new CompactSparseVector(new double[]{0, 1, 0, 0, 1, 2, 1}));
        termMap.put("brown",
                    new CompactSparseVector(new double[]{0, 1, 0, 1, 2, 1, 1}));
        termMap.put("foxes",
                    new CompactSparseVector(new double[]{0, 1, 0, 1, 2, 1, 1}));
        termMap.put("jumped",
                    new CompactSparseVector(new double[]{0, 1, 0, 1, 2, 2, 0}));
        termMap.put("over",
                    new CompactSparseVector(new double[]{0, 1, 0, 1, 1, 2, 1}));
        termMap.put("a",
                    new CompactSparseVector(new double[]{0, 0, 0, 1, 2, 2, 1}));
        termMap.put("cats",
                    new CompactSparseVector(new double[]{0, 1, 0, 0, 1, 2, 1}));
    }

    @Test public void testProcessDocument() {
        ContextExtractor extractor = new GeneralContextExtractor(
                new MockGenerator(), 5, false);
        MockWordsi wordsi = new MockWordsi(null, extractor, null);

        String text = "the brown foxes jumped over a cats";
        setupTermMap();

        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testProcessDocumentWithHeader() {
        ContextExtractor extractor = new GeneralContextExtractor(
                new MockGenerator(), 5, true);
        MockWordsi wordsi = new MockWordsi(null, extractor, "CHICKEN:");

        String text = "CHICKEN: the brown foxes jumped over a cats";
        setupTermMap();

        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testVectorLength() {
        ContextExtractor extractor = new GeneralContextExtractor(
                new MockGenerator(), 5, false);
        assertEquals(7, extractor.getVectorLength());
    }

    class MockGenerator implements ContextGenerator {
        public SparseDoubleVector generateContext(Queue<String> prev,
                                                  Queue<String> next) {
            SparseDoubleVector v = new CompactSparseVector(7);
            for (String word : prev)
                v.add(word.length(), 1);
            for (String word : next)
                v.add(word.length(), 1);
            return v;
        }

        public int getVectorLength() {
            return 7;
        }

        public void setReadOnly(boolean r) {
        }
    }

    class MockWordsi extends BaseWordsi {

        boolean called;

        String expectedSecondaryKey;

        public MockWordsi(Set<String> acceptedWords,
                          ContextExtractor extractor,
                          String expectedSecondaryKey) {
            super(acceptedWords, extractor);
            called = false;
            this.expectedSecondaryKey = expectedSecondaryKey; 
        }
                          
        public void processSpace(Properties props) {
        }

        public void handleContextVector(String primaryKey,
                                        String secondaryKey,
                                        SparseDoubleVector v) {
            called = true;
            if (expectedSecondaryKey != null)
                assertEquals(expectedSecondaryKey, secondaryKey);
            else
                assertEquals(primaryKey, secondaryKey);
            assertTrue(termMap.containsKey(primaryKey));
            SparseDoubleVector expected = termMap.get(primaryKey);
            assertEquals(VectorIO.toString(expected), VectorIO.toString(v));
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }
}
