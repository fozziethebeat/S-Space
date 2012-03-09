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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.seeding.GeneralizedOrssSeed;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;
import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.info;


/**
 * An implementation of Shindler's streaming K Means algorithm.  It is based on
 * a the following paper:
 *
 * <ul> <li> Michael Shindler, Alex Wong, and Adam Meyerson. Fast and Accurate
 *   k-means for Large Datasets. In <i>NIPS</i>, 2011..  Available online <a
 *   href="http://web.engr.oregonstate.edu/~shindler/papers/FastKMeans_nips11.pdf">here</a>
 *   </li> </ul>
 *
 * </p>
 *
 * A key feature of this algorithm is that only one pass is made through a data
 * set and is intended for applications where the total number of data points is
 * known, but the data set cannot be held in memory at any given time.
 *
 * @author David Jurgens
 */
public class FastStreamingKMeans {

    private static final Logger LOGGER = 
        Logger.getLogger(FastStreamingKMeans.class.getName());

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.FastStreamingKMeans";

    /**
     * The property for specifying the beta value which determines increases to
     * the facilities cost for creating a new facility, see page 6 in the paper
     * for details.
     */
    public static final String BETA_PROPERTY =
        PROPERTY_PREFIX + ".beta";

    /**
     * The property for specifying kappa, the maximum number of facilities.  By
     * default kappa is selected as {@code k * log(num data points)}.
     */
    public static final String KAPPA_PROPERTY =
        PROPERTY_PREFIX + ".kappa";

    /**
     * The property for specifying the similarity function with which to compare
     * data points
     */
    public static final String SIMILARITY_FUNCTION_PROPERTY =
        PROPERTY_PREFIX + ".similarityFunction";

    /**
     * The default value of beta that appeared to work best according ot Michael
     * Shindler
     */
    public static final double DEFAULT_BETA = 74;

    /**
     * The upper limit on the number of batch k-means iterations performed when
     * coverting from kappa facilities to <i>k</i> facilities.  This upper bound
     * is provided as a guard against potential edge cases where the batch
     * k-means gets stuck oscilating between a few potential solutions without
     * converging.
     */
    private static final int MAX_BATCH_KMEANS_ITERS = 1000;

