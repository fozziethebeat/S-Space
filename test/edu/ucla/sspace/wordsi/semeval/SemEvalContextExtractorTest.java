/*
 * Copyright 2011 Keith Stevens 
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

package edu.ucla.sspace.wordsi.semeval;

import edu.ucla.sspace.common.*;

import edu.ucla.sspace.vector.*;

import edu.ucla.sspace.wordsi.*;

import java.io.*;

import java.util.Queue;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SemEvalContextExtractorTest {

    @Test public void testProcessDocumentWithDefaultSeparator() {
        ContextExtractor extractor = new SemEvalContextExtractor(
                new MockGenerator(), 5);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "Doc1234: the brown foxes |||| jumped over a cats";
        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testProcessDocumentWithNonDefaultSeparator() {
        ContextExtractor extractor = new SemEvalContextExtractor(
                new MockGenerator(), 5, "chicken");
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "Doc1234: the brown foxes chicken jumped over a cats";
        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testProcessDocumentWithLongerContext() {
        ContextExtractor extractor = new SemEvalContextExtractor(
                new MockGenerator(), 3);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "Doc1234: blah blah blah the brown foxes |||| jumped over a cats blah blah blah";
        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
    }

    @Test public void testProcessDocumentWithNoFocusWord() {
        ContextExtractor extractor = new SemEvalContextExtractor(
                new MockGenerator(), 3);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "Doc1234: blah blah blah the brown foxes jumped over a cats blah blah blah";
        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertFalse(wordsi.called);
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
            assertEquals("Doc1234:", secondaryKey);
            assertEquals("jumped", primaryKey);
            SparseDoubleVector expected = new CompactSparseVector(
                    new double[]{0, 1, 0, 1, 2, 2, 0});
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
