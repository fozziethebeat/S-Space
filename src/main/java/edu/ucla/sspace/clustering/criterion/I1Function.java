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

import edu.ucla.sspace.vector.DoubleVector;

import java.util.List;


/**
 * This {@link CriterionFunction} measures the amount of internal similarity for
 * each computed centroid.  Centroids with higher internal similarity are given
 * higher scores.  It uses the square of the magnitude of each centroid as the
 * basis for this measurement.
 *
 * @author Keith Stevens
 */
public class I1Function extends BaseFunction {

    /**
     * Constructs a new {@link BaseFunction}.
     */
    public I1Function() {
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
     */
    I1Function(List<DoubleVector> matrix,
               DoubleVector[] centroids,
               double[] costs,
               int[] assignments,
               int[] clusterSizes) {
        super(matrix, centroids, costs, assignments, clusterSizes);
    }

    /**
     * {@inheritDoc}
     */
    protected double getOldCentroidScore(DoubleVector vector,
                                         int oldCentroidIndex,
                                         int altClusterSize) {
        return subtractedMagnitudeSqrd(centroids[oldCentroidIndex], vector) / 
               altClusterSize;
       // Math.pow(altCurrentCentroid.magnitude(), 2) / altClusterSize;
    }

    /**
     * {@inheritDoc}
     */
    protected double getNewCentroidScore(int newCentroidIndex,
                                         DoubleVector dataPoint) {
        return modifiedMagnitudeSqrd(centroids[newCentroidIndex], dataPoint) / 
               (clusterSizes[newCentroidIndex]+1);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMaximize() {
        return true;
    }
}
