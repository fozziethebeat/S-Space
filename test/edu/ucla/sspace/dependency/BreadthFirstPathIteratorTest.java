/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
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

package edu.ucla.sspace.dependency;

import java.util.*;
import edu.ucla.sspace.text.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class BreadthFirstPathIteratorTest extends PathIteratorTestBase {
    
    String conll =
        "Anarchism       anarchism       NN      1       2       SBJ\n" +
        "is      be      VBZ     2       0       ROOT\n" +
        "a       a       DT      3      5       NMOD\n" +
        "political       political       JJ      4       5       NMOD\n" +
        "philosophy     philosophy      NN      5       2       PRD\n" +
        "encompassing    encompass       VVG     6       5       NMOD\n" +
        "theories        theory  NNS     7       6       OBJ\n" +
        "and     and     CC      8      7       CC\n" +
        "attitudes       attitude        NNS     9       7       COORD\n" +
        "which   which  WDT     10      11      SBJ\n" +
        "consider        consider        VVP     11      9       NMOD\n" +
        "the    the     DT      12      13      NMOD\n" +
        "state   state   NN      13      15      SBJ\n" +
        "to      t      TO      14      15      VMOD\n" +
        "be      be      VB      15      11      OBJ\n" +
        "unnecessary    unnecessary     JJ      16      15      PRD\n" +
        ",       ,       ,       17      16      P\n" +
        "harmul harmful JJ      18      16      COORD\n" +
        ",       ,       ,       19      16      P\n" +
        "and/    ad/    JJ      20      16      COORD\n" +
        "or      or      CC      21      16      CC\n" +
        "undesirable    undesirable     JJ      22      16      COORD\n" +
        ".       .       SENT    23      2       P\n";

    static final Map<String,Integer> PATH_START_COUNTS
        = new TreeMap<String,Integer>();

    static {
        PATH_START_COUNTS.put(",", 12);
        PATH_START_COUNTS.put(".", 3);
        PATH_START_COUNTS.put("a", 4);
        PATH_START_COUNTS.put("ad/", 7);
        PATH_START_COUNTS.put("anarchism", 3);
        PATH_START_COUNTS.put("and", 3);
        PATH_START_COUNTS.put("attitude", 6);
        PATH_START_COUNTS.put("be", 19);
        PATH_START_COUNTS.put("consider", 7);
        PATH_START_COUNTS.put("encompass", 7);
        PATH_START_COUNTS.put("harmful", 7);
        PATH_START_COUNTS.put("or", 7);
        PATH_START_COUNTS.put("philosophy", 7);
        PATH_START_COUNTS.put("political", 4);
        PATH_START_COUNTS.put("state", 5);
        PATH_START_COUNTS.put("t", 4);
        PATH_START_COUNTS.put("the", 2);
        PATH_START_COUNTS.put("theory", 5);
        PATH_START_COUNTS.put("undesirable", 7);
        PATH_START_COUNTS.put("unnecessary", 10);
        PATH_START_COUNTS.put("which", 3);
    }

    private static final int expected = 132;

    @Test public void testIterator() throws Exception {

        DependencyExtractor extractor = new WaCKyDependencyExtractor();
        Document doc = new StringDocument(conll);
        DependencyTreeNode[] tree = extractor.readNextTree(doc.reader());
        assertTrue(tree != null);
        assertEquals(23, tree.length);
        int pathCount = 0;
        Map<String,Integer> headToCount = new TreeMap<String,Integer>();
        int sumOfPathLengths = 0;
        for (int i = 0; i < tree.length; ++i) {
            BreadthFirstPathIterator iter = new BreadthFirstPathIterator(tree[i]);
            while (iter.hasNext()) {
                DependencyPath p = iter.next();
                if (p.length() > 2)
                    break;
                sumOfPathLengths += p.length();
                System.out.println(p);
                pathCount++;
                String start = p.first().lemma();
                Integer count = headToCount.get(start);
                headToCount.put(start, (count == null) ? 1 : count + 1);
            }
        }
        assertEquals(expected, pathCount);
        assertEquals(220, sumOfPathLengths);
        assertEquals(PATH_START_COUNTS, headToCount);

        /*
        String[][] treeData = {
            {"cat", "n", "1"},
            {"is", "det", "obj"},
            {"dog", "n", "2"},
            {"and", "conj", "blah"},
            {"chicken", "n", "3"}
        };
        int[][] treeLinks = {
            {},
            {0, 2, 4},
            {3},
            {4},
            {}
        };
        DependencyTreeNode[] tree = makeTree(treeData, treeLinks);
        Iterator<DependencyPath> pathIter = new BreadthFirstPathIterator(
                tree[1]);

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "cat");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "dog");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 1, "obj", "is", "chicken");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 2, "obj", "is", "and");

        assertTrue(pathIter.hasNext());
        testPath(pathIter.next(), 3, "obj", "is", "chicken");

        assertFalse(pathIter.hasNext());
        */
    }
}
