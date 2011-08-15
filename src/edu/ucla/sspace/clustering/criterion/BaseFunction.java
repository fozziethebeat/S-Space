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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseDynamicMagnitudeVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


/**
 * This {@link CriterionFunction} implements the basic functionality needed for
 * a majority of the functions available.  It works by first gathering a handful
 * of meta data for the data set, such as the cluster sizes, initial cluster
 * assignments, and initial centroids.  It then implements {@link #update(int)
 * update} and requires subclasses to implement functions for determining
 * the change in the criterion score due to moving a data point. 
 *
 * </p>
 *
 * Sub classes must implement {@link #getOldCentroidScore(DoubleVector, int)
 * getOldCentroidScore} and {@link #getNewCentroidScore(int, DoubleVector)
 * getNewCentroidScore}.  The first function returns the score for the current
 * datapoints cluster assignment when that data point is removed from the data
 * point.  The second function returns the score for an anternate cluster when
 * the current data point is placed in that cluster.
 *
 * </p>
 *
 * This base class also provides two key methods for assisting with compute the
 * above changes: {@link #modifiedMagnitudeSqrd(DoubleVector, DoubleVector)
 * modifiedMagnitudeSqrd} and {@link #modifiedMagnitude(DoubleVector,
 * DoubleVector) modifiedMagnitude}.  For both functions, the first method is
 * cosidered to be the cluster centroid that is being modified and the second
 * vector is the data point that is being added to the centroid, without
 * actually affecting the cluster.
 *
 * @author Keith Stevens
 */
public abstract class BaseFunction implements CriterionFunction {

    /**
     * The {@link Matrix} holding the data points.
     */
    protected List<DoubleVector> matrix;

    /**
     * The set of cluster assignments for each cluster.
     */
    protected int[] assignments;

    /**
     * The centroids representing each cluster.
     */
    protected DoubleVector[] centroids;

    /**
     * The number of data points found in each cluster.
     */
    protected int[] clusterSizes;

    /**
     * The cost computed for each cluster.  This is maintained seperatly so that
     * update can modify only the  two relevant clusters being modified.
     */
    protected double[] costs;

