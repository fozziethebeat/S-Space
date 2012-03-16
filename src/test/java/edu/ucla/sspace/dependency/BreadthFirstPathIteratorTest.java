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
        "Anarchism\tanarchism\tNN\t1\t2\tSBJ\n" +
        "is\tbe\tVBZ\t2\t0\tROOT\n" +
        "a\ta\tDT\t3\t5\tNMOD\n" +
        "political\tpolitical\tJJ\t4\t5\tNMOD\n" +
        "philosophy\tphilosophy\tNN\t5\t2\tPRD\n" +
        "encompassing\tencompass\tVVG\t6\t5\tNMOD\n" +
        "theories\ttheory\tNNS\t7\t6\tOBJ\n" +
        "and\tand\tCC\t8\t7\tCC\n"+
        "attitudes\tattitude\tNNS\t9\t7\tCOORD\n"+
        "which\twhich\tWDT\t10\t11\tSBJ\n"+
        "consider\tconsider\tVVP\t11\t9\tNMOD\n"+
        "the\tthe\tDT\t12\t13\tNMOD\n"+
        "state\tstate\tNN\t13\t15\tSBJ\n"+
        "to\tt\tTO\t14\t15\tVMOD\n"+
        "be\tbe\tVB\t15\t11\tOBJ\n"+
        "unnecessary\tunnecessary\tJJ\t16\t15\tPRD\n"+
        ",\t,\t,\t17\t16\tP\n"+
        "harmul\tharmful\tJJ\t18\t16\tCOORD\n"+
        ",\t,\t,\t19\t16\tP\n"+
        "and/\tad/\tJJ\t20\t16\tCOORD\n"+
        "or\tor\tCC\t21\t16\tCC\n"+
        "undesirable\tundesirable\tJJ\t22\t16\tCOORD\n"+
        ".\t.\tSENT\t23\t2\tP\n";

    static final Map<String,Integer> PATH_START_COUNTS
        = new TreeMap<String,Integer>();

    static {
        PATH_START_COUNTS.put(",", 14);
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

    private static final int expected = 134;

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
        assertEquals(224, sumOfPathLengths);
        assertEquals(PATH_START_COUNTS, headToCount);
    }
}
