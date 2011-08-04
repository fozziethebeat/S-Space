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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.vector.*;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * A collection of tests for the {@link HierarchicalAgglomerativeClustering}
 * class
 */
public class HierarchicalAgglomerativeClusteringTests {

    @Test public void testSingleLinkageDendogram() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 3, 4 }, 100));
        vectors.add(newVec(new int[] { 3, 4, 5 }, 100));
        SparseMatrix m = Matrices.asSparseMatrix(vectors);

        HierarchicalAgglomerativeClustering hac =
            new HierarchicalAgglomerativeClustering();
        List<Merge> mergeOrder = hac.buildDendogram(
                m, ClusterLinkage.SINGLE_LINKAGE, SimType.COSINE);
        assertEquals(3, mergeOrder.size());
        assertEquals(0, mergeOrder.get(0).remainingCluster());
        assertEquals(1, mergeOrder.get(0).mergedCluster());
        assertEquals(2, mergeOrder.get(1).remainingCluster());
        assertEquals(3, mergeOrder.get(1).mergedCluster());
        assertEquals(0, mergeOrder.get(2).remainingCluster());
        assertEquals(2, mergeOrder.get(2).mergedCluster());
    }

    @Test public void testCompleteLinkageDendrogram() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 3, 4 }, 100));
        vectors.add(newVec(new int[] { 3, 4, 5 }, 100));
        SparseMatrix m = Matrices.asSparseMatrix(vectors);

        HierarchicalAgglomerativeClustering hac =
            new HierarchicalAgglomerativeClustering();
        List<Merge> mergeOrder = hac.buildDendogram(
            m, ClusterLinkage.COMPLETE_LINKAGE, SimType.COSINE);
        assertEquals(3, mergeOrder.size());
        assertEquals(0, mergeOrder.get(0).remainingCluster());
        assertEquals(1, mergeOrder.get(0).mergedCluster());
        assertEquals(2, mergeOrder.get(1).remainingCluster());
        assertEquals(3, mergeOrder.get(1).mergedCluster());
        assertEquals(0, mergeOrder.get(2).remainingCluster());
        assertEquals(2, mergeOrder.get(2).mergedCluster());
    }

    @Test public void testMeanLinkageDendogram() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 3, 4 }, 100));
        vectors.add(newVec(new int[] { 3, 4, 5 }, 100));
        SparseMatrix m = Matrices.asSparseMatrix(vectors);

        HierarchicalAgglomerativeClustering hac =
            new HierarchicalAgglomerativeClustering();
        List<Merge> mergeOrder = hac.buildDendogram(
            m, ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);
        assertEquals(3, mergeOrder.size());
        assertEquals(0, mergeOrder.get(0).remainingCluster());
        assertEquals(1, mergeOrder.get(0).mergedCluster());
        assertEquals(2, mergeOrder.get(1).remainingCluster());
        assertEquals(3, mergeOrder.get(1).mergedCluster());
        assertEquals(0, mergeOrder.get(2).remainingCluster());
        assertEquals(2, mergeOrder.get(2).mergedCluster());
    }

    @Test public void testMedianLinkageDendogram() {
        List<SparseDoubleVector> vectors = new ArrayList<SparseDoubleVector>();
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 1, 2 }, 100));
        vectors.add(newVec(new int[] { 3, 4 }, 100));
        vectors.add(newVec(new int[] { 3, 4, 5 }, 100));
        SparseMatrix m = Matrices.asSparseMatrix(vectors);

        HierarchicalAgglomerativeClustering hac =
            new HierarchicalAgglomerativeClustering();
        List<Merge> mergeOrder = hac.buildDendogram(
            m, ClusterLinkage.MEDIAN_LINKAGE, SimType.COSINE);
        assertEquals(3, mergeOrder.size());
        assertEquals(0, mergeOrder.get(0).remainingCluster());
        assertEquals(1, mergeOrder.get(0).mergedCluster());
        assertEquals(2, mergeOrder.get(1).remainingCluster());
        assertEquals(3, mergeOrder.get(1).mergedCluster());
        assertEquals(0, mergeOrder.get(2).remainingCluster());
        assertEquals(2, mergeOrder.get(2).mergedCluster());
    }

    private static SparseDoubleVector newVec(int[] dimsToSet, int dims) {
        SparseDoubleVector sv = new CompactSparseVector(dims); 
        for (int i : dimsToSet)
            sv.set(i, 1);
        return sv;
    } 
    
}
