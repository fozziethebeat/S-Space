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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class WaCKyDependencyExtractorTest extends CoNLLDependencyExtractorTest {

    public static final String SINGLE_PARSE = 
        "Mr. _   NNP 1  2   NMOD    _   _\n" +
        "Holt    _   NNP 2   3   SBJ _   _\n" +
        "is  _   VBZ 3   0   ROOT    _   _\n" +
        "a   _   DT  4   5   NMOD    _   _\n" +
        "columnist   _   NN  5   3   PRD _   _\n" +
        "for _   IN  6   5   NMOD    _   _\n" +
        "the _   DT  7   9   NMOD    _   _\n" +
        "Literary    _   NNP 8   9   NMOD    _   _\n" +
        "Review  _   NNP 9   6   PMOD    _   _\n" +
        "in  _   IN  10   9   ADV _   _\n" +
        "London  _   NNP 11   10  PMOD    _   _\n" +
        ".   _   .   12   3   P   _   _";

    public static final String SECOND_PARSE =
        "Individuell _   AJ  1   2   AT  _   _\n" +
        "beskattning _   N  2   0   ROOT    _   _\n" +
        "av  _   PR  3   2   ET  _   _\n" +
        "arbetsinkomster _   NN  4  3   PA  _   _\n";

    public static final String DOUBLE_PARSE =
        "\n\n" +
        SINGLE_PARSE +
        "\n\n" +
        SECOND_PARSE;

    public static final String CONCATONATED_PARSE =
        SINGLE_PARSE + "\n" + SECOND_PARSE;

    public static final String DOUBLE_ZERO_OFFSET_PARSE = 
        "Mr. _   NNP 0   2   NMOD    _   _\n" +
        "Holt    _   NNP 1   3   SBJ _   _\n" +
        "is  _   VBZ 2   0   ROOT    _   _\n" +
        "a   _   DT  3   5   NMOD    _   _\n" +
        "columnist   _   NN  4   3   PRD _   _\n" +
        "for _   IN  5   5   NMOD    _   _\n" +
        "the _   DT  6  9   NMOD    _   _\n" +
        "Literary    _   NNP 7   9   NMOD    _   _\n" +
        "Review  _   NNP 8   6   PMOD    _   _\n" +
        "in  _   IN  9   9   ADV _   _\n" +
        "London  _   NNP 10   10  PMOD    _   _\n" +
        ".   _   .   11   3   P   _   _" + "\n" +
        "Individuell _   AJ  0   2   AT  _   _\n" +
        "beskattning _   N  1   0   ROOT    _   _\n" +
        "av  _   PR  2   2   ET  _   _\n" +
        "arbetsinkomster _   NN  3  3   PA  _   _\n";
 
    @Test public void testSingleExtraction() throws Exception {
        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(SINGLE_PARSE);
        DependencyTreeNode[] nodes = extractor.readNextTree(doc.reader());

        assertEquals(12, nodes.length);

        // Check the basics of the node.
        assertEquals("review", nodes[8].word());
        assertEquals("NNP", nodes[8].pos());

        // Test expected relation for each of the links for "Review".
        DependencyRelation[] expectedRelations = new DependencyRelation[] {
            new SimpleDependencyRelation(new SimpleDependencyTreeNode("review", "NNP"),
                                         "NMOD",
                                         new SimpleDependencyTreeNode("the", "DT")),
            new SimpleDependencyRelation(new SimpleDependencyTreeNode("review", "NNP"),
                                         "NMOD",
                                         new SimpleDependencyTreeNode("literary", "NNP")),
            new SimpleDependencyRelation(new SimpleDependencyTreeNode("review", "NNP"),
                                         "ADV",
                                         new SimpleDependencyTreeNode("in", "IN")),
            new SimpleDependencyRelation(new SimpleDependencyTreeNode("for", "IN"),
                                         "PMOD",
                                         new SimpleDependencyTreeNode("review", "NNP"))
        };

        evaluateRelations(nodes[8], new LinkedList<DependencyRelation>(Arrays.asList(expectedRelations)));
    }

    @Test public void testDoubleExtraction() throws Exception {
        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(DOUBLE_PARSE);
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());
        assertTrue(relations != null);
        assertEquals(12, relations.length);

        testFirstRoot(relations, 2);

        relations = extractor.readNextTree(doc.reader());
        assertTrue(relations != null);
        assertEquals(4, relations.length);

        testSecondRoot(relations, 1);
    }
    
    @Test public void testRootNode() throws Exception {
        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(SINGLE_PARSE);
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());

        assertEquals(12, relations.length);

        testFirstRoot(relations, 2);
    }

    @Test public void testConcatonatedTrees() throws Exception {
        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(CONCATONATED_PARSE);
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());
        
        assertEquals(16, relations.length);
        testFirstRoot(relations, 2);
        testSecondRoot(relations, 13);
    }

    @Test public void testConcatonatedTreesZeroOffset() throws Exception {
        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(DOUBLE_ZERO_OFFSET_PARSE);
        DependencyTreeNode[] relations = extractor.readNextTree(doc.reader());
        
        assertEquals(16, relations.length);
        testFirstRoot(relations, 2);
        testSecondRoot(relations, 13);
    }
}
