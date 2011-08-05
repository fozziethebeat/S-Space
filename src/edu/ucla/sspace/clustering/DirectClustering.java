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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.criterion.CriterionFunction;
import edu.ucla.sspace.clustering.criterion.I1Function;

import edu.ucla.sspace.clustering.seeding.KMeansSeed;
import edu.ucla.sspace.clustering.seeding.RandomSeed;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.*;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.*;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Properties;


/**
 * An implementation of the Direct K-Means clustering available in the 
 * <a href="http://glaros.dtc.umn.edu/gkhome/views/cluto/">CLUTO Clustering
 * Software</a>.  This implementation of K-Means varies from the standard
 * implementation in a variety of ways:
 *
 * <li>
 *   <ol> After creating the initial set of clusters, each data point is
 *   assigned to a cluster if such a move improvies the overall clustering
 *   score.  This implementation is an iterative method, in that it makes the
 *   cluster change immediately and recomputes the centroid after every change.
 *   </ol>
 *   <ol> The clustering objective can be changed by using a different {@link
 *   edu.ucla.sspace.clustering.criterion.CriterionFunction CriterionFunction}.
 *   The {@link edu.ucla.sspace.clustering.criteria.I1Function I1Function}
 *   implements the standard K-Means criterion function.  Others try to optimize
 *   a variety of
 *   different objectives.</ol>
 * </li>
 *
 * @author Keith Stevens
 */
public class DirectClustering implements Clustering {

    /**
     * A property prefix for specifiying options when using {@link
     * DirectClustering}.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.DirectClustering";

    /**
     * The property to set the name of a {@link CriterionFunction} to use when
     * clustering the data.
     */
    public static final String CRITERIA_PROPERTY =
        PROPERTY_PREFIX + ".criteria";

    /**
     * The property to set the number of times a single run of {@link
     * DirectClustering} will be run.  If this is more than 1, the best scoring
     * run will be returned.
     */
    public static final String REPEAT_PROPERTY=
        PROPERTY_PREFIX + ".repeat";

    /**
     * A property for setting the {@link KMeansSeed} algorithm to use.
     */
    public static final String SEED_PROPERTY =
        PROPERTY_PREFIX + ".seed";

    /**
     * The default seed algorithm used.
     */
    private static final String DEFAULT_SEED =
        "edu.ucla.sspace.clustering.seeding.RandomSeed";

    /**
     * The default {@link CriterionFunction} to be used while clustering.
     */
    private static final String DEFAULT_CRITERION = 
        "edu.ucla.sspace.clustering.criterion.I1Function";

    /**
     * The default number of repetitions.
     */
    private static final String DEFAULT_REPEATS = "10";

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    public Assignments cluster(Matrix matrix, Properties properties) {
        throw new UnsupportedOperationException(
                "DirectClustering requires the number of clusters to be set.");
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix,
                               int numClusters,
                               Properties properties) {
        // Get the number of repitetions to use.
        int numRepetitions = Integer.parseInt(properties.getProperty(
                    REPEAT_PROPERTY, DEFAULT_REPEATS));

        // Get an instance of the seed generator.
        KMeansSeed seedType = ReflectionUtil.getObjectInstance(
                properties.getProperty(SEED_PROPERTY, DEFAULT_SEED));

        // Create the criterion function.
        CriterionFunction criterion = ReflectionUtil.getObjectInstance(
                properties.getProperty(CRITERIA_PROPERTY, DEFAULT_CRITERION));

        return cluster(matrix, numClusters, numRepetitions,
                       seedType, criterion);
    }

    /**
     * Clusters {@link matrix} using the {@link RandomSeed} seeding algorithm
     * and the default kmeans {@link CriterionFunction}.  The best scoring
     * solution out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions) {
        return cluster(matrix, numClusters, numRepetitions,
                       new RandomSeed(), new I1Function());
    }

    /**
     * Clusters {@link matrix} using the {@link RandomSeed}
     * seeding algorithm and the specified {@link CriterionFunction}. The best
     * scoring solution out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions,
                                      CriterionFunction criterion) {
        return cluster(matrix, numClusters, numRepetitions,
                       new RandomSeed(), criterion);
    }

    /**
     * Clusters {@link matrix} using the specified {@link SeedAlgorithm}
     * and the specified {@link CriterionFunction}. The best scoring solution
     * out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions,
                                      KMeansSeed seedType,
                                      CriterionFunction criterion) {
        int[] bestAssignment = null;
        double bestScore = (criterion.maximize()) ? 0 : Double.MAX_VALUE;
        for (int i = 0; i < numRepetitions; ++i) {
            clusterInt(matrix, numClusters, seedType, criterion);
            if (criterion.maximize()) {
                if (criterion.score() > bestScore) {
                    bestScore = criterion.score();
                    bestAssignment = criterion.assignments();
                }
            } else {
                if (criterion.score() < bestScore) {
                    bestScore = criterion.score();
                    bestAssignment = criterion.assignments();
                }
            }
        }

        // Convert the array of assignments to an Assignments object.
        Assignment[] assignments = new Assignment[matrix.rows()];
        for (int i = 0; i < bestAssignment.length; ++i)
            assignments[i] = new HardAssignment(bestAssignment[i]);

        return new Assignments(numClusters, assignments);

    }

    /**
     * performs one run of Direct Clustering over the data set.
     */
    private static void clusterInt(Matrix matrix,
                                   int numClusters,
                                   KMeansSeed seedType,
                                   CriterionFunction criterion) {
        DoubleVector[] centers = seedType.chooseSeeds(numClusters, matrix);

        // Compute the initial set of assignments for each data point based on
        // the initial assignments.
        int[] initialAssignments = new int[matrix.rows()];

        // If there is to be only one cluster, then everything will be auto
        // assigned to the first cluster.  This is just a special case that only
        // comes up when comparing other solutions to the non-solution.
        if (numClusters != 1) {
            int nc = 0;
            for (int i = 0; i < matrix.rows(); ++i) {

                DoubleVector vector = matrix.getRowVector(i);
                double bestSimilarity = 0;
                for (int c = 0; c < numClusters; ++c) {
                    double similarity = Similarity.cosineSimilarity(
                            centers[c], vector);
                    nc++;
                    if (similarity >= bestSimilarity) {
                        bestSimilarity = similarity;
                        initialAssignments[i] = c;
                    }
                }
            }
        }

        // Setup the criterion function with it's meta data.
        criterion.setup(matrix, initialAssignments, numClusters);

        // Iteratively swap each data point to a better cluster if such an
        // assignment exists.
        List<Integer> indices = new ArrayList<Integer>(matrix.rows());
        for (int i = 0; i < matrix.rows(); ++i)
            indices.add(i);

        // Iterate through each data point in random order.  For each data
        // point, try to assign it to a new cluster.  If no data point is moved
        // in an iteration, end the iterations.
        boolean changed = true;
        while (changed) {
            changed = false;
            Collections.shuffle(indices);
            for (int index : indices)
                changed |= criterion.update(index);
        }
    }

    public String toString() {
        return "DirectClustering";
    }
}
