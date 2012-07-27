/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.graph;

import edu.ucla.sspace.util.*;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class ChineseWhispersTest {

    @Test public void testPaperExample() {
        Graph<Edge> g = new SparseUndirectedGraph();
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(1, 4));
        g.add(new SimpleEdge(1, 5));
        g.add(new SimpleEdge(1, 11));

        g.add(new SimpleEdge(2, 3));
        g.add(new SimpleEdge(2, 4));
        g.add(new SimpleEdge(2, 5));
        g.add(new SimpleEdge(2, 8));

        g.add(new SimpleEdge(3, 4));
        g.add(new SimpleEdge(3, 5));

        g.add(new SimpleEdge(4, 5));

        g.add(new SimpleEdge(6, 7));
        g.add(new SimpleEdge(6, 8));
        g.add(new SimpleEdge(6, 10));
        g.add(new SimpleEdge(6, 11));

        g.add(new SimpleEdge(7, 9));
        g.add(new SimpleEdge(7, 10));
        g.add(new SimpleEdge(7, 11));

        g.add(new SimpleEdge(8, 6));
        g.add(new SimpleEdge(8, 9));
        g.add(new SimpleEdge(8, 11));

        g.add(new SimpleEdge(9, 10));
        g.add(new SimpleEdge(9, 11));

        g.add(new SimpleEdge(10, 11));

        

        MultiMap<Integer,Integer> clustering = 
            new ChineseWhispersClustering().cluster(Graphs.pack(g));

        System.out.println(clustering);
    }

} 