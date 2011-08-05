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

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


public class ClusterUtil {

    /**
     * Returns the centroids of a {@link Matrix} based on the assignments for
     * each data point.  This is only a helper function for users of {@link
     * KMeansClustering} so that they can reconstruct the centroids.
     */
    public static DoubleVector[] computeCentroids(Matrix dataPoints,
                                                  Assignment[] assignments,
                                                  DoubleVector[] centroids) {
        double[] numAssignments = new double[centroids.length];
        for (int i = 0; i < dataPoints.rows(); ++i) {
            // Skip any data points that could not be clustered.
            if (assignments[i].assignments().length == 0)
                continue;

            int assignment = assignments[i].assignments()[0];
            VectorMath.add(centroids[assignment], dataPoints.getRowVector(i));
            numAssignments[assignment]++;
        }

        for (int c = 0; c < centroids.length; ++c)
            if (numAssignments[c] > 0)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c], 1/numAssignments[c]);
        return centroids;
    }


    /**
     * Returns the centroids of a {@link Matrix} based on the assignments for
     * each data point.  This is only a helper function for users of {@link
     * KMeansClustering} so that they can reconstruct the centroids.
     */
    public static DoubleVector[] computeCentroids(Matrix dataPoints,
                                                  Assignment[] assignments,
                                               SparseDoubleVector[] centroids) {
        double[] numAssignments = new double[centroids.length];
        for (int i = 0; i < dataPoints.rows(); ++i) {
            // Skip any data points that could not be clustered.
            if (assignments[i].assignments().length == 0)
                continue;

            int assignment = assignments[i].assignments()[0];
            VectorMath.add(centroids[assignment], dataPoints.getRowVector(i));
            numAssignments[assignment]++;
        }

        for (int c = 0; c < centroids.length; ++c)
            if (numAssignments[c] > 0)
                centroids[c] = new ScaledSparseDoubleVector(
                        centroids[c], 1/numAssignments[c]);
        return centroids;
    }


    /**
     * Returns the K-Means objective score of a given solution.
     */
    public static double computeObjective(Matrix dataPoints,
                                          DoubleVector[] centroids,
                                          Assignment[] assignments) {
        double objective = 0;
        for (int i = 0; i < dataPoints.rows(); ++i) {
            // Skip any data points that could not be clustered.
            if (assignments[i].assignments().length == 0)
                continue;

            int assignment = assignments[i].assignments()[0];
            objective += Math.pow(Similarity.euclideanDistance(
                    centroids[assignment], dataPoints.getRowVector(i)), 2);
        }
        return objective;
    }
}
