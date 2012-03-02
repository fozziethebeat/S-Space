/*
 * Copyright 2012 David Jurgens
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

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntPair;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Arrays;


/**
 * A utility class for selected <i>k</i> data points as seeds from a list of
 * <i>n &gt;&gt; k</i> data points using a general method for comparing the
 * similarity (distance) of data points.  The seeds are intented to be used as
 * input to the <i>k</i>-means algorithm for the initial data points
 * (facilities) to which other points are assigned.  This implementation is
 * based on the work of
 *
 * <ul> <li> Rafail Ostrovsky, Yuval Rabani, Leonard Schulman, and Chaitanya
 *     Swamy. The Effectiveness of Lloyd-Type Methods for the k-Means
 *     Problem. In FOCS, 2006.</li> </ul>
 *
 * Unlike the {@link OrssSeed} implementation, this implementation is based on
 * using a {@link SimilarityFunction} to compare points.  In contrast, the
 * {@code OrssSeed} uses the Euclidean distance to compare points as in the
 * Ostrovsky et al. (2006) formulation.  The properties defined in the ORSS
 * paper are preserved if the similarity is defined as the inverse of the
 * squared Euclidean distances, which produces the same results as the {@code
 * OrssSeed} implemenation.  However, this implemenation generalizes the notion
 * of distance to inverse-similarity, which allows data to be compared using
 * alternate methods, such as {@link
 * edu.ucla.sspace.similarity.CosineSimilarity}, which is frequently used in
 * comparing text documents.  Note that the similarity values returned by any
 * {@code SimilarityFunction} used by this class must always be non-negative.
 *
 * <p> In addition, this class provides an additional overload of the {@code
 * chooseSeeds} method that allows the input data points to be weighed.
 * Weighting enables finding seeds where the input are representative of
 * different sample sizes.
 *
 * <p> This implementation is in part derived from the ORSS seed implementation
 * of Michael Shindler as a part of the Fast Streaming K-Means implementation
 * available <a
 * href="http://web.engr.oregonstate.edu/~shindler/kMeansCode/">here</a>.
 *
 * @see OrssSeed
 * @author David Jurgens
 */
public class GeneralizedOrssSeed implements KMeansSeed {

    /**
     * The similarity function used to compare targets.
     */
    private final SimilarityFunction simFunc;

    public GeneralizedOrssSeed(SimilarityFunction simFunc) {
        this.simFunc = simFunc;
    }

    /**
     * Selects {@code k} rows of {@code dataPoints} to be seeds of a
     * <i>k</i>-means instance.  If more seeds are requested than are available,
     * all possible rows are returned.
     *
     * @param dataPoints a matrix whose rows are to be evaluated and from which
     *        {@code k} data points will be selected
     * @param k the number of data points (rows) to select
     *
     * @return the set of rows that were selected
     */
    public DoubleVector[] chooseSeeds(int k, Matrix dataPoints) {
        // If no weights were selected, then just use a uniform weighting of 1.
        int[] weights = new int[dataPoints.rows()];
        Arrays.fill(weights, 1);
        return chooseSeeds(dataPoints, k, weights);
    }

