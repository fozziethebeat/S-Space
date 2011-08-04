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

import edu.ucla.sspace.clustering.ClutoClustering.Criterion;
import edu.ucla.sspace.clustering.ClutoClustering.Method;

import edu.ucla.sspace.matrix.ClutoDenseMatrixBuilder;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.Properties;
import java.util.Random;

import java.util.logging.Logger;


/**
 * A {@link Clustering} implementation that iteratively computes the k-means
 * clustering of a data set and fines the value of k that produced the most
 * significant advantage compared to other values of k.  This approach attempts
 * to find a "knee" or "bend" in the graph of objective scores for k-means with
 * different values of k.  This clustering method is an implementation of the
 * method specified in the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif">Pedersen, T and Kulkarni,
 *   A. (2006) Automatic Cluster Stopping with Criterion Functions and the Gap
 *   Statistic <i>Sixth Annual Meeting of the North American Chapter of the
 *   Association for Computational Linguistics</i>, <b>6</b>, 276-279.
 *   Available <a
 *   href="http://www.d.umn.edu/~tpederse/Pubs/naacl06-demo.pdf">here</a>
 *   </li>
 *
 * </p>
 *
 * Three measures for finding the knee are provided: PK1, PK2, and PK3
 *
 * @author Keith Stevens
 */
