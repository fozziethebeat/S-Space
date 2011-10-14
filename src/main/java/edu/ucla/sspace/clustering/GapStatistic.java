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
import edu.ucla.sspace.clustering.criterion.I2Function;
import edu.ucla.sspace.clustering.criterion.H2Function;

import edu.ucla.sspace.matrix.ClutoSparseMatrixBuilder;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.RowMagnitudeTransform;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.TfIdfDocStripedTransform;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.util.WorkerThread;
import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.logging.Logger;


/**
 * A {@link Clustering} implementation that iteratively computes the
 * k-means clustering of a data set and compares it to a random sample of
 * reference data points.  This will recompute k-means with incresing values of
 * k until the difference between the original data set and the reference data
 * sets begins to decline.  Clustering will stop at the first k value where this
 * difference is less than the previous difference.  This clustering method is
 * an implementation of the method specified in the following paper:
 *
 *   <li style="font-family:Garamond, Georgia serif">R. Tibshirani, G. Walther,
 *   and T. Hastie. (2001). Estimating the number of clusters in a dataset via
 *   the Gap statistic. <i>Journal of the Royal Statistics Society (Series
 *   B)</i>, 411–423. Available <a
 *   href="http://www-stat.stanford.edu/~tibs/ftp/gap.ps">here</a>
 *   </li>
 *
 * @author Keith Stevens
 */
