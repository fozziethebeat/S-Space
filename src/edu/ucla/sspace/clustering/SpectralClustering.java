/*
 * Copyright 2009 Keith Stevens 
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

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;


/**
 * Implementation of Spectral Clustering using divide and merge methodology.
 * The implementation is based on two papers:
 *
 * <ul>
 *   <li style="font-family:Garamond, Georgia, serif">Cheng, D., Kannan, R.,
 *     Vempala, S., Wang, G.  (2006).  A Divide-and-Merge Methodology for
 *     Clustering. <i>ACM Transactions on Database Systsms</i>, <b>31</b>,
 *     1499-1525.  Available <a
 *     href=http://www-math.mit.edu/~vempala/papers/eigencluster.pdf">here</a>
 *   </li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">Kannan, R., Vempala, S.,
 *     Vetta, A.  (2000).  On clustering: Good, bad, and spectral.  
 *     <i>FOCS '00: Proceedings of the 41st Annual Symposium on Foundations of
 *   Computer Science</i> Available <a
 *     href="http://www-math.mit.edu/~vempala/papers/specfocs.ps">here</a>
 *   </li>
 * </ul>
 *
 * This class utilizes a {@link EigenCut} instance to compute a spectral
 * partition of a dataset.  Given a single data set, it will recursively
 * partition that data set using a {@link EigenCut} instance until one of two
 * limits: all data points are in a unique cluster, or the maximum number of
 * partitions have been made, as set by the number of clusters.  Once all
 * paritions are made, neighboring paritions may be merged according to the
 * objective scores returned by the {@link EigenCut} instance.  If {@link
 * cluster(Matrix, int)} is used, the requested number of clusters,
 * or fewer, will be returned, otherwise the algorithm will decide on the
 * correct number of clusters.
 *
 * @see EigenCut
 * @see BaseSpectralCut
 *
 * @author Keith Stevens
 */