    /**
     * Constructs a new {@link BaseFunction}.
     */
    public BaseFunction() {
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
    BaseFunction(List<DoubleVector> matrix,
                 DoubleVector[] centroids,
                 double[] costs,
                 int[] assignments,
                 int[] clusterSizes) {
        this.matrix = matrix;
        this.centroids = centroids;
        this.costs = costs;
        this.assignments = assignments;
        this.clusterSizes = clusterSizes;
    }

    /**
     * {@inheritDoc}
     */
    public void setup(Matrix m, int[] initialAssignments, int numClusters) {
        // Save the meta data we need to maintain.
        assignments = initialAssignments;
        matrix = new ArrayList<DoubleVector>(m.rows());
        for (int i = 0; i < m.rows(); ++i)
            matrix.add(m.getRowVector(i));

        // Initialize the cluster information.
        centroids = new DoubleVector[numClusters];
        clusterSizes = new int[numClusters];
        costs = new double[numClusters];

        // Initialize the clusters.
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseVector(m.columns());

        // Form the cluster composite vectors, i.e. unscaled centroids.
        for (int i = 0; i < m.rows(); ++i) {
            int assignment = initialAssignments[i];
            VectorMath.add(centroids[assignment], matrix.get(i));
            clusterSizes[assignment]++;
        }

        // Compute the cost of each centroid.
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseDynamicMagnitudeVector(
                    centroids[c].toArray());

        subSetup(m);

        SparseDoubleVector empty = new CompactSparseVector(m.columns());
        for (int c = 0; c < numClusters; ++c)
            if (clusterSizes[c] != 0)
                costs[c] = getOldCentroidScore(empty, c, clusterSizes[c]);
    }

    /**
     * Setup any extra information needed before computing the cost values for
     * each cluster.
     */
    protected void subSetup(Matrix m) {
    }

    /**
     * {@inheritDoc}
     */
    public boolean update(int currentVectorIndex) {
        int currentClusterIndex = assignments[currentVectorIndex];

        double bestDelta = (isMaximize()) ? 0 : Double.MAX_VALUE;
        int bestDeltaIndex = -1;

        // Get the current vector.
        DoubleVector vector = matrix.get(currentVectorIndex);

        // Get the current centroid without the current data point assigned to
        // it.  Compute the cost delta with that point removed from the cluster.
        //DoubleVector altCurrentCentroid = subtract(
        //        centroids[currentClusterIndex], vector);
        double deltaBase = (clusterSizes[currentClusterIndex] == 1)
            ? 0
            : getOldCentroidScore(vector, currentClusterIndex,
                                  clusterSizes[currentClusterIndex] - 1);
        deltaBase -= costs[currentClusterIndex];

        // Compute the cost delta for moving that data point to each of the
        // other possible clusters.
        for (int i = 0; i < centroids.length; ++i) {
            // Skip the cluster the data point is already assigned to.
            if (currentClusterIndex == i)
                continue;

            // Compute the cost of adding the data point to the current
            // alternate cluster.
            double newCost = getNewCentroidScore(i, vector);

            // Compute the cost delta for that change and the removal from the
            // data points original cluster.
            double delta = newCost - costs[i] + deltaBase;

            if (isMaximize()) {
                // Remember this move if it's positive and the best seen so far.
                // Negative detlas can be safely ignored since we only want to
                // maximize the cost.
                if (delta > 0 && delta > bestDelta) {
                    bestDelta = delta;
                    bestDeltaIndex = i;
                }
            } else {
                // Remember this move if it's the best seen so far.
                if (delta < bestDelta) {
                    bestDelta = delta;
                    bestDeltaIndex = i;
                }
            }
        }

        // If the best delta index was modified, make an update and return true.
        if (bestDeltaIndex >= 0) {
            // Change the costs.
            double newDelta = bestDelta - deltaBase;
            costs[currentClusterIndex] += deltaBase;
            costs[bestDeltaIndex] += newDelta;
            updateScores(bestDeltaIndex, currentClusterIndex, vector);

            // Update the sizes.
            clusterSizes[currentClusterIndex]--;
            clusterSizes[bestDeltaIndex]++;

            // Update the centroids.
            centroids[currentClusterIndex] = subtract(
                centroids[currentClusterIndex], vector);
            centroids[bestDeltaIndex] = VectorMath.add(
                centroids[bestDeltaIndex], vector);

            // Update the assignment.
            assignments[currentVectorIndex] = bestDeltaIndex;
            return true;
        }

        // Otherwise, this data point cannot be relocated, so return false.
        return false;
    }

    /**
     * Returns the new score for the cluster centroid represented by {@code
     * altCurrentCentroid} with the new {@code altClusterSize}.  
     *
     * @param altCurrentCentroid The current updated cluster centroid
     * @param altClusterSize The current updated cluster size
     */
    protected abstract double getOldCentroidScore(DoubleVector vector,
                                                  int oldCentroidIndex,
                                                  int altClusterSize);

    /**
     * Returns the new score for the cluster centroid indexed by {@code
     * newCentroidIndex} when {@code dataPoint} is added to it.  Implementations
     * of this method should not actually add {@code dataPoint} to the centroid,
     * but should instead use the helper functions provided to compute the new
     * score.
     *
     * @param newCentroidIndex The index of the current alternate centroid
     * @param dataPoint The current data point that is being reassigned
     */
    protected abstract double getNewCentroidScore(int newCentroidIndex,
                                                  DoubleVector dataPoint);

    protected void updateScores(int newCentroidIndex,
                                int oldCentroidIndex,
                                DoubleVector vector) {
    }

    /**
     * Returns a {@link DoubleVector} that is equal to {@code c - v}.  This
     * method is used instead of the one in {@link VectorMath} so that a {@link
     * DenseDynamicMagnitudeVector} can be used to represent the difference.
     * This vector type is optimized for when many calls to magnitude are
     * interleaved with updates to a few dimensions in the vector.
     */
    protected static DoubleVector subtract(DoubleVector c, DoubleVector v) {
        DoubleVector newCentroid = new DenseDynamicMagnitudeVector(c.length());

        // Special case sparse double vectors so that we don't incure a possibly
        // log n get operation for each zero value, as that's the common case
        // for CompactSparseVector.
        if (v instanceof SparseDoubleVector) {
            SparseDoubleVector sv = (SparseDoubleVector) v;
            int[] nonZeros = sv.getNonZeroIndices();
            int sparseIndex = 0;
            for (int i = 0; i < c.length(); ++i) {
                double value = c.get(i);
                if (sparseIndex < nonZeros.length &&
                    i == nonZeros[sparseIndex])
                    value -= sv.get(nonZeros[sparseIndex++]);

                newCentroid.set(i, value);
            }
        } else
            for (int i = 0; i < c.length(); ++i)
                newCentroid.set(i, c.get(i) - v.get(i));
        return newCentroid;
    }

    /**
     * {@inheritDoc}
     */
    public int[] assignments() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector[] centroids() {
        return centroids;
    }

    /**
     * {@inheritDoc}
     */
    public int[] clusterSizes() {
        return clusterSizes;
    }

    /**
     * {@inheritDoc}
     */
    public double score() {
        double score = 0;
        for (double cost : costs)
            score += cost;
        return score;
    }

    /**
     * Returns the magnitude squared of {@code c} as if {@code v} was added to
     * the vector.  We do this because it would be more costly, garbage
     * collection wise, to create a new vector for each alternate cluster and
     * then throw away all but one of them.
     */
    protected static double modifiedMagnitudeSqrd(DoubleVector c,
                                                  DoubleVector v) {
        if (v instanceof SparseDoubleVector) {
            SparseDoubleVector sv = (SparseDoubleVector) v;
            int[] nonZeros = sv.getNonZeroIndices();

            double magnitude = Math.pow(c.magnitude(), 2);
            for (int i : nonZeros) {
                double value = c.get(i);
                magnitude -= Math.pow(value, 2);
                magnitude += Math.pow(value + v.get(i), 2);
            }
            return magnitude;
        } else {
            double magnitude = 0;
            for (int i = 0; i < c.length(); ++i)
                magnitude += Math.pow(c.get(i) + v.get(i), 2);
            return magnitude;
        }
    }

    /**
     * Returns the magnitude of {@code c} as if {@code v} was added to the the
     * vector.  We do this because it would be more costly, garbage collection
     * wise, to create a new vector for each alternate cluster and * vector.
     */
    protected static double modifiedMagnitude(DoubleVector c, DoubleVector v) {
        return Math.sqrt(modifiedMagnitudeSqrd(c, v));
    }

    /**
     * Returns the magnitude squared of {@code c} as if {@code v} was added to
     * the vector.  We do this because it would be more costly, garbage
     * collection wise, to create a new vector for each alternate cluster and
     * then throw away all but one of them.
     */
    protected static double subtractedMagnitudeSqrd(DoubleVector c,
                                                   DoubleVector v) {
        if (v instanceof SparseDoubleVector) {
            SparseDoubleVector sv = (SparseDoubleVector) v;
            int[] nonZeros = sv.getNonZeroIndices();

            double magnitude = Math.pow(c.magnitude(), 2);
            for (int i : nonZeros) {
                double value = c.get(i);
                magnitude -= Math.pow(value, 2);
                magnitude += Math.pow(value - v.get(i), 2);
            }
            return magnitude;
        } else {
            double magnitude = 0;
            for (int i = 0; i < c.length(); ++i)
                magnitude += Math.pow(c.get(i) - v.get(i), 2);
            return magnitude;
        }
    }

    /**
     * Returns the magnitude of {@code c} as if {@code v} was added to the the
     * vector.  We do this because it would be more costly, garbage collection
     * wise, to create a new vector for each alternate cluster and * vector.
     */
    protected static double subtractedMagnitude(DoubleVector c, DoubleVector v) {
        return Math.sqrt(subtractedMagnitudeSqrd(c, v));
    }
}
