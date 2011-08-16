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

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;


/** 
 * This interface defines criteria methods used for {@link
 * edu.ucla.sspace.clustering.DirectClustering DirecClustering}.  Each criteria
 * method tries to maximize of minimize some objective function that measures
 * inter cluster similarity and/or intra cluster similarity.  Before calling
 * {@link #update(int, int) update}, {@link #setup(Matrix, int[], int]) setup}
 * must be called, which is reponsible for creating any {@link Matrix} specific
 * meta data.
 *
 * @author Keith Stevens
 */
public interface CriterionFunction {

    /**
     * Creates the cluster centroids and any other meta data needed by this
     * {@link CriterionFunction}.
     *
     * @param m The {@code Matrix} holding data points.  This will be used as
     *        read only.
     * @param initialAssignments The cluster assignments for each data point in
     *        {@code m}.  This is used as read only and discarded.
     * @param numClustesr The number of clusters to create.
     */
    void setup(Matrix m, int[] initialAssignments, int numClusters);

    /**
     * Updates the clustering assignment for data point indexed by {@code
     * currentVectorIndex}.  This returns {@code true} if the data point is left
     * in the same cluster and false if it was relocated to another data point.
     */
    boolean update(int currentVectorIndex);

    /**
     * Returns the cluster assignment indices for each data point in the
     * original matrix passed to {@link #setup(Matrix, int[] int) setup).
     */
    int[] assignments();

    /**
     * Returns the final set of centroids computed for the dataset passed to
     * {@link #setup(Matrix, int[], int) setup}.
     */
    DoubleVector[] centroids();

    /**
     * Returns the number of data points assigned to each cluster.
     */
    int[] clusterSizes();

    /**
     * Returns the score computed by this {@link CriterionFunction}.
     */
    double score();

    /**
     * Returns true if this {@link CriterionFunction} tries to maximize it's
     * score, and false otherwise.
     */
    boolean isMaximize();
}
