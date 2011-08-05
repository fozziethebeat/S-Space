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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrices;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.logging.Logger;


/**
 * An implementation of the Bisecting K-Means algorithm, also known as Repeated
 * Bisections.  This implementation is based on the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif">Michael Steinbach,
 *   George Karypis, Vipin Kumar.  "A comparison of document clustering
 *   techniques," in <i>KDD Workshop on Text Mining</i>, 200</li>
 *
 * This clustering algorithm improves upon the standard K-Means algorithm by
 * taking a data set and repeatedly splitting the data points into two regions.
 * Initially all data points are separated into two clusters.  Then, until the
 * desired number of clusters are created, the largest cluster is divided using
 * K-Means with K equal to 2.  This implementation relies on the {@link
 * KMeansClustering} implementation.  Any properties passed to this clustering
 * method are passed onto the {@link KMeansClustering} algorithm, allowing the
 * user to set the desired seeding method.
 *
 * @see KMeansClustering
 * 
 * @author Keith Stevens
 */
public class BisectingKMeans implements Clustering {

    /**
     * Not implemented.
     */
    public Assignments cluster(Matrix dataPoints, Properties props) {
        throw new UnsupportedOperationException(
                "KMeansClustering requires that the " +
                "number of clusters be specified");
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix dataPoints,
                               int numClusters,
                               Properties props) {
        // Handle a simple base case.
        if (numClusters <= 1) {
            Assignment[] assignments = new Assignment[dataPoints.rows()];
            for (int i = 0; i < assignments.length; ++i)
                assignments[i] = new HardAssignment(0);
            return new Assignments(numClusters, assignments);
        }

        // Create a count of cluster assignments.
        int[] numAssignments = new int[numClusters];

        // Create a list of lists.  The inner list represents the vectors
        // assigned to a particular cluster.  We use this method so that we can
        // easily transform the cluster to a Matrix
        List<List<DoubleVector>> clusters = new ArrayList<List<DoubleVector>>(
                numClusters);
        for (int c = 0; c < numClusters; ++c)
            clusters.add(new ArrayList<DoubleVector>());

        Clustering clustering = new DirectClustering();
        // Make the first bisection.
        Assignment[] assignments =
            clustering.cluster(dataPoints, 2, props).assignments();

        // Count the number of assignments made to each cluster and move the
        // vectors in to the corresponding list.
        for (int i = 0; i < assignments.length; ++i) {
            int assignment = assignments[i].assignments()[0];
            numAssignments[assignment]++;
            clusters.get(assignment).add(dataPoints.getRowVector(i));
        }

        // Generate the numClusters - 2 clusters by finding the largest cluster
        // and bisecting it.  Of the 2 resulting clusters, one will maintain the
        // same cluster index and the other will be given a new cluster index,
        // namely k, the current cluster number.
        for (int k = 2; k < numClusters; k++) {
            // Find the largest cluster.
            int largestSize = 0;
            int largestIndex = 0;
            for (int c = 0; c < numClusters; ++c) {
                if (numAssignments[c] > largestSize) {
                    largestSize = numAssignments[c];
                    largestIndex = c;
                }
            }

            // Get the list of vectors representing the cluster being split and
            // the cluster that will hold the vectors split off from this
            // cluster.
            List<DoubleVector> originalCluster = clusters.get(largestIndex);
            List<DoubleVector> newCluster = clusters.get(k);

            // Split the largest cluster.
            Matrix clusterToSplit = Matrices.asMatrix(originalCluster);
            Assignment[] newAssignments = 
                clustering.cluster(dataPoints, 2, props).assignments();

            // Clear the lists for cluster being split and the new cluster.
            // Also clear the number of assignments.
            originalCluster.clear();
            newCluster.clear();
            numAssignments[largestIndex] = 0;
            numAssignments[k] = 0;

            // Reassign data points in the largest cluster.  Data points
            // assigned to the 0 cluster maintain their cluster number in the
            // real assignment list.  Data points assigned to cluster 1 get the
            // new cluster number, k.  
            for (int i = 0, j = 0; i < dataPoints.rows(); ++i) {
                if (assignments[i].assignments()[0] == largestIndex) {
                    // Make the assignment for vectors that keep their
                    // assignment.
                    if (newAssignments[j].assignments()[0] == 0) {
                        originalCluster.add(dataPoints.getRowVector(i));
                        numAssignments[largestIndex]++;
                    }
                    // Make the assignment for vectors that have changed their
                    // assignment.
                    else {
                        newCluster.add(dataPoints.getRowVector(i));
                        assignments[i] = new HardAssignment(k);
                        numAssignments[k]++;
                    }
                    j++;
                }
            }
        }
        return new Assignments(numClusters, assignments);
    }

    public String toString() {
        return "BisectingKMeans";
    }
}
