/*
 * Copyright 2011 David Jurgens
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

import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Tests for the {@link SamplingSubgraphIterator}.
 */
public class SamplingSubgraphIteratorTests { 

    @Test public void testConstructor() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 1, 1, 1});
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorNonpositive() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, -1, new double[] { 1, 1, 1});
    }

    @Test(expected=NullPointerException.class) public void testConstructorNull() {
        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(null, 10, new double[] { 1, 1, 1});
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorSizeTooLarge() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 20, new double[] { 1, 1, 1});
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorNegativeProbs() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { -1, 1, 1});
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorTooLargeProbs() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 2, 1, 1});
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorWrongNumberOfProbs() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.add(new SimpleEdge(i, j));
        }    

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 1, 1, 1, 1});
    }

    @Test public void testWorkedExample() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.add(i);
        }
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(1, 4));
        g.add(new SimpleEdge(1, 5));
        g.add(new SimpleEdge(2, 3));
        g.add(new SimpleEdge(2, 6));
        g.add(new SimpleEdge(2, 7));
        g.add(new SimpleEdge(3, 8));
        g.add(new SimpleEdge(3, 9));
        
        // Note that ESU = Rand-ESU when all the probabilities are 1, so we
        // expect this to work.
        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 1, 1, 1});
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(16, numSubgraphs);
    }

    @Test public void testWorkedExampleWithSampling() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.add(i);
        }
        g.add(new SimpleEdge(1, 2));
        g.add(new SimpleEdge(1, 3));
        g.add(new SimpleEdge(1, 4));
        g.add(new SimpleEdge(1, 5));
        g.add(new SimpleEdge(2, 3));
        g.add(new SimpleEdge(2, 6));
        g.add(new SimpleEdge(2, 7));
        g.add(new SimpleEdge(3, 8));
        g.add(new SimpleEdge(3, 9));
        
        int iters = 100;
        int totalSubgraphsSeen = 0;
        for (int i = 0; i < iters; ++i) {
            // We expect 8 on average
            SamplingSubgraphIterator<Edge> iter =
                new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 1, 1, .5});
            int subgraphs = 0;
            while (iter.hasNext()) {
                iter.next();
                subgraphs++;
            }
            System.out.printf("Saw %d subgraphs on iteration %d%n", subgraphs, i);
            totalSubgraphsSeen += subgraphs;
        }
        
        double average = ((double)totalSubgraphsSeen) / iters;
        System.out.printf("Saw %f subgraphs on average%n", average);
        assertEquals(8d, average, 1d);
    }
 
    @Test public void testNoSubgraph() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.add(i);
        }

        SamplingSubgraphIterator<Edge> iter = new SamplingSubgraphIterator<Edge>(g, 3, new double[] { 1, 1, 1});
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(0, numSubgraphs);
    }
    

}