public class AutomaticStopClustering implements Clustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(AutomaticStopClustering.class.getName());

    /**
     * A property prefix used for properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.AutomaticStopClustering";

    /**
     * The number of clusters to start clustering at.
     */
    public static final String NUM_CLUSTERS_START = 
        PROPERTY_PREFIX + ".numClusterStart";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String NUM_CLUSTERS_END = 
        PROPERTY_PREFIX + ".numClusterEnd";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String CLUSTERING_METHOD = 
        PROPERTY_PREFIX + ".clusteringMethod";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String PK1_THRESHOLD = 
        PROPERTY_PREFIX + ".pk1Threshold";

    /**
     * The default number of clusters at which to start clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_START = "1";

    /**
     * The default number of clusters at which to stop clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_END = "10";

    /**
     * The default objective method to use.
     */
    private static final String DEFAULT_CLUSTERING_METHOD = "PK3";

    /**
     * The default threshold when using the pk1 objective method.
     */
    private static final String DEFAULT_PK1_THRESHOLD = "-.70";

    /**
     * The available stopping criteria.  For each measure, let I2(k) be the
     * objective method for evaluating the quality of the k-means clustering
     * with k clusters.
     */
    public enum Measure {

        /**
         * For each number of clusters k, the score for is defined as 
         *   W(k) = (I2(k) - mean(I2(k_i))) / std(I2(k_i))
         *
         * This method will select the smallest k such that W(k) is greater than
         * or equal to some threshold.
         */
        PK1,

        /**
         * For each number of clusters k, the score is defined as
         *   W(k) = I2(k) / I2(k-1)
         *
         * This method will select the smallest k such that W(k) is greater than
         * 1 + std(I2(k-1))
         */
        PK2,

        /**
         * For each number of clusters k, the score is defined as
         *   W(k) = 2 * I2(k) / (I2(k-1) + I2(k+1))
         *
         * This method will select the smallest k such that W(k) is greater than
         * 1 + std(I2(k-1))
         */
        PK3,
    }

    /**
     * A random number generator for creating reference data sets.
     */
    private static final Random random = new Random();

    /**
     * The CLUTO clustering method name for k-means clustering.
     */
    private static final Method METHOD = Method.KMEANS;

    private static final Criterion CRITERION = Criterion.H2;

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Iteratively computes the k-means clustering of the dataset {@code m}
     * using a specified method for determineing when to automaticaly stop
     * clustering.
     */
    public Assignment[] cluster(Matrix matrix, Properties props) {
        int endSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_END, DEFAULT_NUM_CLUSTERS_END));
        return cluster(matrix, endSize, props);
    }

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Iteratively computes the k-means clustering of the dataset {@code m}
     * using a specified method for determineing when to automaticaly stop
     * clustering.
     */
    public Assignment[] cluster(Matrix m,
                                int numClusters,
                                Properties props) {
        int startSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_START, DEFAULT_NUM_CLUSTERS_START));

        int numIterations = numClusters - startSize;

        Measure measure = Measure.valueOf(props.getProperty(
                    CLUSTERING_METHOD, DEFAULT_CLUSTERING_METHOD));

        double pk1Threshold = Double.parseDouble(props.getProperty(
                    PK1_THRESHOLD, DEFAULT_PK1_THRESHOLD));

        // Transfer the data set to a cluto matrix file.
        File matrixFile = null;
        try {
            matrixFile = File.createTempFile("cluto-input",".matrix");
            MatrixIO.writeMatrix(m, matrixFile, Format.CLUTO_DENSE);
        } catch (IOException ioe) {
            throw new IOError(ioe); 
        }

        double[] objectiveWeights = new double[numIterations];
        File[] outFiles = new File[numIterations];
        // Compute the gap statistic for each iteration.
        String result = null;
        for (int i = 0; i < numIterations; ++i) {
            LOGGER.fine("Clustering with " + (startSize + i) + " clusters");

            try {
                // Compute the score for the original data set with k clusters.
                outFiles[i] = 
                    File.createTempFile("autostop-clustering-out", ".matrix");
                result = ClutoWrapper.cluster(null,
                                              matrixFile,
                                              METHOD.getClutoName(),
                                              CRITERION.getClutoName(),
                                              outFiles[i],
                                              i + startSize);

                objectiveWeights[i] = extractScore(result);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        // Compute the best index based on the measure being used.
        int bestK = -1;
        switch (measure) {
            case PK1:
                bestK = computePk1Measure(objectiveWeights, pk1Threshold);
                break;
            case PK2:
                bestK = computePk2Measure(objectiveWeights);
                break;
            case PK3:
                bestK = computePk3Measure(objectiveWeights);
                break;
        }

        // Extract the cluster assignments based on the best found value of k.
        Assignment[] assignments = new HardAssignment[m.rows()];
        try {
            ClutoWrapper.extractAssignments(outFiles[bestK], assignments);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Delete all the data files so that there are not too many open files
        // later on.
        matrixFile.delete();
        for (File outFile : outFiles)
            outFile.delete();

        return assignments;
    }

    /**
     * Compute the smallest k that satisfies the Pk1 method.
     */
    private int computePk1Measure(double[] objectiveScores,
                                  double pk1Threshold) {
        LOGGER.fine("Computing the PK1 measure");

        // Compute the average of the objective scores.
        double average = 0;
        for (int k = 0; k < objectiveScores.length; ++k)
            average += objectiveScores[k];
        average /= objectiveScores.length;

        // Compute the standard deviation of the objective scores.
        double stdev = 0;
        for (int k = 0; k < objectiveScores.length; ++k)
            stdev += Math.pow(objectiveScores[k], 2);
        stdev /= objectiveScores.length;
        stdev = Math.sqrt(stdev);

        // Find the smallest k such that the pk1 score surpasses the threshold.
        for (int k = 0; k < objectiveScores.length; ++k) {
            objectiveScores[k] = (objectiveScores[k] - average) / stdev;
            if (objectiveScores[k] > pk1Threshold)
                return k;
        }

        return 0;
    }

    /**
     * Compute the smallest k that satisfies the Pk3 method.
     */
    private int computePk2Measure(double[] objectiveScores) {
        LOGGER.fine("Computing the PK2 measure");

        // Compute each Pk2 score and the average score.
        double average = 0;
        for (int k = objectiveScores.length - 1; k > 0; --k) {
            objectiveScores[k] /= objectiveScores[k-1];
            average += objectiveScores[k];
        }
        average /= (objectiveScores.length - 1);

        // Compute the standard deviation of the PK2 scores.
        double stdev = 0;
        for (int k = 1; k < objectiveScores.length; ++k)
            stdev += Math.pow(objectiveScores[k] - average, 2);
        stdev /= (objectiveScores.length - 2);
        stdev = Math.sqrt(stdev);

        // Find the point where the score is the smallest value greater than 1 +
        // stdev of the PK1 scores.
        double referencePoint = 1 + stdev;
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int k = 1; k < objectiveScores.length; ++k) {
            if (objectiveScores[k] < bestScore &&
                objectiveScores[k] >= referencePoint) {
                bestIndex = k;
                bestScore = objectiveScores[k];
            }
        }

        return bestIndex;
    }

    /**
     * Compute the smallest k that satisfies the Pk3 method.
     */
    private int computePk3Measure(double[] objectiveScores) {
        LOGGER.fine("Computing the PK3 measure");

        // Compute each Pk3 score and the average score.
        double average = 0;
        double[] weightedScores = new double[objectiveScores.length - 2];
        for (int k = 1; k < objectiveScores.length - 1 ; ++k) {
            weightedScores[k-1] = 2 * objectiveScores[k] / 
                (objectiveScores[k-1] + objectiveScores[k+1]);
            average += weightedScores[k-1];
        }
        average /= (objectiveScores.length - 2);

        // Compute the standard deviation of PK3 scores.
        double stdev = 0;
        for (int k = 0; k < weightedScores.length; ++k)
            stdev += Math.pow(weightedScores[k] - average, 2);
        stdev /= (objectiveScores.length - 2);
        stdev = Math.sqrt(stdev);

        // Find the point where the score is the smallest value greater than 1 +
        // stdev of the PK3 scores.
        double referencePoint = 1 + stdev;
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int k = 0; k < weightedScores.length; ++k) {
            if (weightedScores[k] < bestScore &&
                weightedScores[k] >= referencePoint) {
                bestIndex = k;
                bestScore = weightedScores[k];
            }
        }

        return bestIndex + 1;
    }

    /**
     * Extracts the score of the objective function for a given set of
     * clustering assignments.  This requires scraping the output from Cluto to
     * find the line specifiying the score.
     */
    private double extractScore(String clutoOutput) throws IOException {
        double score = 0;
        BufferedReader reader =
            new BufferedReader(new StringReader(clutoOutput));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("[I2=")) {
                String[] split = line.split("=");
                int endOfIndex = split[1].indexOf("]");
                return Double.parseDouble(split[1].substring(0, endOfIndex));
            }
        }
        return 0;
    }
}