    /**
     * The default similarity function is in the inverse of the square of the
     * Euclidean distances, which preserves all the properties specified in the
     * Shindler et al (2011) paper.
     */
    public static final SimilarityFunction DEFAULT_SIMILARITY_FUNCTION =
        new SimilarityFunction() {
            public void setParams(double... arguments) { }
            public boolean isSymmetric() { return true; }
            public double sim(IntegerVector v1, IntegerVector v2) {
                return sim(Vectors.asDouble(v1), Vectors.asDouble(v2));
            }
            public double sim(Vector v1, Vector v2) {
                return sim(Vectors.asDouble(v1), Vectors.asDouble(v2));
            }
            public double sim(DoubleVector v1, DoubleVector v2) {
                if (v1.length() != v2.length())
                    throw new IllegalArgumentException(
                        "vector lengths are different");
                int length = v1.length();
                double sum = 0d;
                for (int i = 0; i < length; ++i) {
                    double d1 = v1.get(i);
                    double d2 = v2.get(i);
                    sum += (d1 - d2) * (d1 - d2);
                }
                return (sum > 0) ? 1d / sum : 1;
            }

        };


    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        throw new UnsupportedOperationException(
            "Must specify the number of clusters");
    }

    /**
     * Clusters the set of rows in the given {@code Matrix} into the specified
     * number of clusters and using the default values for beta, kappa, and the
     * {@link SimilarityFunction}, unless otherwise specified in the properties.
     *
     * @param matrix {@inheritDoc}
     * @param numClusters {@inheritDoc}
     * @param props {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, int numClusters, Properties props) {
        if (matrix == null || props == null)
            throw new NullPointerException();
        if (numClusters < 1)
            throw new IllegalArgumentException(
                "The number of clusters must be positive");
        if (numClusters > matrix.rows())
            throw new IllegalArgumentException(
                "The number of clusters exceeds the number of data points");

        int kappa = (int)(numClusters * (1 + Math.log(matrix.rows())));
        String kappaProp = props.getProperty(KAPPA_PROPERTY);
        if (kappaProp != null) {
            try {
                int k = Integer.parseInt(kappaProp);
                if (k < numClusters
                        || k > numClusters * (1 + Math.log(matrix.rows())))
                    throw new IllegalArgumentException(
                        "kappa must be at least the number of clusters, k, " +
                        "and at most k * log(matrix.rows())");
                kappa = k;
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid kappa", nfe);
            }
        }

        double beta = DEFAULT_BETA;
        String betaProp = props.getProperty(BETA_PROPERTY);
        if (betaProp != null) {
            try {
                double b = Double.parseDouble(betaProp);
                if (b <= 0)
                    throw new IllegalArgumentException(
                        "beta must be positive");
                beta = b;
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("Invalid beta", nfe);
            }
        }

        SimilarityFunction simFunc = DEFAULT_SIMILARITY_FUNCTION;
        String simFuncProp = props.getProperty(SIMILARITY_FUNCTION_PROPERTY);
        if (simFuncProp != null) {
            try {
                SimilarityFunction sf = ReflectionUtil
                    .<SimilarityFunction>getObjectInstance(simFuncProp);
                simFunc = sf;
            } catch (Error e) {
                throw new IllegalArgumentException(
                    "Invalid similarity function class", e);
            }
        }

        return cluster(matrix, numClusters, kappa, beta, simFunc);
    }

    /**
     * Clusters the rows of the provided matrix into the specified number of
     * clusters in a single pass using the parameters to guide how clusters are
     * formed.  Note that due to the streaming nature of the algorithm, fewer
     * than {@code numClusters} may be returned.
     *
     * @param matrix the matrix whose rows are to be clustered
     * @param numClusters the number of clusters to be returned.  Note that
     *        under some circumstances, the algorithm may return fewer clusters
     *        than this amount.
     * @param kappa the maximum number of facilities (clusters) to keep in
     *        memory at any given point.  At most this should be {@code
     *        numClusters * Math.log(matrix.rows())}
     * @param beta the initial cost for creating a new facility.  The default
     *        value of {@value #DEFAULT_BETA_VALUE} is recommended for this
     *        parameter, unless specific customization is required.
     * @param simFunc the similarity function used to compare rows of the
     *        matrix.  In the original paper, this is the inverse of square of
     *        the Euclidean distance.
     *
     * @return an assignment from each row of the matrix to a cluster
     *         identifier.
     */
    public Assignments cluster(Matrix matrix, int numClusters, 
                               int kappa, double beta,
                               SimilarityFunction simFunc) {

        int rows = matrix.rows();
        int cols = matrix.columns();

        // f is the facility cost;
        double f = 1d / (numClusters * (1 + Math.log(rows)));
        
        // This list contains at most kappa facilities.
        List<CandidateCluster> facilities = 
            new ArrayList<CandidateCluster>(kappa);

        for (int r = 0; r < rows; /* no auto-increment */) {
            
            for ( ; facilities.size() <= kappa && r < rows; ++r) {
                DoubleVector x = matrix.getRowVector(r);
                
                CandidateCluster closest = null;
                // Delta is ultimately assigned the lowest inverse-similarity
                // (distance) to any of the current facilities' center of mass
                double delta = Double.MAX_VALUE;
                for (CandidateCluster y : facilities) {
                    double similarity = 
                        simFunc.sim(x, y.centerOfMass());
                    double invSim = invertSim(similarity);
                    if (invSim < delta) {
                        delta = invSim;
                        closest = y;
                    }
                }
                
                // Base case: If this is the first data point and there are no
                // other facilities
                //
                // Or if we surpass the probability of a new event occurring
                // (line 6)
                if (closest == null || Math.random() < delta / f) {
                    CandidateCluster fac = new CandidateCluster();
                    fac.add(r, x);
                    facilities.add(fac);
                }
                // Otherwise, add this data point to an existing facility
                else {
                    closest.add(r, x);
                }

            }

            // If we still have data points left to process (line 10:)
            if (r < rows) {
                // Check whether we have more than kappa clusters (line 11).
                // Kappa provides the upper bound on the clusters (facilities)
                // that are kept at any given time.  If there are more, then we
                // need to consolidate facilities
                while (facilities.size() > kappa) {
                    f *= beta;

                    int curNumFacilities = facilities.size();
                    List<CandidateCluster> consolidated = 
                        new ArrayList<CandidateCluster>(kappa);
                    consolidated.add(facilities.get(0));
                    for (int j = 1; j < curNumFacilities; ++j) {
                        CandidateCluster x = facilities.get(j);
                        int pointsAssigned = x.size();
                        // Compute the similarity of this facility to all other
                        // consolidated facilities.  Delta represents the lowest
                        // inverse-similarity (distance) to another facility.
                        // See line 17 of the algorithm.
                        double delta = Double.MAX_VALUE;                        
                        CandidateCluster closest = null;
                        for (CandidateCluster y : consolidated) {
                            double similarity = 
                                simFunc.sim(x.centerOfMass(), y.centerOfMass());
                            double invSim = invertSim(similarity);
                            if (invSim < delta) {
                                delta = invSim;
                                closest = y;
                            }
                        }
                         
                        // Use (pointsAssigned * delta / f) as a threshold for
                        // whether this facility could constitute a new event.
                        // If a random check is less than it, then we nominate
                        // this facilty to continue.
                        if (Math.random() < (pointsAssigned * delta) / f) {
                            consolidated.add(x);
                        }
                        // Otherwise, we consolidate the points in this
                        // community to the closest facility
                        else {
                            assert closest != null : "no closest facility";
                            closest.merge(x);
                        }
                    }
                    verbose(LOGGER, "Consolidated %d facilities down to %d",
                            facilities.size(), consolidated.size());
                    facilities = consolidated;
                }
            }
            // Once we have processed all of the items in the stream (line 23 of
            // algorithm), reduce the kappa clusters into k clusters.
            else {
                // Edge case for when we already have fewer facilities than we
                // need
                if (facilities.size() <= numClusters) {
                    verbose(LOGGER, "Had fewer facilities, %d, than the " +
                            "requested number of clusters %d",
                            facilities.size(), numClusters);

                    // There's no point in reducing the number of facilities
                    // further since we're under the desired amount, nor can we
                    // go back and increase the number of facilities since all
                    // the data has been seen at this point.  Therefore, just
                    // loop through the candidates and report their assignemnts.
                    Assignment[] assignments = new Assignment[rows];
                    int numFacilities = facilities.size();
                    for (int j = 0; j < numFacilities; ++j) {
                        CandidateCluster fac = facilities.get(j);
                        veryVerbose(LOGGER, "Facility %d had a center of mass at %s",
                                    j, fac.centerOfMass());
                        
                        int clusterId = j;
                        IntIterator iter = fac.indices().iterator();
                        while (iter.hasNext()) {
                            int row = iter.nextInt();
                            assignments[row] = 
                                new HardAssignment(clusterId);
                        }
                    }
                    return new Assignments(numClusters, assignments, matrix);                    
                }
                else {
                    verbose(LOGGER, "Had more than %d facilities, " +
                            "consolidating to %d", facilities.size(), 
                            numClusters);
                    
                    List<DoubleVector> facilityCentroids = 
                        new ArrayList<DoubleVector>(facilities.size());
                    int[] weights = new int[facilities.size()];
                    int i = 0;
                    for (CandidateCluster fac : facilities) {
                        facilityCentroids.add(fac.centerOfMass());
                        weights[i++] = fac.size();
                    }
                    // Wrap the facilities centroids in a matrix for convenience
                    Matrix m = Matrices.asMatrix(facilityCentroids);

                    // Select the initial seed points for reducing the kappa
                    // clusters to k using the generalized ORSS selection
                    // process, which supports data comparisons other than
                    // Euclidean distance
                    GeneralizedOrssSeed orss = new GeneralizedOrssSeed(simFunc);
                    DoubleVector[] centroids = orss.chooseSeeds(numClusters, m);
                    assert nonNullCentroids(centroids) 
                        : "ORSS seed returned too few centroids";
                    
                    // This records the assignments of the kappa facilities to
                    // the k centers.  Initially, everyhting is assigned to the
                    // same center and iterations repeat until convergence.
                    int[] facilityAssignments = new int[facilities.size()];
                    
                    // Using those facilities as starting points, run k-means on
                    // the facility centroids until no facilities change their
                    // memebership.
                    int numChanged = 0;
                    int kmeansIters = 0;
                    do {
                        numChanged = 0;
                        // Recompute the new centroids each time
                        DoubleVector[] updatedCentroids = 
                            new DoubleVector[numClusters];
                        for (i = 0; i < updatedCentroids.length; ++i) 
                            updatedCentroids[i] = new DenseVector(cols);
                        int[] updatedCentroidSizes = new int[numClusters];

                        double similaritySum = 0;
                        
                        // For each CandidateCluster find the most similar centroid
                        i = 0;
                        for (CandidateCluster fac : facilities) {
                            int mostSim = -1;
                            double highestSim = -1;
                            for (int j = 0; j < centroids.length; ++j) {
//                                  System.out.printf("centroids[%d]: %s%n fac.centroid(): %s%n",
//                                                    j, centroids[j], 
//                                                    fac.centerOfMass());
                                double sim = simFunc.sim(centroids[j], 
                                                         fac.centerOfMass());
                                if (sim > highestSim) {
                                    highestSim = sim;
                                    mostSim = j;
                                }
                            }

                            // For the most similar centroid, update its center
                            // of mass for the next round with the weighted
                            // vector
                            VectorMath.add(updatedCentroids[mostSim], 
                                           fac.sum());
                            updatedCentroidSizes[mostSim] += fac.size();
                            int curAssignment = facilityAssignments[i];
                            facilityAssignments[i] = mostSim;
                            similaritySum += highestSim;
                            if (curAssignment != mostSim) { 
                                veryVerbose(LOGGER, "Facility %d changed its " +
                                            "centroid from %d to %d",
                                            i, curAssignment, mostSim);
                                numChanged++;
                            }
                            i++;
                        }

                        // Once all the facilities have been assigned to one of
                        // the k-centroids, recompute the centroids by
                        // normalizing the sum of the weighted vectors according
                        // the number of points
                        for (int j = 0; j < updatedCentroids.length; ++j) {
                            DoubleVector v = updatedCentroids[j];
                            int size = updatedCentroidSizes[j];
                            for (int k = 0; k < cols; ++k) 
                                v.set(k, v.get(k) / size);
                            // Update this centroid for the next round
                            centroids[j] = v;                            
                        }

                        veryVerbose(LOGGER, "%d centroids swapped their facility",
                                    numChanged);
                    } while (numChanged > 0 && 
                                 ++kmeansIters < MAX_BATCH_KMEANS_ITERS);

                    // Use the final assignments to create assignments for each
                    // of the input data points
                    Assignment[] assignments = new Assignment[rows];
                    for (int j = 0; j < facilityAssignments.length; ++j) {
                        CandidateCluster fac = facilities.get(j);
                        veryVerbose(LOGGER, "Facility %d had a center of mass at %s",
                                    j, fac.centerOfMass());
                        
                        int clusterId = facilityAssignments[j];
                        IntIterator iter = fac.indices().iterator();
                        while (iter.hasNext()) {
                            int row = iter.nextInt();
                            assignments[row] = 
                                new HardAssignment(clusterId);
                        }
                    }
                    return new Assignments(numClusters, assignments, matrix);
                }
            }
        }
        throw new AssertionError(
            "Processed all data points without making assignment");
    }

    static boolean nonNullCentroids(DoubleVector[] centroids) {
        for (int i = 0; i < centroids.length; ++i)
            if (centroids[i] == null)
                return false;
        return true;
    }
    
    /**
     * Inverts the similarity, effectively turning it into a distance
     */
    static double invertSim(double sim) {
        assert sim >= 0 : "negative similarity invalidates distance conversion";
        return (sim == 0) ? 0 : 1d / sim;
    }

}
