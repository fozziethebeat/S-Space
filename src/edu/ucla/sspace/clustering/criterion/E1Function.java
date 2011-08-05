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

package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseDynamicMagnitudeVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.List;


/**
 * This {@link CriterionFunction} measures the external differences between
 * clusters.  It gives a better score to clustering solutions with centroids
 * that are more distant from the centroid for the data set as a whole.
 *
 * @author Keith Stevens
 */
public class E1Function extends BaseFunction {

    /**
     * The centroid for all data points if they were assigned to a single
     * cluster.
     */
    private DoubleVector completeCentroid;

    /**
     * The distance from each cluster to {@code completeCentroid}.
     */
    private double[] simToComplete;

    /**
     * Constructs a new {@link E1Function}.
     */
    public E1Function() {
    }

    /**
     * A package private constructor for all {@link CriterionFunction}s
     * subclassing from this {@link BaseFunction}.  This is to facilitate the
     * implementation of {@link HybridBaseFunction}.  The provided objects are
     * intended to replace those that would have been computed by {@link
     * #setup(Matrix, int[], int) setup} so that one class can do this work once
     * and then share the computed values with other functions.
     *
     * @param matrix The list of normalized data points that are to be
     *        clustered
     * @param centroids The set of centroids associated with the dataset.
     * @param costs The set of costs for each centroid.
     * @param assignments The initial assignments for each cluster.
     * @param clusterSizes The size of each cluster.
     * @param completeCentroid The new summation vector of all data points
     * @param simToComplete The distance from each cluster to {@code
     *        completeCentroid}
     */
    E1Function(List<DoubleVector> matrix,
               DoubleVector[] centroids,
               double[] costs,
               int[] assignments,
               int[] clusterSizes,
               DoubleVector completeCentroid,
               double[] simToComplete) {
        super(matrix, centroids, costs, assignments, clusterSizes);
        this.completeCentroid = completeCentroid;
        this.simToComplete = simToComplete;
    }

    /**
     * {@inheritDoc}
     */
    protected void subSetup(Matrix m) {
        completeCentroid = new DenseDynamicMagnitudeVector(m.rows());
        for (DoubleVector v : matrix)
            VectorMath.add(completeCentroid, v);

        simToComplete = new double[centroids.length];
        for (int c = 0; c < centroids.length; ++c)
            simToComplete[c] = dotProduct(centroids[c], completeCentroid);
    }

    /**
     * {@inheritDoc}
     */
    protected double getOldCentroidScore(DoubleVector vector,
                                         int oldCentroidIndex,
                                         int altClusterSize) {
        double newScore = simToComplete[oldCentroidIndex];
        newScore -= dotProduct(completeCentroid, vector);
        newScore /= subtractedMagnitude(centroids[oldCentroidIndex], vector);
        newScore *= altClusterSize;
        return newScore;
    }

    /**
     * {@inheritDoc}
     */
    protected double getNewCentroidScore(int newCentroidIndex,
                                         DoubleVector dataPoint) {
        double newScore = dotProduct(completeCentroid, dataPoint);
        newScore += simToComplete[newCentroidIndex];
        newScore /= modifiedMagnitude(centroids[newCentroidIndex], dataPoint);
        newScore *= (clusterSizes[newCentroidIndex] + 1);
        return newScore;
    }

    /**
     * {@inheritDoc}
     */
    public boolean maximize() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    protected void updateScores(int newCentroidIndex,
                                int oldCentroidIndex,
                                DoubleVector vector) {
        simToComplete[newCentroidIndex] += dotProduct(
                completeCentroid, vector);
        simToComplete[oldCentroidIndex] -= dotProduct(
                completeCentroid, vector);
    }
}
