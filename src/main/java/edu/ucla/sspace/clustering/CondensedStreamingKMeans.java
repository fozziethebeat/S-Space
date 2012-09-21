/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;


/**
 * An extension of {@link StreamingKMeans} that further condenses the results by
 * running {@link HierarchicalAgglomerativeClustering} over the resulting
 * clusters returned by {@link StreamingKMeans}.
 *
 * @author Keith Stevens
 */
public class CondensedStreamingKMeans implements Clustering {

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        OnlineClustering<DoubleVector> results = computeStreamingCluster(
                matrix);
        int[] assignments = HierarchicalAgglomerativeClustering.clusterRows(
                Matrices.asMatrix(collectCentroids(results)), 
                .15, ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);

        List<Cluster<DoubleVector>> newClusters = mergeClusters(
                assignments, results.getClusters());
        return computeAssignments(newClusters, matrix.rows());
    }
    
    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, 
                               int numClusters,
                               Properties props) {
        OnlineClustering<DoubleVector> results = computeStreamingCluster(
                matrix);
        int[] assignments = HierarchicalAgglomerativeClustering.partitionRows(
                Matrices.asMatrix(collectCentroids(results)), 
                numClusters, ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);

        List<Cluster<DoubleVector>> newClusters = mergeClusters(
                assignments, results.getClusters());
        return computeAssignments(newClusters, matrix.rows());
    }

    /**
     * Runs {@link StreamingKMeans} over the dataset and returns the resulting
     * clustering solution.
     */
    @SuppressWarnings("unchecked")
    public static <T extends DoubleVector> OnlineClustering<T> computeStreamingCluster(
            Matrix matrix) {
        StreamingKMeans<T> generator = new StreamingKMeans<T>();

        OnlineClustering<T> onlineClustering = generator.generate();
        for (int r = 0; r < matrix.rows(); ++r)
            onlineClustering.addVector((T) matrix.getRowVector(r));

        return onlineClustering;
    }

    /**
     * Transforms the clustering solution returned from {@link StreamingKMeans}
     * to a list of centroid vectors.
     */
    public static <T extends DoubleVector> List<T> collectCentroids(
            OnlineClustering<T> onlineClustering) {
        List<T> centroids = new ArrayList<T>();
        for (Cluster<T> cluster : onlineClustering.getClusters())
            centroids.add(cluster.centroid());
        return centroids;
    }

    /**
     * Combines vectors in a clustering solution.
     */
    public static <T extends DoubleVector> List<Cluster<T>> mergeClusters(
            int[] assignments, 
            List<Cluster<T>> clusters) {
        // Combine clusters determined to be merged by HAC.
        List<Cluster<T>> newClusters = new ArrayList<Cluster<T>>();

        // When a new assignment index is encountered, create an empty
        // cluster.  The first cluster assigned to that index becomes the
        // initial value.  Later clusters assigned to that index are merged
        // into the primary cluster.
        for (int i = 0; i < assignments.length; ++i) {
            int assignment = assignments[i];
            while (assignment >= newClusters.size())
                newClusters.add(null);

            Cluster<T> cluster = newClusters.get(assignment);
            if (cluster == null)
                newClusters.set(assignment, clusters.get(i));
            else
                cluster.merge(clusters.get(i));
        }
        return newClusters;
    }

    /**
     * Transforms a set of {@link Cluster} objects into an {@link Assignments}
     * objects.
     */
    public static <T extends DoubleVector> Assignments computeAssignments(
            List<Cluster<T>> clusters,
            int numDataPoints) {
        Assignments assignments = new Assignments(clusters.size(), numDataPoints);
        int clusterId = 0;
        for (Cluster<T> cluster : clusters) {
            BitSet ids = cluster.dataPointIds();
            for (int id = ids.nextSetBit(0); id >= 0; id=ids.nextSetBit(id+1))
                assignments.set(id, clusterId);
            clusterId++;
        }
        return assignments;
    }
}
