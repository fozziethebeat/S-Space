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

import edu.ucla.sspace.clustering.*;

import edu.ucla.sspace.matrix.*;

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
public class WaitingWordsiTest {

    SparseDoubleVector[] vectors;
    @Test public void testAcceptWord() {
        Set<String> words = new HashSet<String>();
        words.add("cat");

        Wordsi wordsi = new WaitingWordsi(words, null, null, null, 5);

        assertTrue(wordsi.acceptWord("cat"));
        assertFalse(wordsi.acceptWord("dog"));
        assertFalse(wordsi.acceptWord(""));
    }

    @Test public void testEmptyAcceptWord() {
        Set<String> words = new HashSet<String>();
        Wordsi wordsi = new WaitingWordsi(words, null, null, null, 5);

        assertTrue(wordsi.acceptWord("cat"));
        assertTrue(wordsi.acceptWord("dog"));
        assertTrue(wordsi.acceptWord(""));
    }

    @Test public void testNullAcceptWord() {
        Wordsi wordsi = new WaitingWordsi(null, null, null, null, 5);

        assertTrue(wordsi.acceptWord("cat"));
        assertTrue(wordsi.acceptWord("dog"));
        assertTrue(wordsi.acceptWord(""));
    }

    @Test public void testHandleContextVector() {
        MockReporter reporter = new MockReporter();
        Wordsi wordsi = new WaitingWordsi(null, null, null, reporter, 5);

        reporter.setExpectation("cat", "dog", 0);
        wordsi.handleContextVector("cat", "dog", null);
        assertTrue(reporter.called);

        reporter.setExpectation("cat", "dog", 1);
        wordsi.handleContextVector("cat", "dog", null);
        assertTrue(reporter.called);

        reporter.setExpectation("cats", "dog", 0);
        wordsi.handleContextVector("cats", "dog", null);
        assertTrue(reporter.called);
    }

    @Test public void testProcessSpace() {
        MockClustering clustering = new MockClustering();
        MockExtractor extractor = new MockExtractor();
        WaitingWordsi wordsi = new WaitingWordsi(
                null, extractor, clustering, null, 2);

        vectors = new SparseDoubleVector[] {
            new CompactSparseVector(new double[] {1, 0, 0, 0}),
            new CompactSparseVector(new double[] {0, 1, 0, 0}),
            new CompactSparseVector(new double[] {0, 0, 1, 0}),
            new CompactSparseVector(new double[] {0, 0, 0, 1}),
        };

        wordsi.handleContextVector("cat", "dog", vectors[0]);
        wordsi.handleContextVector("cat", "dog", vectors[1]);
        wordsi.handleContextVector("cat", "dog", vectors[2]);
        wordsi.handleContextVector("cat", "dog", vectors[3]);

        wordsi.processSpace(System.getProperties());
        assertTrue(clustering.calledWithNumC);

        vectors = new SparseDoubleVector[] {
            new CompactSparseVector(new double[] {.5, .5, 0, 0}),
            new CompactSparseVector(new double[] {0, 0, .5, .5}),
        };

        assertEquals(VectorIO.toString(vectors[0]),
                     VectorIO.toString(wordsi.getVector("cat")));
        assertEquals(VectorIO.toString(vectors[1]),
                     VectorIO.toString(wordsi.getVector("cat-1")));

        assertTrue(wordsi.getWords().contains("cat"));
        assertTrue(wordsi.getWords().contains("cat-1"));
        assertEquals(2, wordsi.getWords().size());
    }

    class MockClustering implements Clustering {

        boolean calledWithoutNumC;
        boolean calledWithNumC;

        public MockClustering() {
            calledWithoutNumC = false;
            calledWithNumC = false;
        }

        public Assignments cluster(Matrix matrix, Properties props) {
            calledWithoutNumC = true;
 
            Assignment[] assignments = new Assignment[4];
            assignments[0] = new HardAssignment(0);
            assignments[1] = new HardAssignment(0);
            assignments[2] = new HardAssignment(1);
            assignments[3] = new HardAssignment(1);

            return new Assignments(2, assignments, matrix);
        }

        public Assignments cluster(Matrix matrix, int numC, Properties props) {
            assertEquals(2, numC);
            assertEquals(VectorIO.toString(vectors[0]),
                         VectorIO.toString(matrix.getRowVector(0)));
            assertEquals(VectorIO.toString(vectors[1]),
                         VectorIO.toString(matrix.getRowVector(1)));
            assertEquals(VectorIO.toString(vectors[2]),
                         VectorIO.toString(matrix.getRowVector(2)));
            assertEquals(VectorIO.toString(vectors[3]),
                         VectorIO.toString(matrix.getRowVector(3)));
            
            Assignment[] assignments = new Assignment[4];
            assignments[0] = new HardAssignment(0);
            assignments[1] = new HardAssignment(0);
            assignments[2] = new HardAssignment(1);
            assignments[3] = new HardAssignment(1);

            calledWithNumC = true;
            return new Assignments(2, assignments, matrix);
        }
    }

    class MockExtractor implements ContextExtractor {

        public void processDocument(BufferedReader document, Wordsi wordsi) {
        }

        public int getVectorLength() {
            return 4;
        }
    }


    class MockReporter implements AssignmentReporter {

        String expectedPrimary;
        String expectedSecondary;
        int expectedContextId;
        boolean called;

        public void setExpectation(String primary, String secondary, int id) {
            expectedPrimary = primary;
            expectedSecondary = secondary;
            expectedContextId = id;
            called = false;
        }

        public void updateAssignment(String primary, String secondary, int id) {
        }

        public void finalizeReport() {
        }

        public void assignContextToKey(String primaryKey,
                                       String secondaryKey,
                                       int contextId) {
            assertEquals(expectedPrimary, primaryKey);
            assertEquals(expectedSecondary, secondaryKey);
            assertEquals(expectedContextId, contextId);
            called = true;
        }

        public String[] contextLabels(String word) {
            return null;
        }
    }
}