public class GapStatistic implements Clustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(GapStatistic.class.getName());

    /**
     * A property prefix used for properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.GapStatistic";

    /**
     * The number of clusters to start clustering at.
     */
    public static final String NUM_CLUSTERS_START = 
        PROPERTY_PREFIX + ".numClusterStart";

    /**
     * The number of reference data sets to use.
     */
    public static final String NUM_REFERENCE_DATA_SETS = 
        PROPERTY_PREFIX + ".numReferenceDataSets";

    public static final String METHOD_PROPERTY = 
        PROPERTY_PREFIX + ".method";

    /**
     * The default number of clusters at which to start clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_START = "1";

    /**
     * The default number of clusters at which to stop clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_END = "10";

    /**
     * The default number of reference data sets to use.
     */
    private static final String DEFAULT_NUM_REFERENCE_DATA_SETS = "5";

    private static final String DEFAULT_METHOD = 
        "edu.ucla.sspace.clustering.criterion.H2Function";


    /**
     * A random number generator for creating reference data sets.
     */
    private static final Random random = new Random();

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        return cluster(matrix, Integer.MAX_VALUE, props);
    }

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Iteratively computes the k-means clustering of the dataset {@code m}
     * using the the Gap Statistic .
     */
    public Assignments cluster(Matrix m,
                               int maxClusters,
                               Properties props) {
        int startSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_START, DEFAULT_NUM_CLUSTERS_START));
        int numGaps = Integer.parseInt(props.getProperty(
                NUM_REFERENCE_DATA_SETS, DEFAULT_NUM_REFERENCE_DATA_SETS));
        int numIterations = maxClusters - startSize;
        String criterion = props.getProperty(METHOD_PROPERTY, DEFAULT_METHOD);

        verbose("Transforming the original data set");
        Transform tfidf = new TfIdfDocStripedTransform();
        Transform rowMag = new RowMagnitudeTransform();
        m = rowMag.transform(tfidf.transform(m));

        verbose("Generating the reference data set");
        // Generate the reference data sets.
        ReferenceDataGenerator generator = new ReferenceDataGenerator(m);
        Matrix[] gapMatrices = new Matrix[numGaps];
        for (int i = 0; i < numGaps; ++i)
            gapMatrices[i] = rowMag.transform(tfidf.transform(
                    generator.generateTestData()));

        double[] gapResults = new double[numIterations];
        double[] gapStds = new double[numIterations];
        Assignments[] gapAssignments = new Assignments[numIterations];

        Assignments bestAssignments = null;
        double bestGap = Double.NEGATIVE_INFINITY;
        int bestK = 0;
        // Compute the gap statistic for each iteration.
        for (int i = 0; i < numIterations; ++i) {
            clusterIteration(i, startSize, criterion, m, gapMatrices,
                             gapResults, gapStds, gapAssignments);
            if (bestGap >= (gapResults[i] - gapStds[i])) {
                break;
            }

            // Otherwise, continue clustering with higher values of k.
            bestGap = gapResults[i];
            bestAssignments = gapAssignments[i];
            bestK = i + startSize;
        }

        return bestAssignments;
    }

    private void clusterIteration(int i, int startSize, 
                                  String methodName,
                                  Matrix matrix, Matrix[] gapMatrices, 
                                  double[] gapResults, double[] gapStds,
                                  Assignments[] gapAssignments) {
        int k = i+startSize;
        int numGaps = gapMatrices.length;
        CriterionFunction function = 
            ReflectionUtil.getObjectInstance(methodName);
        verbose("Clustering reference data for %d clusters\n", k);

        // Compute the score for the reference data sets with k
        // clusters.
        double referenceScore = 0;
        double[] referenceScores = new double[numGaps];
        for (int j = 0; j < gapMatrices.length; ++j) {
            verbose("Clustering reference data %d \n", j);
            Assignments result = DirectClustering.cluster(
                    gapMatrices[j], k, 1, function);
            referenceScores[j] = Math.log(function.score());
            referenceScore += referenceScores[j];
        }
        referenceScore /= numGaps;

        // Compute the standard deviation for the reference scores.
        double referenceStdev = 0;
        for (double score : referenceScores)
            referenceStdev += Math.pow(score - referenceScore, 2);
        referenceStdev /= numGaps;
        referenceStdev = Math.sqrt(referenceStdev);

        verbose("Clustering original data for %d clusters\n", k);
        // Compute the score for the original data set with k
        // clusters.
        Assignments result = DirectClustering.cluster(
                matrix, k, 1, function);

        // Compute the difference between the two scores.  If the
        // current score is less than the previous score, then the
        // previous assignment is considered best.
        double gap = Math.log(function.score());
        verbose("Completed iteration with referenceScore: %f, gap:%f\n",
                referenceScore, gap);
        gap = referenceScore - gap;

        System.out.printf("k: %d gap: %f std: %f\n", i, gap, referenceStdev);
        gapResults[i] = gap;
        gapStds[i] = referenceStdev;
        gapAssignments[i] = result;
    }

    /**
     * A simple data set generator that generates new vectors based on the range
     * of values each feature in the vector can take one.
     */
    private class ReferenceDataGenerator {

        /**
         * The minimum value for each feature.
         */
        private final double[] minValues;

        /**
         * The maximum value for each feature.
         */
        private final double[] maxValues;

        /**
         * The average number of non zero values in a single row.
         */
        private final double averageNumValuesPerRow;

        /**
         * The standard deviation of the number of non zero values in a single
         * row.
         */
        private final double stdevNumValuesPerRow;

        /**
         * The number of rows to generate in a test data set.
         */
        private final int rows;

        private Set<Integer> nonZeroFeatures;

        /**
         * Creates a new {@code ReferenceDataGenerator} based on the given
         * matrix {@code m}.
         */
        public ReferenceDataGenerator(Matrix m) {
            // Initialize the bounds.
            rows = m.rows();
            minValues = new double[m.columns()];
            maxValues = new double[m.columns()];
            nonZeroFeatures = new HashSet<Integer>();
            int[] numNonZeros = new int[m.rows()];
            double averageNumNonZeros = 0;

            if (m instanceof SparseMatrix) {
                SparseMatrix sm = (SparseMatrix) m;
                for (int r = 0; r < m.rows(); ++r) {
                    SparseDoubleVector v = sm.getRowVector(r);
                    int[] nonZeros = v.getNonZeroIndices();
                    numNonZeros[r] += nonZeros.length;
                    averageNumNonZeros += nonZeros.length;
                    for (int column : nonZeros) {
                        nonZeroFeatures.add(column);
                        double value = v.get(column);
                        // Get the max and minimum value for the row.
                        if (value < minValues[column])
                            minValues[column] = value;
                        if (value > maxValues[column])
                            maxValues[column] = value;
                    }
                }
            } else {
                for (int r = 0; r < m.rows(); ++r) {
                    for (int c = 0; c < m.columns(); ++c) {
                        double value = m.get(r, c);
                        // Get the max and minimum value for the row.
                        if (value < minValues[c])
                            minValues[c] = value;
                        if (value > maxValues[c])
                            maxValues[c] = value;

                        // Calculate the number of non zeros per row.
                        if (value != 0d) {
                            numNonZeros[r]++;
                            averageNumNonZeros++;
                            nonZeroFeatures.add(c);
                        }
                    }
                }
            }

            // Finalize the average number of non zeros per row.
            averageNumValuesPerRow = averageNumNonZeros / m.rows();

            // Compute the standard deviation of the number of non zeros per
            // row.
            double stdev = 0;
            for (int nonZeroCount : numNonZeros)
                stdev += Math.pow(averageNumValuesPerRow- nonZeroCount, 2);

            // Finalize the standar deviation.
            stdevNumValuesPerRow = Math.sqrt(stdev / m.rows());
            
        }

        /**
         * Creates a test file in the {@code CLUTO_DENSE} format containing
         * reference data points from a data distribution similar to the
         * original.
         */
        public Matrix generateTestData() {
            verbose("Generating a new reference set");

            List<SparseDoubleVector> vectors =
                new ArrayList<SparseDoubleVector>();
            // Assume that data is sparse.
            MatrixBuilder builder = new ClutoSparseMatrixBuilder();
            for (int i = 0; i < rows; ++i) {
                int cols = minValues.length;

                // If the average number of values per row is significantly
                // smaller than the total number of columns then select a subset
                // to be non zero.
                SparseDoubleVector column = new CompactSparseVector(cols);
                int numNonZeros =
                    (int) (random.nextGaussian() * stdevNumValuesPerRow +
                           averageNumValuesPerRow);
                if (numNonZeros == 0)
                    numNonZeros++;

                for (int j = 0; j < numNonZeros; ++j) {
                    // Get the next index to set.
                    int col = getNonZeroColumn();
                    double value = random.nextDouble() *
                            (maxValues[col] - minValues[col]) + minValues[col];
                    column.set(col, value);
                }
                vectors.add(column);
            }
            //builder.finish();

            return Matrices.asSparseMatrix(vectors);
            //return builder.getMatrixFile();
        }

        /**
         * Returns a randomly chosen, with replacement, column that has a non
         * zero feature in the original data set.
         */
        private int getNonZeroColumn() {
            while (true) {
                int col = random.nextInt(minValues.length);
                if (nonZeroFeatures.contains(col))
                    return col;
            }
        }

    }

    protected void verbose(String msg) {
        LOGGER.fine(msg);
    }

    protected void verbose(String format, Object... args) {
        LOGGER.fine(String.format(format, args));        
    }
}
