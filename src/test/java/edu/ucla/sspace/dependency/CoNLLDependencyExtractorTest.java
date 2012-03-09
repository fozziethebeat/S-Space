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

package edu.ucla.sspace.dependency;

import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.text.Document;

import java.io.BufferedReader;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class CoNLLDependencyExtractorTest {

    public static final String SINGLE_PARSE = 
        "1   mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "2   holt    _   NNP NNP _   3   SBJ _   _\n" +
        "3   is  _   VBZ VBZ _   0   ROOT    _   _\n" +
        "4   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "5   columnist   _   NN  NN  _   3   PRD _   _\n" +
        "6   for _   IN  IN  _   5   NMOD    _   _\n" +
        "7   the _   DT  DT  _   9   NMOD    _   _\n" +
        "8   literary    _   NNP NNP _   9   NMOD    _   _\n" +
        "9   review  _   NNP NNP _   6   PMOD    _   _\n" +
        "10  in  _   IN  IN  _   9   ADV _   _\n" +
        "11  london  _   NNP NNP _   10  PMOD    _   _\n" +
        "12  .   _   .   .   _   3   P   _   _";

    public static final String SECOND_PARSE =
        "1   individuell _   AJ  AJ  _   2   AT  _   _\n" +
        "2   beskattning _   N   VN  _   0   ROOT    _   _\n" +
        "3   av  _   PR  PR  _   2   ET  _   _\n" +
        "4   arbetsinkomster _   N   NN  SS  3   PA  _   _\n";

    public static final String DOUBLE_PARSE =
        "\n\n" +
        SINGLE_PARSE +
        "\n\n" +
        SECOND_PARSE;

    public static final String CONCATONATED_PARSE =
        SINGLE_PARSE + "\n" + SECOND_PARSE;

    public static final String DOUBLE_ZERO_OFFSET_PARSE = 
        "0   mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "1   holt    _   NNP NNP _   3   SBJ _   _\n" +
        "2   is  _   VBZ VBZ _   0   ROOT    _   _\n" +
        "3   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "4   columnist   _   NN  NN  _   3   PRD _   _\n" +
        "5   for _   IN  IN  _   5   NMOD    _   _\n" +
        "6   the _   DT  DT  _   9   NMOD    _   _\n" +
        "7   literary    _   NNP NNP _   9   NMOD    _   _\n" +
        "8   review  _   NNP NNP _   6   PMOD    _   _\n" +
        "9   in  _   IN  IN  _   9   ADV _   _\n" +
        "10  london  _   NNP NNP _   10  PMOD    _   _\n" +
        "11  .   _   .   .   _   3   P   _   _" + "\n" +
        "0   individuell _   AJ  AJ  _   2   AT  _   _\n" +
        "1   beskattning _   N   VN  _   0   ROOT    _   _\n" +
        "2   av  _   PR  PR  _   2   ET  _   _\n" +
        "3   arbetsinkomster _   N   NN  SS  3   PA  _   _\n";
 
    /**
     * A simple function that tests the neighbors for a given relation.  The
     * passed in string is expected to contain the relation for each node that
     * is connected to {@code relation}.
     */
    protected void evaluateRelations(DependencyTreeNode node,
                                     List<DependencyRelation> expectedRelations) {
        // Check that the relations have the expected number 
        assertEquals(expectedRelations.size(), node.neighbors().size());

        System.out.println("Expected: " + expectedRelations);
        // Check that all the neighbors are in the e
        for (DependencyRelation rel : node.neighbors()) {
            System.out.println("relation: " + rel);
            if (!expectedRelations.contains(rel))
                System.out.printf("FAIL: %s does not contain %s%n", expectedRelations, rel);
            assertTrue(expectedRelations.contains(rel));
            // Remove the relation from the list to double check that the
            // neighbors are a list of duplicate relations.
            expectedRelations.remove(rel);
        }
        assertEquals(0, expectedRelations.size());
    }

    /**
     * A simple helper method for checking that the root node in the first
     * sentence is correctly linked up."
     */
    protected void testFirstRoot(DependencyTreeNode[] relations, int index) {
        // Check the basics of the node.
        assertEquals("is", relations[index].word());
        assertEquals("VBZ", relations[index].pos());

        // Test that the root node does not have a link to itself.
        DependencyRelation[] expectedRelations = new DependencyRelation[] {
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("is", "VBZ", 0), 
                    "SBJ",
                    new SimpleDependencyTreeNode("holt", "NNP", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("is", "VBZ", 0),
                    "PRD",
                    new SimpleDependencyTreeNode("columnist", "NN", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("is", "VBZ", 0),
                    "P",
                    new SimpleDependencyTreeNode(".", ".", 0))
        };
        evaluateRelations(relations[index],
                          new LinkedList<DependencyRelation>(Arrays.asList(expectedRelations)));
    }

    /**
     * A simple helper method for checking that the root node in the second
     * sentence is correctly linked up."
     */
    protected void testSecondRoot(DependencyTreeNode[] relations, int index) {
        // Check the basics of the node.
        assertEquals("beskattning", relations[index].word());
        assertEquals("N", relations[index].pos());

        // Test expected relation for each of the links for "beskattning".
        DependencyRelation[] expectedRelations = new DependencyRelation[] {
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("beskattning", "N", 0),
                    "AT",
                    new SimpleDependencyTreeNode("individuell", "AJ", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("beskattning", "N", 0),
                    "ET",
                    new SimpleDependencyTreeNode("av", "PR", 0))
        };
        
        evaluateRelations(relations[index],
                          new LinkedList<DependencyRelation>(
                              Arrays.asList(expectedRelations)));
    }

    @Test public void testSingleExtraction() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Document doc = new StringDocument(toTabs(SINGLE_PARSE));
        DependencyTreeNode[] nodes = extractor.readNextTree(doc.reader());

        assertEquals(12, nodes.length);

        // Check the basics of the node.
        assertEquals("review", nodes[8].word());
        assertEquals("NNP", nodes[8].pos());

        // Test expected relation for each of the links for "Review".
        DependencyRelation[] expectedRelations = new DependencyRelation[] {
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("review", "NNP", 0),
                    "NMOD",
                    new SimpleDependencyTreeNode("the", "DT", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("review", "NNP", 0),
                    "NMOD",
                    new SimpleDependencyTreeNode("literary", "NNP", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("review", "NNP", 0),
                    "ADV",
                    new SimpleDependencyTreeNode("in", "IN", 0)),
            new SimpleDependencyRelation(
                    new SimpleDependencyTreeNode("for", "IN", 0),
                    "PMOD",
                    new SimpleDependencyTreeNode("review", "NNP", 0))
        };

        evaluateRelations(nodes[8], new LinkedList<DependencyRelation>(
                    Arrays.asList(expectedRelations)));
    }

    @Test public void testDoubleExtraction() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Document doc = new StringDocument("\n\n" +
                                          toTabs(SINGLE_PARSE) +
                                          "\n" +
                                          toTabs(SECOND_PARSE));
        BufferedReader reader = doc.reader();
        DependencyTreeNode[] relations = extractor.readNextTree(reader);
        assertTrue(relations != null);
        assertEquals(12, relations.length);

        testFirstRoot(relations, 2);

        relations = extractor.readNextTree(reader);
        assertTrue(relations != null);
        assertEquals(4, relations.length);

        testSecondRoot(relations, 1);
    }
    
    @Test public void testRootNode() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Document doc = new StringDocument(toTabs(SINGLE_PARSE));
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());

        assertEquals(12, relations.length);

        testFirstRoot(relations, 2);
    }

    @Test public void testConcatonatedTrees() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Document doc = new StringDocument(toTabs(CONCATONATED_PARSE));
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());
        
        assertEquals(16, relations.length);
        testFirstRoot(relations, 2);
        testSecondRoot(relations, 13);
    }

    @Test public void testConcatonatedTreesZeroOffset() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        Document doc = new StringDocument(toTabs(DOUBLE_ZERO_OFFSET_PARSE));
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());
        
        assertEquals(16, relations.length);
        testFirstRoot(relations, 2);
        testSecondRoot(relations, 13);
    }

    static String toTabs(String doc) {
        StringBuilder sb = new StringBuilder();
        String[] arr = doc.split("\n");
        for (String line : arr) {
            String[] cols = line.split("\\s+");
            for (int i = 0; i < cols.length; ++i) {
                sb.append(cols[i]);
                if (i + 1 < cols.length)
                    sb.append('\t');
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
