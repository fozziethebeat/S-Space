/*
 * Copyright 2010 David Jurgens
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

import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.Assignment;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.vector.*;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link LinkClustering}
 * class
 */
public class LinkClusteringTests {

    /**
     * Tests the link clustering example from the Ahn et al. supplementary
     * material on page 4.
     */
    @Test public void testPaperExample() {
        LinkClustering linkClustering = new LinkClustering();
        
        assertEquals(0, linkClustering.numberOfSolutions());

        UndirectedMultigraph<String> m = new UndirectedMultigraph<String>();
        // node 1 -> 2, 3, 4
        m.add(new SimpleTypedEdge<String>("type-1", 0, 1));
        m.add(new SimpleTypedEdge<String>("type-1", 0, 2));
        m.add(new SimpleTypedEdge<String>("type-1", 0, 3));
        // node 2 -> 1, 3, 4
        m.add(new SimpleTypedEdge<String>("type-1", 1, 2));
        m.add(new SimpleTypedEdge<String>("type-1", 1, 3));
        // node 3 -> 1, 2, 4
        m.add(new SimpleTypedEdge<String>("type-1", 2, 3));
        // node 4 -> 1, 2, 3, 5, 6, 7
        m.add(new SimpleTypedEdge<String>("type-1", 3, 4));
        m.add(new SimpleTypedEdge<String>("type-1", 3, 5));
        m.add(new SimpleTypedEdge<String>("type-1", 3, 6));
        // node 5 -> 4, 6
        m.add(new SimpleTypedEdge<String>("type-1", 4, 5));
        // node 6 -> 4, 5
        // node 7 -> 4, 8, 9
        m.add(new SimpleTypedEdge<String>("type-1", 6, 7));
        m.add(new SimpleTypedEdge<String>("type-1", 6, 8));
        // node 8 -> 7, 9
        m.add(new SimpleTypedEdge<String>("type-1", 7, 8));
        // node 9 -> 7, 8       


        Assignment[] assignments = linkClustering.cluster(m, new Properties());
        for (int i = 0; i < assignments.length; ++i) 
            System.out.printf("Node %d is in clusters %s%n",
                              i, Arrays.toString(assignments[i].assignments()));

        assertEquals(9, assignments.length);

        // Check that there were the right number of nodes in the dendrogram
        assertEquals(12, linkClustering.numberOfSolutions());
        
        // Nodes 1, 2, and 3 should all have one cluster, which is the same
        assertEquals(1, assignments[0].assignments().length);
        int clusterId = assignments[0].assignments()[0];
        assertEquals(1, assignments[1].assignments().length);
        assertEquals(clusterId, assignments[1].assignments()[0]);
        assertEquals(1, assignments[2].assignments().length);
        assertEquals(clusterId, assignments[2].assignments()[0]);
        
        // Node 4 should have 3 clusters
        assertEquals(3, assignments[3].assignments().length);

        // Nodes 5 and 6 should have one cluster, which is the same
        assertEquals(1, assignments[4].assignments().length);
        clusterId = assignments[4].assignments()[0];
        assertEquals(1, assignments[5].assignments().length);
        assertEquals(clusterId, assignments[5].assignments()[0]);

        // Node 7 should have two clusters
        assertEquals(2, assignments[6].assignments().length);

        // Nodes 8 and 9 should have one cluster, which is the same
        assertEquals(1, assignments[7].assignments().length);
        clusterId = assignments[7].assignments()[0];
        assertEquals(1, assignments[8].assignments().length);
        assertEquals(clusterId, assignments[8].assignments()[0]);

        int numSol = linkClustering.numberOfSolutions();
        for (int i = 0; i < numSol; ++i) {
            System.out.printf("Solution %d had density %f%n", i,
                             linkClustering.getSolutionDensity(i));
            assertEquals(9, linkClustering.getSolution(i).length);            
        }
    }

    private static SparseDoubleVector newVec(int[] dimsToSet, int dims) {
        SparseDoubleVector sv = new CompactSparseVector(dims); 
        for (int i : dimsToSet)
            sv.set(i, 1);
        return sv;
    } 
    
}