/*
 * Copyright 2011 Keith Stevens 
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

package edu.ucla.sspace.clustering.seeding;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;


/**
 * This {@link KMeansSeed} implementation attempts to select centroids from the
 * set of data points that are well scattered.  This is done first selecting a
 * data point at random as the first centroid and then selected data points to
 * be the next centroid with a probability proportional to the distance between
 * the data point and the nearest centroid.  It is based on the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif">      David Arthur,
 *   Sergei Vassilvitskii, "k-means++: the advantages of careful seeding,"
 *   in <i>Symposium on Discrete Algorithms</i> and <i>Proceedings of the
 *   eighteenth annual ACM-SIAM symposium on Discrete algorithms</i>,
 *   2007</li>
 *
 * @author Keith Stevens
 */
public class KMeansPlusPlusSeed implements KMeansSeed {
    
    /**
     * A small number used to determine when the centroids have converged.
     */
    private static final double EPSILON = 1e-3;

    /**
     * Selects the best scattered {@link numCentroids} data points from {@link
     * dataPoints}.  The actual {@link DoubleVector}s are returned, not a copy
     * or wrapped version.
     */
    public DoubleVector[] chooseSeeds(int numCentroids, Matrix dataPoints) {
        int[] centroids = new int[numCentroids];
        // Select the first centroid randomly.
        DoubleVector[] centers = new DoubleVector[numCentroids];
        int centroidIndex = (int) Math.round(
                Math.random()*(dataPoints.rows()-1));
        centers[0] = dataPoints.getRowVector(centroidIndex);

        // Compute the distance each data point has with the first centroid.
        double[] distances = new double[dataPoints.rows()];
        computeDistances(distances, false, dataPoints, centers[0]);

        // For each of the remaining centroids, select one of the data points,
        // p, with probability
        // p(dist(p, last_centroid)^2/sum_p(dist(p, last_centroid)^2))
        // This is an attempt to pick the data point which is furthest away from
        // the previously selected data point.
        for (int i = 1; i < numCentroids; ++i) {
            double sum = distanceSum(distances);
            double probability = Math.random();
            centroidIndex = chooseWithProbability(distances, sum, probability);
            centers[i] = dataPoints.getRowVector(centroidIndex);
            computeDistances(distances, true, dataPoints, centers[i]);
        }

        return centers;
    }

    /**
     * Returns the sum of distances squared.
     */
    private static double distanceSum(double[] distances) {
        double sum = 0;
        for (double distance : distances)
            sum += Math.pow(distance, 2);
        return sum;
    }


    /**
     * Computes the distance between each data point and the given centroid.  If
     * {@code selectMin} is set to true, then this will only overwrite the
     * values in {@code distances} if the new distance is smaller.  Otherwise
     * the new distance will always be stored in {@code distances}.
     *
     * @param distances An array of distances that need to be updated.
     * @param selectMin Set to true a new distance must smaller than the
     *        current values in {@code distances}.
     * @param dataPoints The set of data points.
     * @param centroid The centroid to compare against.
     */
    private static void computeDistances(double[] distances,
                                         boolean selectMin,
                                         Matrix dataPoints,
                                         DoubleVector centroid) {
        for (int i = 0; i < distances.length; ++i) {
            double distance = Similarity.euclideanDistance(
                    centroid, dataPoints.getRowVector(i));
            if (!selectMin || selectMin && distance < distances[i])
                distances[i] = distance;
        }
    }

    /**
     * Returns a data point index i with probability 
     *   p(distances[i]^2/sum)
     */
    private static int chooseWithProbability(double[] distances,
                                             double sum,
                                             double probability) {
        for (int j = 0; j < distances.length; ++j) {
            double probOfDistance = Math.pow(distances[j], 2) / sum;
            probability -= probOfDistance;
            if (probability <= EPSILON) {
                return j;
            }
        }
        return distances.length-1;
    }

}
