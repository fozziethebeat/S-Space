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

package edu.ucla.sspace.wordsi.psd;

import edu.ucla.sspace.dependency.*;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;

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
public class PseudoWordDependencyContextExtractorTest {

    public static final String SINGLE_PARSE = 
        "target: cat absolute_position: 4 relative_position: 4 prior_trees: 0 after_trees: 0\n" + 
        "1   Mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "2   Holt    _   NNP NNP _   3   SBJ _   _\n" +
        "3   is  _   VBZ VBZ _   0   ROOT    _   _\n" +
        "4   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "5   cat _ NN  NN  _   3   PRD _   _\n" +
        "6   for _   IN  IN  _   5   NMOD    _   _\n" +
        "7   the _   DT  DT  _   9   NMOD    _   _\n" +
        "8   Literary    _   NNP NNP _   9   NMOD    _   _\n" +
        "9   Review  _   NNP NNP _   6   PMOD    _   _\n" +
        "10  in  _   IN  IN  _   9   ADV _   _\n" +
        "11  London  _   NNP NNP _   10  PMOD    _   _\n" +
        "12  .   _   .   .   _   3   P   _   _";

    private SparseDoubleVector testVector;

    @Test public void testProcessDocument() throws Exception {
        testVector = new CompactSparseVector(new double[] {0, 0, 1, 0});
        Map<String, String> termMap = new HashMap<String, String>();
        DependencyContextExtractor extractor =
            new PseudoWordDependencyContextExtractor(
                    new CoNLLDependencyExtractor(), 
                    new MockGenerator(), termMap);
        MockWordsi wordsi = new MockWordsi(null, extractor);

        termMap.put("cat", "catdog");
        extractor.processDocument(
                new BufferedReader(new StringReader(SINGLE_PARSE)), wordsi);
        assertTrue(wordsi.called);
    }

    @Test public void testAcceptWord() {
        Map<String, String> termMap = new HashMap<String, String>();
        termMap.put("cat", "catdog");
        PseudoWordDependencyContextExtractor extractor = 
            new PseudoWordDependencyContextExtractor(null, null, termMap);

        DependencyTreeNode node = new SimpleDependencyTreeNode(
                "cat", "n", "c", null);
        assertTrue(extractor.acceptWord(node, "cat", null));
        assertFalse(extractor.acceptWord(node, "cat.n.1", null));

        node = new SimpleDependencyTreeNode("c", "n", "c", null);
        assertFalse(extractor.acceptWord(node, "cat", null));

        node = new SimpleDependencyTreeNode("", "n", "c", null);
        assertFalse(extractor.acceptWord(node, "", null));
    }

    class MockGenerator implements DependencyContextGenerator {

        public SparseDoubleVector generateContext(DependencyTreeNode[] nodes,
                                                  int index) {
            assertEquals("cat", nodes[index].word());
            return testVector;
        }

        public int getVectorLength() {
            return 5;
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

        public boolean acceptWord(String word) {
            return word.equals("cat");
        }
    }
}