public class SpectralClustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(SpectralClustering.class.getName());

    /**
     * The amount of weight given to inter-cluster similarity when using the
     * relaxed correlation objective function.
     */
    private final double alpha;

    /**
     * {@code 1 - alpha}, the weight given to intra-cluster dis-similarity.
     */
    private final double beta;

    /**
     * A generator function that creates new fresh instances of a {@link
     * EigenCut} implementation on demand.  This is used to reduce the
     * reflection overhead, as many {@link EigenCut} instances may be needed.
     */
    private final Generator<EigenCut> cutterGenerator;

    /**
     * Creates a new {@link SpectralClustering} instance.
     *
     * @param alpha The weight given to inter cluster similarity for the relaxed
     *        correlation objective function.  {@code beta} will be set to
     *        {@link 1 - alpha}.
     * @param cutterGenerator A {@link Generator} of {@link EigenCut} instances.
     */
    public SpectralClustering(double alpha,
                              Generator<EigenCut> cutterGenerator) {
        this.alpha = alpha;
        this.beta = 1 - alpha;
        this.cutterGenerator = cutterGenerator;
    }

    /**
     * Returns the Cluster {@link Assignments} of each data point in {@link
     * matrix}.  This method will determine a "good" number of clusters
     * algorithmiclly.  This method also uses the relaxed correlation objective
     * function, as the k-means objective function would always keep each data
     * point in it's own cluster.
     */
    public Assignments cluster(Matrix matrix) {
        ClusterResult r = fullCluster(scaleMatrix(matrix), 0);
        verbose("Created " + r.numClusters + " clusters");

        Assignment[] assignments = new HardAssignment[r.assignments.length];
        for (int i = 0; i < r.assignments.length; ++i)
            assignments[i] = new HardAssignment(r.assignments[i]);

        return new Assignments(r.numClusters, assignments, matrix);
    }

    /**
     * Returns the Cluster {@ link Assignments} for each data point in {@link
     * matrix}.  This method will return at most {@code maxClusters}.  If {@code
     * useKMeans} is true, the K-means objective function is used to merge
     * clusters, otherwise the relaxed correlation function is used.
     */
    public Assignments cluster(Matrix matrix,
                                int maxClusters,
                                boolean useKMeans) {
        // Cluster the matrix recursively.
        LimitedResult[] results = limitedCluster(
                scaleMatrix(matrix), maxClusters, useKMeans);

        // If the matrix could not be cut, there will be only one result
        // returned, so check for that use case.
        LimitedResult r = (results.length == 1)
            ? results[0]
            : results[maxClusters-1];

        // Sometimes the desired number of clusters won't be computed, so find
        // the next largest clustering result and return that instead.
        for (int i = maxClusters -2; r == null && i >= 0; i--)
            r = results[i];

        // Convert the LimitedResult object into an Assignments object.
        verbose("Created " + r.numClusters + " clusters");
        Assignment[] assignments = new HardAssignment[r.assignments.length];
        for (int i = 0; i < r.assignments.length; ++i)
            assignments[i] = new HardAssignment(r.assignments[i]);

        return new Assignments(r.numClusters, assignments, matrix);
    }

    /**
     * Returns a scaled {@link Matrix}, where each row is the unit
     * magnitude version of the corresponding row vector in {@link matrix}.
     * This is required so that dot product computations over a large number of
     * data points can be distributed, wherease the cosine similarity cannot be.
     */
    private Matrix scaleMatrix(Matrix matrix) {
        // Scale every data point such that it has a dot product of 1 with
        // itself.  This will make further calculations easier since the dot
        // product distrubutes when the cosine similarity does not.
        if (matrix instanceof SparseMatrix) {
            List<SparseDoubleVector> scaledVectors =
                new ArrayList<SparseDoubleVector>(matrix.rows());
            SparseMatrix sm = (SparseMatrix) matrix;
            for (int r = 0; r < matrix.rows(); ++r) {
                SparseDoubleVector v = sm.getRowVector(r);
                scaledVectors.add(new ScaledSparseDoubleVector(
                            v, 1/v.magnitude()));
            }
            return Matrices.asSparseMatrix(scaledVectors);
        } else {
            List<DoubleVector> scaledVectors = 
                new ArrayList<DoubleVector>(matrix.rows());
            for (int r = 0; r < matrix.rows(); ++r) {
                DoubleVector v = matrix.getRowVector(r);
                scaledVectors.add(new ScaledDoubleVector(v, 1/v.magnitude()));
            }
            return Matrices.asMatrix(scaledVectors);
        }
    }

    /**
     * Returns a {@link ClusterResult} when {@link matrix} is spectrally
     * clustered at a given {@link depth}.  This will recursively call itself
     * until the number of rows in {@code matrix} is less than or equal to 1.
     */
    private ClusterResult fullCluster(Matrix matrix,
                                      int depth) {
        verbose("Clustering at depth " + depth);

        // If the matrix has only one element or the depth is equal to the
        // maximum number of desired clusters then all items are in a single
        // cluster.
        if (matrix.rows() <= 1) 
            return new ClusterResult(new int[matrix.rows()], 1);

        // Get a fresh new eigen cutter and compute the specral cut of the
        // matrix.
        EigenCut eigenCutter = cutterGenerator.generate();
        eigenCutter.computeCut(matrix);

        Matrix leftMatrix = eigenCutter.getLeftCut();
        Matrix rightMatrix = eigenCutter.getRightCut();

        verbose(String.format("Splitting into two matricies %d-%d",
                              leftMatrix.rows(), rightMatrix.rows()));

        // If the compute decided that the matrix should not be split, short
        // circuit any attempts to further cut the matrix.
        if (leftMatrix.rows() == matrix.rows() ||
            rightMatrix.rows() == matrix.rows())
            return new ClusterResult(new int[matrix.rows()], 1);

        // Do clustering on the left and right branches.
        ClusterResult leftResult =
            fullCluster(leftMatrix, depth+1);
        ClusterResult rightResult =
            fullCluster(rightMatrix, depth+1);

        verbose("Merging at depth " + depth);

        // Compute the relaxed correlation objective function over the split
        // partitions found so far.
        double splitObjective = eigenCutter.getSplitObjective(
                alpha, beta,
                leftResult.numClusters, leftResult.assignments,
                rightResult.numClusters, rightResult.assignments);

        // Compute the objective when we merge the two branches together.
        double mergedObjective = eigenCutter.getMergedObjective(alpha, beta);

        // If the merged objective value is less than the split version, combine
        // all clusters into one.
        int[] assignments = new int[matrix.rows()];
        int numClusters = 1;
        if (mergedObjective < splitObjective) {
            verbose("Selecting to combine sub trees at depth " + depth);
            Arrays.fill(assignments, 0);
        } else  {
            verbose("Selecting to maintain sub trees at depth " + depth);

            // Copy over the left assignments and the right assignments, where
            // the cluster id's of the right assignments are incremented to
            // avoid duplicate cluster ids.
            numClusters = leftResult.numClusters + rightResult.numClusters;

            int[] leftReordering = eigenCutter.getLeftReordering();
            int[] rightReordering = eigenCutter.getRightReordering();

            for (int index = 0; index < leftReordering.length; ++index)
                assignments[leftReordering[index]] = 
                    leftResult.assignments[index];
            for (int index = 0; index < rightReordering.length; ++index)
                assignments[rightReordering[index]] =
                    rightResult.assignments[index] + leftResult.numClusters;
        }
        return new ClusterResult(assignments, numClusters);
    }

    /**
     * Returns {@code maxClusters} {@link LimitedResult}s using spectral
     * clustering with the relaxed correlation objective function.
     */
    private LimitedResult[] limitedCluster(Matrix matrix,
                                           int maxClusters) {
        return limitedCluster(matrix, maxClusters, false);
    }

    /**
     * Returns {@code maxClusters} {@link LimitedResult}s using spectral
     * clustering.  If {@code useKMeans}, the k-means objective function is used
     * for merging, other wise the relaxed correlation objective function is
     * used.
     */
    private LimitedResult[] limitedCluster(Matrix matrix,
                                           int maxClusters,
                                           boolean useKMeans) {
        verbose("Clustering for " + maxClusters + " clusters.");

        // Get a fresh new EigenCut first so that we can compute the RhoSum of
        // the matrix, which is useful when computing the objective function for
        // the merged result.
        EigenCut eigenCutter = cutterGenerator.generate();

        // If the matrix has only one element or the depth is equal to the
        // maximum number of desired clusters then all items are in a single
        // cluster.
        if (matrix.rows() <= 1 || maxClusters <= 1) {
            eigenCutter.computeRhoSum(matrix);
            LimitedResult result;
            if (useKMeans)
                result = new KMeansLimitedResult(new int[matrix.rows()], 1,
                        eigenCutter.getKMeansObjective());
            else
                // When computing the inter-cluster similarity for the full
                // matrix, this is simply rhowSum with out any self-similarity
                // scores.
                result = new SpectralLimitedResult(new int[matrix.rows()], 1, 
                        eigenCutter.getMergedObjective(alpha, beta),
                        eigenCutter.rhoSum() - matrix.rows() / 2.0,
                        (matrix.rows() * (matrix.rows()-1)) / 2);
            return new LimitedResult[] {result};
        }

        // Get a fresh new eigen cutter and compute the specral cut of the
        // matrix.
        eigenCutter.computeCut(matrix);

        Matrix leftMatrix = eigenCutter.getLeftCut();
        Matrix rightMatrix = eigenCutter.getRightCut();

        // If the compute decided that the matrix should not be split, short
        // circuit any attempts to further cut the matrix.
        if (leftMatrix.rows() == matrix.rows() ||
            rightMatrix.rows() == matrix.rows()) {
            eigenCutter.computeRhoSum(matrix);
            LimitedResult result;
            if (useKMeans)
                result = new KMeansLimitedResult(new int[matrix.rows()], 1,
                        eigenCutter.getKMeansObjective());
            else
                // When computing the inter-cluster similarity for the full
                // matrix, this is simply rhowSum with out any self-similarity
                // scores.
                result = new SpectralLimitedResult(new int[matrix.rows()], 1, 
                        eigenCutter.getMergedObjective(alpha, beta),
                        eigenCutter.rhoSum() - matrix.rows() / 2.0,
                        (matrix.rows() * (matrix.rows()-1)) / 2);
            return new LimitedResult[] {result};
        }

        verbose(String.format("Splitting into two matricies %d-%d",
                              leftMatrix.rows(), rightMatrix.rows()));

        // Do clustering on the left and right branches.
        LimitedResult[] leftResults =
            limitedCluster(leftMatrix, maxClusters-1, useKMeans);
        LimitedResult[] rightResults =
            limitedCluster(rightMatrix, maxClusters-1, useKMeans);

        verbose("Merging at for: " + maxClusters + " clusters");

        // Get the re-ordering mapping for each partition.   Using this
        // re-ordering, compute objective function for the entire data matrix.
        int[] leftReordering = eigenCutter.getLeftReordering();
        int[] rightReordering = eigenCutter.getRightReordering();

        LimitedResult[] results = new LimitedResult[maxClusters];
        if (useKMeans)
            results[0] = new KMeansLimitedResult(new int[matrix.rows()], 1,
                    eigenCutter.getKMeansObjective());
        else
            // When computing the inter-cluster similarity for the full
            // matrix, this is simply rhowSum with out any self-similarity
            // scores.
            results[0] = new SpectralLimitedResult(new int[matrix.rows()], 1, 
                    eigenCutter.getMergedObjective(alpha, beta),
                    eigenCutter.rhoSum() - matrix.rows() / 2.0,
                    (matrix.rows() * (matrix.rows()-1)) / 2);

        // Each LimitedResult is ordered based on the number of clusters found.
        // Iterate through each clustering result computed for each partition
        // and find combined set of cluter assignments that satisfies the
        // requested number of clusters.
        for (int i = 0; i < leftResults.length; ++i) {
            LimitedResult leftResult = leftResults[i];

            // If there are no assignments found for this number of clusters,
            // skip it.
            if (leftResult == null)
                continue;

            for (int j = 0; j < rightResults.length; ++j) {
                LimitedResult rightResult = rightResults[j];

                // If there are no assignments found for this number of
                // clusters, skip it.
                if (rightResult == null)
                    continue;

                // Compute the number of clusters found when using both the left
                // and right parititons.
                int numClusters =
                    leftResult.numClusters + rightResult.numClusters - 1;
                if (numClusters >= results.length)
                    continue;

                // Compute the combined objective function when using both the
                // left and right partition results.
                LimitedResult newResult;
                if (useKMeans)
                    newResult = leftResult.combine(leftResult, rightResult,
                            leftReordering, rightReordering, 0);
                else
                    newResult = leftResult.combine(
                            leftResult, rightResult,
                            leftReordering, rightReordering,
                            eigenCutter.rhoSum());

                // If no assignments have been made so far for this number of
                // clusters, or the found score is better than the old score,
                // store the combined LimitedResult as the best result for the
                // number of clusters.
                if (results[numClusters] == null ||
                    results[numClusters].compareTo(newResult) < 0)
                    results[numClusters] = newResult;
            }
        }

        return results;
    }



    /**
     * Logs data to info.
     */
    private void verbose(String out) {
        LOGGER.info(out);
    }

    /**
     * A internal helper class that can combine cluster results from two
     * separate partions using a particular objective function.
     */
    private abstract class LimitedResult {
        
        /**
         * The data assignments for a single {@link LimitedResult}.
         */
        public int[] assignments;

        /**
         * The number of clusters.
         */
        public int numClusters;

        /**
         * Creates a new {@link LimitedResult} using the given assignments and
         * number of clusters.
         */
        public LimitedResult(int[] assignments, int numClusters) {
            this.assignments = assignments;
            this.numClusters = numClusters;
        }

        /**
         * Combines the assignments made between two {@link LimitedResult}s.
         * The cluster ids in {@code res2} will be increased based on the number
         * of clusters in {@code res1}.
         */
        int[] combineAssignments(LimitedResult res1, LimitedResult res2,
                                 int[] ordering1, int[] ordering2) {
            int[] newAssignments = new int[
                res1.assignments.length + res2.assignments.length];

            for (int k = 0; k < ordering1.length; ++k)
                newAssignments[ordering1[k]] = res1.assignments[k];
            for (int k = 0; k < ordering2.length; ++k)
                newAssignments[ordering2[k]] =
                    res2.assignments[k] + res1.numClusters;
            return newAssignments;
        }

        /**
         * Returns the objective function score for this result.
         */
        abstract double score();

        /**
         * Returns greater than 0 when {@code this} {@link LimitedResult} should
         * be selected over {@code other}.
         */
        abstract double compareTo(LimitedResult other);

        /**
         * Returns a combined {@link LimitedResult} based on two existing {@link
         * LimitedResult}s.
         */
        abstract LimitedResult combine(LimitedResult res1, LimitedResult res2,
                                       int[] ordering1, int[] ordering2,
                                       double extra);
    }

    /**
     * Computes the k-means objective when combining two {@link LimitedResult}s.
     */
    private class KMeansLimitedResult extends LimitedResult {
        
        /**
         * The k-means objective score for this result.
         */
        public double score;

        /**
         * Creates a new {@link KMeansLimitedResult} with a given objective
         * score.
         */
        public KMeansLimitedResult(int[] assignments,
                                  int numClusters,
                                  double score) {
            super(assignments, numClusters);
            this.score = score;
        }

        /**
         * Returns a new {@link KMeansLimitedResult} with an update scored based
         * on the scores of {@code res1} and {@code res2}.
         */
        LimitedResult combine(LimitedResult res1, LimitedResult res2,
                              int[] ordering1, int[] ordering2,
                              double extra) {
            KMeansLimitedResult kres1 = (KMeansLimitedResult) res1;
            KMeansLimitedResult kres2 = (KMeansLimitedResult) res2;

            int[] newAssignments = combineAssignments(
                    res1, res2, ordering1, ordering2);
            int newNumClusters = res1.numClusters + res2.numClusters;
            double newScore = kres1.score + kres2.score;
            return new KMeansLimitedResult(
                    newAssignments, newNumClusters, newScore);
        }

        /**
         * Returns the k-means objective score.
         */
        public double score() {
            return score;
        }

        /**
         * Returns greater than 0 when this score is greater than other's score.
         */
        public double compareTo(LimitedResult other) {
            return this.score() - other.score();
        }
    }

    /**
     * Computes the relaxed correlation objective function when combining two
     * {@link LimitedResult}s.
     */
    private class SpectralLimitedResult extends LimitedResult {

        /**
         * The total inter-cluster similarity.
         */
        public double totalScore;

        /**
         * The full intra-cluster similarity.
         */
        public double rawInterScore;

        /**
         * The number of intra-cluster similarity comparisons.
         */
        public int interCount;

        /**
         * Constructs a new {@link SpectralLimitedResult}.
         */
        public SpectralLimitedResult(int[] assignments, int numClusters,
                                     double totalScore, double rawInterScore,
                                     int interCount) {
            super(assignments, numClusters);

            this.totalScore = totalScore;
            this.rawInterScore = rawInterScore;
            this.interCount = interCount;
        }

        /**
         * Returns {@link totalScore}.
         */
        public double score() {
            return totalScore;
        }

        /**
         * Returns greater than 0 when other's score is greater than this score.
         */
        public double compareTo(LimitedResult other) {
            return other.score() - this.score();
        }

        /**
         * Returns the combined {@link LimitedResult} when using the
         * relaxed correlation objective function. 
         */ 
        LimitedResult combine(LimitedResult res1, LimitedResult res2, 
                              int[] ordering1, int[] ordering2, double rhoSum) {
            // Assume that both are SpectralLimitedResults.
            SpectralLimitedResult sres1 = (SpectralLimitedResult) res1;
            SpectralLimitedResult sres2 = (SpectralLimitedResult) res2;

            // Get the raw combined assignments and the number of clusters.
            int[] newAssignments = combineAssignments(
                    res1, res2, ordering1, ordering2);
            int newNumClusters = res1.numClusters + res2.numClusters;

            // Use the raw inter-cluster similarity to compute the new
            // inter-cluster similarity and the number of inter-cluster
            // similarity computations.
            double newInterScore = sres1.rawInterScore + sres2.rawInterScore;
            int newCount = sres1.interCount + sres2.interCount;

            // We can compute the intra cluster scores by taking subtracting out
            // any self-similarity scores, any duplicate similarity scores, and
            // the inter-cluster similarity scores from rhoSum.  This leaves
            // only the intra-cluster similarity scores behind.
            double intraClusterScore =
                (rhoSum-newAssignments.length) / 2.0 - newInterScore;
            newInterScore = newCount - newInterScore;
            double newScore = alpha * newInterScore + beta * intraClusterScore;

            return new SpectralLimitedResult(newAssignments, newNumClusters,
                                            newScore, newInterScore, newCount);
        }
    }

    /**
     * A simple struct holding the cluster assignments and the number of
     * unique clusters generated.
     */
    private class ClusterResult {

        public int[] assignments;
        public int numClusters;

        public ClusterResult(int[] assignments, int numClusters) {
            this.assignments = assignments;
            this.numClusters = numClusters;
        }
    }
}
