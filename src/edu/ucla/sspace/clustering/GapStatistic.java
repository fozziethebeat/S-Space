package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.ClutoClustering.Criterion;
import edu.ucla.sspace.clustering.ClutoClustering.Method;

import edu.ucla.sspace.matrix.ClutoSparseMatrixBuilder;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector ;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

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

    /**
     * A random number generator for creating reference data sets.
     */
    private static final Random random = new Random();

    /**
     * The cluto clustering method name for k-means clustering.
     */
    private static final Method METHOD = Method.KMEANS;

    private static final Criterion CRITERION = Criterion.H2;

    /**
     * {@inheritDoc}
     */
    public Assignment[] cluster(Matrix matrix, Properties props) {
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
    public Assignment[] cluster(Matrix m,
                                int maxClusters,
                                Properties props) {
        int startSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_START, DEFAULT_NUM_CLUSTERS_START));
        int numGaps = Integer.parseInt(props.getProperty(
                NUM_REFERENCE_DATA_SETS, DEFAULT_NUM_REFERENCE_DATA_SETS));
        int numIterations = maxClusters - startSize;

        verbose("Generating the reference data set");
        // Generate the reference data sets.
        ReferenceDataGenerator generator = new ReferenceDataGenerator(m);
        File[] gapFiles = new File[numGaps];
        for (int i = 0; i < numGaps; ++i)
            gapFiles[i] = generator.generateTestData();

        // Transfer the data set to a cluto matrix file.
        File matrixFile = null;
        try {
            matrixFile = File.createTempFile("cluto-input",".matrix");
            MatrixIO.writeMatrix(m, matrixFile, Format.CLUTO_SPARSE);
        } catch (IOException ioe) {
            throw new IOError(ioe); 
        }

        // Setup files to store  store what the previous gap statistic was and
        // the previous clustering assignment. 
        File previousFile = null;
        double previousGap = Double.MIN_VALUE;

        // Compute the gap statistic for each iteration.
        String result = null;
        for (int i = 0; i < numIterations; ++i) {
            int k = i + startSize;
            try {
                verbose("Clustering reference data for %d clusters\n", k);

                // Compute the score for the reference data sets with k
                // clusters.
                double referenceScore = 0;
                double[] referenceScores = new double[numGaps];
                for (int j = 0; j < numGaps; ++j) {
                    File outputFile = 
                        File.createTempFile("gap-clustering-output", ".matrix");
                    try {
                    result = ClutoWrapper.cluster(null,
                                                  gapFiles[j],
                                                  METHOD.getClutoName(),
                                                  CRITERION.getClutoName(),
                                                  outputFile,
                                                  k);
                    outputFile.delete();
                    } catch (Error e) {
                        // The ClutoWrapper throws an error when cluto crashes.
                        // If this happens, we don't want the system to crash
                        // and die, so assume that larger values of K cannot be
                        // used and use the previous clustering solutition as
                        // the best.
                        verbose("Cluto experienced an error clustering with " +
                                "%d clusters.  Returning %d as the best " +
                                "clusteirng solution", k+1, k+1);
                        break;
                    }

                    referenceScores[j] = Math.log(extractScore(result));
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
                // Compute the score for the original data set with k clusters.
                File outFile =
                    File.createTempFile("gap-clustering-output", ".matrix");

                try {
                result = ClutoWrapper.cluster(null,
                                              matrixFile,
                                              METHOD.getClutoName(),
                                              CRITERION.getClutoName(),
                                              outFile,
                                              i + startSize);
                } catch (Error e) {
                    // The ClutoWrapper throws an error when cluto crashes.
                    // If this happens, we don't want the system to crash
                    // and die, so assume that larger values of K cannot be
                    // used and use the previous clustering solutition as
                    // the best.
                    verbose("Cluto experienced an error clustering with " +
                            "%d clusters.  Returning %d as the best " +
                            "clusteirng solution", k+1, k+1);
                    break;
                }

                // Compute the difference between the two scores.  If the
                // current score is less than the previous score, then the
                // previous assignment is considered best.
                double gap = Math.log(extractScore(result));
                gap = referenceScore - gap;
                if (previousGap >= (gap - referenceStdev)) {
                    verbose("Found best clustering with %d clusters\n", (k-1));
                    break;
                }

                // Delete the contents of the previous file so that there isn't
                // an overflow of open files.
                if (previousFile != null)
                    previousFile.delete();

                // Otherwise, continue clustering with higher values of k.
                previousGap = gap;
                previousFile = outFile;
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        // Extract the cluster assignments based on the best found value of k.
        Assignment[] assignments = new Assignment[m.rows()];
        try {
            ClutoWrapper.extractAssignments(previousFile, assignments);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Delete the matrix files so that there is not an abundance of open
        // files.
        matrixFile.delete();
        for (File gapFile : gapFiles)
            gapFile.delete();

        return assignments;
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
        public File generateTestData() {
            verbose("Generating a new reference set");

            // Assume that data is sparse.
            MatrixBuilder builder = new ClutoSparseMatrixBuilder();
            for (int i = 0; i < rows; ++i) {
                int cols = minValues.length;
                //double[] values = new double[cols];

                // If the average number of values per row is significantly
                // smaller than the total number of columns then select a subset
                // to be non zero.
                //if (averageNumValuesPerRow < cols / 2) {
                SparseHashDoubleVector column =
                    new SparseHashDoubleVector(cols);
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
                builder.addColumn(column);
                /*} else {
                    // Set all values in the column.
                    for (int j = 0; j < cols; ++j) {
                        double value = random.nextDouble();
                        values[j] = value * (maxValues[j] - minValues[j]) + 
                                    minValues[j];
                    }
                }
                */
            }
            builder.finish();
            return builder.getFile();
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
