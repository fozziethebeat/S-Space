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
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.VectorMath;


/**
 * Select seeds using a modification of the ORSS algorithm.  Seeds are selected
 * from the data such that they are well scattered.  This implementation has a
 * runtime of O(nkd) and is is based on the following paper:
 *
 *     <li style="font-family:Garamond, Georgia, serif"> Rafail Ostrovsky ,
 *     Yuval Rabani ,  Leonard J. Schulman ,  Chaitanya Swamy, "The
 *     effectiveness of lloyd-type methods for the k-means problem," in
 *     <i>47th IEEE Symposium on the Foundations of Computer Science</i>,
 *     2006</li>
 *
 * This involves 4 steps:
 * <ol>
 *   <li>First, the cost of an optimal solution to the K-Means
 *       problem is solved for the 1 cluster case along (called
 *       optimalSingleCost) with the centroid for this case (called c).</li>
 *   <li>Then, an intial seed is selected from the set of data points (X)
 *       with 
 *       probability p((optimalSingleCost + n*euclieanDist(x,c)^2)/2*n*optimalSingleCost) </li>
 *   <li>The second centroid is selected from X with probability
 *        p(euclideanDist(x, c_1)^2/(optimalSingleCost + n*euclideanDist(c,
 *        c_1)^2)) </li>
 *   <li>The remaining k-2 centroids are selected from X with probability
 *       p(euclideanDist(x, c_nearest)^2)
 *   </li>
 * </ol>
 *
 * @author Keith Stevens
 */
public class OrssSeed implements KMeansSeed {

    /**
     * A small number used to determine when the centroids have converged.
     */
    private static final double EPSILON = 1e-3;

    /**
     * {@inheritDoc}
     */
    public DoubleVector[] chooseSeeds(int numCentroids, Matrix dataPoints) {
        int numDataPoints = dataPoints.rows();
        DoubleVector[] centroids = new DoubleVector[numCentroids];

        // Compute the centroid assuming that there is only one cluster to be
        // found.  This is required for computing the optimal cost of a single
        // cluster solution.
        DoubleVector singleCentroid = new DenseVector(dataPoints.columns());
        for (int i = 0; i < numDataPoints; ++i)
            VectorMath.add(singleCentroid, dataPoints.getRowVector(i));
        singleCentroid = new ScaledDoubleVector(
                singleCentroid, 1/((double)numDataPoints));

        // Compute the optimal cost of the single cluster case.
        double optimalSingleCost = 0;
        double[] distances = new double[numDataPoints];
        for (int i = 0; i < numDataPoints; ++i) {
            distances[i] = Similarity.euclideanDistance(
                    singleCentroid, dataPoints.getRowVector(i));
            optimalSingleCost += distances[i];
        }

        // Select the first centroid.  We pick the first centroid based on the
        // probability p((optimalSingleCost + n*euclideanDistance(x, c)^2)/
        //               2*n*optimalSingleCost)
        double probability = Math.random();
        double optimalDistance = 0;
        for (int i = 0; i < numDataPoints; ++i) {
            // Get the distance between each data point and the single
            // centroid.
            double distance = distances[i];
            // Compute the probability of selecting this data point.
            double cost = optimalSingleCost + numDataPoints*distance*distance;
            cost /= (2 * numDataPoints * optimalSingleCost);

            // Determine whether or not we should select the data point based on
            // it's probability.  Also save the distance from this new centroid
            // to the single centroid.
            probability -= cost;
            if (probability <= EPSILON) {
                centroids[0] = dataPoints.getRowVector(i);
                optimalDistance = distance;
                break;
            }
        }

        // Select the second centroid.
        probability = Math.random();
        
        // Precompute the normalizing factor for the probability of selecting a
        // data point.  In short this should be:
        //   optimalSingleCost * n*|euclideanDist(c, c_1)|^2
        double normalFactor = 
            optimalSingleCost + numDataPoints*Math.pow(optimalDistance, 2);

        // Store the set minimum distance each data point has to a single
        // centroid.  This is used later on for selecting the last k-2
        // centroids.
        double[] minDistances = new double[numDataPoints];
        boolean selected = false;
        for (int i = 0; i < numDataPoints; ++i) {
            // Compute the distance.  Since the first centroid is the only one,
            // we know its the closest so store the distance.
            double distance = Similarity.euclideanDistance(
                    centroids[0], dataPoints.getRowVector(i));
            minDistances[i] = distance;

            // Determine whether or not we should select this data point as the
            // second centroid.
            double cost = Math.pow(distance, 2) / normalFactor;
            probability -= cost;
            if (probability <= EPSILON && !selected) {
                centroids[1] = dataPoints.getRowVector(i);
                selected = true;
            }
        }

        // Select the remaining k-2 centroids.
        // We pick a data point to be a new centroid based on the following
        // probability:
        //   p(|euclideanDist(dataPoint, nearestCentroid)|^2)
        // Since we store the minimum distance each data point has to any given
        // centroid in minDistances, all we need to do is update that with the
        // distance between each data point and the most recently selected
        // centroid.
        for (int c = 2; c < numCentroids; ++c) {
            selected = false;
            probability = Math.random();
            for (int i = 0; i < numDataPoints; ++i) {
                // Compute the distance between each data point and the most
                // recently selected centroid.  Update the minimum distance
                // array if the recent centroid is nearer.
                double distance = Similarity.euclideanDistance(
                        centroids[c-1], dataPoints.getRowVector(i));
                if (distance < minDistances[c])
                    minDistances[i] = distance;

                // Determine whether or not we should select this data point as
                // the next centroid.
                double cost = Math.pow(minDistances[i], 2);
                probability -= cost;
                if (probability <= EPSILON && !selected) {
                    centroids[c] = dataPoints.getRowVector(i);
                    selected = true;
                }
            }
        }

        return centroids;
    }
}