    /**
     * Selects {@code k} rows of {@code dataPoints}, weighted by the specified
     * amount, to be seeds of a <i>k</i>-means instance.  If more seeds are
     * requested than are available, all possible rows are returned.
     *
     * @param dataPoints a matrix whose rows are to be evaluated and from which
     *        {@code k} data points will be selected
     * @param k the number of data points (rows) to select
     * @param weights as set of scalar int weights that reflect the importance
     *        of each data points.
     *
     * @return the set of rows that were selected
     */
    public DoubleVector[] chooseSeeds(Matrix dataPoints, int k, int[] weights) {

        IntSet selected = new TroveIntSet();
        int rows = dataPoints.rows();
        // Edge case for where the user has requested more seeds than are
        // available.  In this case, just return indices for all the rows
        if (rows <= k) {
            DoubleVector[] arr = new DoubleVector[rows];
            for (int i = 0; i < rows; ++i)
                arr[i] = dataPoints.getRowVector(i);
            return arr;
        }

        // This array keeps the relative probability of that index's data point
        // being selected as a centroid.  Although the probabilities change with
        // each center added, the array is only allocated once and is refilled
        // using determineProbabilities() method.
        double[] probabilities = new double[rows];

        // This array keeps the memoized computation of the maximum similarity
        // of each data point i, to any center currently in selected.  After the
        // first two points are selected, each iteration updates this array with
        // the maximum simiarlity of the new center to that point's index.
        double[] inverseSimilarities  = new double[rows];

        // Pick the first two centers, x, y, with probability proportional to
        // 1/sim(x, y).  In the original paper the probility is proportional to
        // ||x - y||^2, which is the square of the distance between the two
        // points.  However, since we use the simiarlity (which is conceptually
        // the inverse of distance), we use the inverse similarity so that
        // elements that are more similarity (i.e., larger values) have smaller
        // probabilities.
	IntPair firstTwoCenters = 
            pickFirstTwo(dataPoints, simFunc, weights, inverseSimilarities);
        selected.add(firstTwoCenters.x);
        selected.add(firstTwoCenters.y);

        // For the remaining k-2 points to select, pick a random point, x, with
        // probability min(1/sim(x, c_i)) for all centers c_i in selected.
        // Again, this probability-based selection is updated from the original
        // ORSS paper, which used || x - c_i ||^2 for all centers c.  See the
        // comment above for the reasoning.
	for (int i = 2; i < k; i++) {

            // First, calculate the probabilities for selecting each point given
            // its similarity to any of the currently selected centers
            determineProbabilities(inverseSimilarities, weights,  
                                   probabilities, selected);

            // Then sample a point from the multinomial distribution over the
            // remaining points in dataPoints
            int point = selectWithProb(probabilities);

            // Once we've selected a point, add it the set that we will return
            // and update the similarity all other non-selected points relative
            // to be the highest similarity to any selected point
            boolean added = selected.add(point);
            assert added : "Added duplicate row to the set of selected points";            
            updateNearestCenter(inverseSimilarities, dataPoints, 
                                point, simFunc);
	}

        IntIterator iter = selected.iterator();
        DoubleVector[] centroids = new DoubleVector[k];
        for (int i = 0; iter.hasNext(); ++i) 
            centroids[i] = dataPoints.getRowVector(iter.nextInt());
        return centroids;
    }

    
    private static IntPair pickFirstTwo(Matrix dataPoints, 
                                        SimilarityFunction simFunc, 
                                        int[] weights, double[] inverseSimilarities) {
	double OPT_1 = 0; // optimal 1-means cost.
	DoubleVector centerOfMass = new DenseVector(dataPoints.columns());
	double sum = 0d;
	int rows = dataPoints.rows();
        int cols = dataPoints.columns();
        double[] probs = new double[rows];
	int totalWeight = 0; 

	for (int i = 0; i < rows; i++) {
            DoubleVector v = dataPoints.getRowVector(i);
            int weight = weights[i];
            // Update the center of mass for the entire solution based
            VectorMath.add(centerOfMass, new ScaledDoubleVector(v, weight));
            totalWeight += weight;
        }
         
        // Then rescale the center of mass based on the total weight
        for (int j = 0; j < cols; j++) 
            centerOfMass.set(j, centerOfMass.get(j) / totalWeight);
        
        
        for (int i = 0; i < rows; i++) {
            double sim = simFunc.sim(centerOfMass, dataPoints.getRowVector(i));
            sim = invertSim(sim);
            inverseSimilarities[i] = sim;
            OPT_1 += sim * weights[i];
        }

        // Compute the probability mass of picking the first mean 
        for (int i = 0; i < rows; i++) {
            probs[i] = (OPT_1 + totalWeight * inverseSimilarities[i])
                / (2 * totalWeight * OPT_1);
            sum += probs[i];
        }
	
        // Normalize the relative mass assigned to each point to create a true
        // probability distribution that sums to 1.
        for (int i = 0; i < rows; i++) 
            probs[i] = probs[i] / sum;

	// Select the first center with probability proportional to its
	// dissimilarity from the center of mass
	int c1 = selectWithProb(probs);
        DoubleVector y = dataPoints.getRowVector(c1);

        // Keep the inverse similarity from the first center to the center of
        // mass
	double invSimFromCtrToC1 = invertSim(simFunc.sim(y, centerOfMass));
        
	// Recalculate inverseSimilarities and probs for selecting the second point.  Also
	// reset the probability of picking the first center again to 0
	sum = 0.0; 
	probs[c1] = 0;
        for (int i = 0; i < rows; i++) {
            // Skip assigning any probability mass to the first center's index
            // since it has already been selected
            if (i == c1) 
                continue;
            
            double sim = invertSim(simFunc.sim(dataPoints.getRowVector(i), y))
                * weights[i];
            inverseSimilarities[i] = sim;
            probs[i] = sim / ( OPT_1 + totalWeight * invSimFromCtrToC1);
            sum += probs[i];
        }

        // Normalize the probability masses to be probabilities
        for (int i = 0; i < rows; i++) 
		probs[i] = probs[i] / sum;
	
        // Select a second center
	int c2 = selectWithProb(probs);
        DoubleVector z = dataPoints.getRowVector(c2);

	inverseSimilarities[c1] = 0;
	inverseSimilarities[c2] = 0;
	
        // For each of the non-center points, assign it's initial inverse
        // similarity (i.e., distance) to be the minimum to either of the two
        // centers
        for (int i = 0; i < rows; i++) {
            DoubleVector v = dataPoints.getRowVector(i);
            double sim1 = simFunc.sim(v, y); // center 1
            sim1 = invertSim(sim1);
            double sim2 = simFunc.sim(v, z); // center 2
            sim2 = invertSim(sim2);
            inverseSimilarities[i] = Math.min(sim1, sim2);
        }
        return new IntPair(c1, c2);
    }

