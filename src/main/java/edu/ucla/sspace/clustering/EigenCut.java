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

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * An interface for computing the spectral cut of a {@link Matrix}.
 *
 * This interface is based on the spectral clustering algorithm described in the
 * following two papers:
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
 * To briefly summarize, these algorithms take in a data matrix and attempt to
 * recursively split the matrix using a spectral cut accross the according to
 * the link structure of the data points.  The algorithm assumes that the data
 * matrix is sparse and uses several optimizations to avoid computing the full
 * affinity matrix in memory, which would be both large and dense.  Instead, the
 * algorithm computes the dot product between each data point and the centroid
 * of all data points.  These values are then used to scale the data matrix and
 * compute the second eigen vector of the affinity matrix.  The second eigen
 * vector is used to find an optimal cut accross the affinity matrix.  The
 * "left" side of the cut should be made accessible by {@link getLeftCut} and
 * the "right" side of the cut should be made accessible by {@link getRightCut}.
 *
 * </p>
 *
 * Once the data matrix has been split until every data point is in it's own
 * partition, the algorithm merges portions of the tree using one of two
 * objective functions.  The first, and most often used, is a relaxed
 * correlation objective function.  This method balances inter cluster
 * similarity and intra cluster dissimilarity, with a varying weight between
 * the two scores.  {@link #getMergedObjective} and {@link #getSplitObjective}
 * compute this objective function, with the first method computing the score
 * when the full data matrix is used and the second method computing the score
 * accross a given set of paritions.  The second objective function is the
 * standard k-means objective function.  {@link #getKMeansObjective} computes
 * this, with one method computing the objective of the data set as a single
 * cluster and the other method computing the objective over a given set of
 * clusters.
 *
 * @see BaseSpectralCut
 *
 * @author Keith Stevens
 */
public interface EigenCut {

    /**
     * Returns the sum of values in {@code rho}.  This is equivalent to
     * sum({@code matrix} * {@code matrix}').
     */
    double rhoSum();

    /**
     * Computes the similarity between each data point and centroid of the data
     * set.  This is essentially the row sums of the affinity matrix for {@code
     * matrix}.
     */
    DoubleVector computeRhoSum(Matrix matrix);

    /**
     * Compute the cut with the lowest conductance for the data set.  This
     * involves the following main steps:
     *   <ol>
     *     <li> Computing the second eigen vector of the data set.</li>
     *     <li> Sorting both the eigen vector, and each dimensions
     *     corresponding data point, based on the eigen values.</li>
     *     </li> Dividing the original data matrix into two regions, which are
     *     hopefully of equal size.</li>
     *   </ol>
     * 
     * </p>
     *
     * The resulting regions are accessible by in {@link #getLeftCut} and {@link
     * #getRightCut}.
     */
    void computeCut(Matrix matrix);

    /**
     * Return the ordering of the first region with respect to the original data
     * set.
     */
    int[] getLeftReordering();

    /**
     * Returns the data set in the first (left) region.
     */
    Matrix getLeftCut();

    /**
     * Returns the data set in the second (right) region.
     */
    Matrix getRightCut();

    /**
     * Return the ordering of the second region with respect to the original
     * data set.
     */
    int[] getRightReordering();

    /**
     * Returns the K-Means objective score of the entire data set, i.e. the sum
     * of the similarity between each dataset and the centroid.
     */
    double getKMeansObjective();

    /**
     * Returns the K-Means objective computed over the two regions computed over
     * the data set.
     */
    double getKMeansObjective(double alpha, double beta,
                              int leftNumClusters, int[] leftAssignments,
                              int rightNumClusters, int[] rightAssignments);
    /**
     * Returns the score for the relaxed correlation objective when the data
     * matrix is divided into multiple clusters.  The relaxed correlation
     * objective measures both inter-cluster similarity and intra-cluster
     * dissimilarity.  A high score means that values with in a cluster are
     * highly similar and each cluster is highly distinct.  This is to be used
     * after clustering values in each sub region.
     * 
     * @param alpha The weight given to the inter-cluster similarity.
     * @param beta The weight given to the intra-cluster similarity.
     * @param leftNumClusters The number of clusters found in the left split
     * @param leftAssignments The assignments for data points in the left region
     * @param rightNumClusters The number of clusters found in the right split
     * @param rightAssignments The assignments for data points in the right
     *        region
     */
    double getSplitObjective(double alpha, double beta,
                             int leftNumClusters, int[] leftAssignments,
                             int rightNumClusters, int[] rightAssignments);

    /**
     * Returns the score for the relaxed correlation objective over the entire
     * data set, undivided.
     */
    double getMergedObjective(double alpha, double beta);
}