    /**
     * Inverts the similarity, effectively turning it into a distance
     */
    static double invertSim(double sim) {
        assert sim >= 0 : "negative similarity invalidates distance conversion";
        return (sim == 0) ? 0 : 1d / sim;
    }

    /**
     * Given an array of probability values (which sum to 1) samples an index in
     * {@code prob} according to its probability.
     */
    private static int selectWithProb(double probs[]) {
	double cutoff = Math.random();
	double curProbSum = 0.0;

	for (int i = 0; i < probs.length; i++) {
            curProbSum += probs[i];
            // NOTE: the assert is slightly larger than 1 to allow for marginal
            // accumulation due to double round-off errors
            assert curProbSum <= 1.0001
                : "sum of probabilities > 1: " + curProbSum;
            if (curProbSum >= cutoff)
                return i;
        }
        throw new IllegalStateException("Probablilities do not sum to 1");
    }

    /**
     * For the given newly chosen center, updates the {@code inverseSimilarities} array to
     * be the maximum similarity of a data point to any center.
     */
    private static void updateNearestCenter(double inverseSimilarities[], Matrix dataPoints,
                                            int newlyChosen, 
                                            SimilarityFunction simFunc) {
        
        DoubleVector chosenVec = dataPoints.getRowVector(newlyChosen);
	for (int i = 0; i < inverseSimilarities.length; i++) {
            double sim = simFunc.sim(dataPoints.getRowVector(i), chosenVec);
            sim = invertSim(sim);
            inverseSimilarities[i] = Math.min(inverseSimilarities[i], sim);
        }
    }

    private static void determineProbabilities(double inverseSimilarities[], 
                                               int[] weights, double probs[], 
                                               IntSet selected) {
	double sum = 0;

        // Iterate over all non-selected points, assigning each probability mass
        // proportional to its size and to its maximum inverse similarity to any
        // existing center.
	for (int i = 0; i < probs.length; i++) {
            // Points that are currently selected do not have any probability
            // mass so that they will not be selected again in the future
            if (selected.contains(i)) {
                probs[i] = 0;
                continue;
            }
            // The probability of a point is a product of its maximal inverse
            // similarity to any existing centroid and the relative weight.
            probs[i] = inverseSimilarities[i] * weights[i];
            sum += probs[i];
        }

        // Once probability mass has been assigned to each point, normalize the
        // distribution based on the total mass.
	for (int i = 0; i < probs.length; i++) 
            probs[i] /= sum;
    }
}