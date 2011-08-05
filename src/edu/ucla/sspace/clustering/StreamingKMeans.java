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

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.Properties;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An implementation of a simple, highly accurate streaming K Means algorithm.
 * It is based on a forthcoming paper.
 *
 * </p>
 *
 * A key feature of this algorithm is that only one pass is made through a data
 * set.  It is intended for applications where the total number of data points
 * is known, but can be used if a rough estimate of the data points is known.
 * This algorithm periodically reformulates the centroids by performing an
 * batch form of K Means over the centroids, and potentially a sample of data
 * points assigned to each centroid, clearing out all centroids, and then
 * treating the old centroids as new heavily weighted data points.  This process
 * happens automatically when one of several thresholds are passed.
 *
 * @author Keith Stevens
 */
public class StreamingKMeans<T extends DoubleVector>
        implements Generator<OnlineClustering<T>> {

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.StreamingKMeans";

    /**
     * An estimate of the total number of data points that will be clustered.
     */
    public static final String NUM_POINTS_PROPERTY =
        PROPERTY_PREFIX + ".maxPoints";

    /**
     * An alpha value, see page 6 in the paper for details.
     */
    public static final String ALPHA_PROPERTY =
        PROPERTY_PREFIX + ".alpha";

    public static final String COFL_PROPERTY =
        PROPERTY_PREFIX + ".cofl";

    public static final String KOFL_PROPERTY =
        PROPERTY_PREFIX + ".kofl";

    /**
     * A beta value, see page 6 in the paper for details.
     */
    public static final String BETA_PROPERTY =
        PROPERTY_PREFIX + ".beta";

    /**
     * A gamma value, see page 6 in the paper for details.
     */
    public static final String GAMMA_PROPERTY =
        PROPERTY_PREFIX + ".gamma";

    /**
     * The default number of clusters.
     */
    public static final int DEFAULT_NUM_CLUSTERS = 2;

    /**
     * The default number of clusters.
     */
    public static final int DEFAULT_NUM_POINTS = 1000;

    /**
     * The default alpha value.
     */
    public static final double DEFAULT_ALPHA = 2.0;

    public static final double DEFAULT_COFL = 2.0;
    public static final double DEFAULT_KOFL = 2.0;

    /**
     * The default beta value.
     */
    public static final double DEFAULT_BETA = 216.25;

    /**
     * The default gamma value.
     */
    public static final double DEFAULT_GAMMA = 169/4.0;

    /** 
     * The maximum number of clusters permitted.
     */
    private final int numClusters;

    /**
     * The estimated number of data points.
     */
    private final double logNumPoints;

    /**
     * The alpha constant.
     */
    private final double alpha;

    private final double cofl;

    private final double kofl;

    /**
     * The beta constant.
     */
    private final double beta;

    /**
     * The gamma constant.
     */
    private final double gamma;

    /**
     * Creates a new generator using the system properties.
     */
    public StreamingKMeans() {
        this(new Properties());
    }

    /**
     * Creates a new generator using the given properties.
     */
    public StreamingKMeans(Properties props) {
        numClusters = props.getProperty(
            OnlineClustering.NUM_CLUSTERS_PROPERTY, DEFAULT_NUM_CLUSTERS);
        int numPoints = props.getProperty(
                NUM_POINTS_PROPERTY, DEFAULT_NUM_POINTS);
        logNumPoints = Math.log(numPoints) / Math.log(2);

        alpha = props.getProperty(ALPHA_PROPERTY, DEFAULT_ALPHA);
        cofl = props.getProperty(COFL_PROPERTY, DEFAULT_COFL);
        kofl = props.getProperty(KOFL_PROPERTY, DEFAULT_KOFL);

        beta = 2 * alpha * alpha * cofl + 2 * alpha;
	    gamma = Math.max(
                4*alpha*alpha*alpha*cofl*cofl + 2*alpha*alpha*cofl,
                beta*kofl+ 1);
    }

    /**
     * Generates a new instance of a {@code StreamingClustering} based on the
     * values used to construct this generator.
     */
    public OnlineClustering<T> generate() {
        return new StreamingKMeansClustering<T>(
                alpha, beta, gamma, numClusters, logNumPoints);
    }

    /**
     * Returns "StreamingKMeans"
     */
    public String toString() {
        return "StreamingKMeans";
    }

    /**
     * The internal {@link OnlineClustering} implementation.
     */
    public class StreamingKMeansClustering<T extends DoubleVector>
            implements OnlineClustering<T> {

        /**
         * A list of the first K data points.  After the first K data points
         * have been observed, this list is set to {@code null} and never
         * re-used.
         */
        private List<T> firstKPoints;

        /**
         * The scaled clustering cost.
         */
        private double LCost;

        /**
         * The cost of creating a new cluster.
         */
        private double facilityCost;

        /**
         * The total clustering cost.
         */
        private double totalCost;

        /**
         * The alpha constant used for evaluating the clustering cost and number
         * of clusters.
         */
        private final double alpha;

        /**
         * The beta constant used for evaluating the clustering cost and number
         * of clusters.
         */
        private final double beta;

        /**
         * The gamma constant used for evaluating the clustering cost and number
         * of clusters.
         */
        private final double gamma;

        /**
         * An estimate of the number of data points that will be observed.  This
         * is used for evaluating the clustering cost and number of clusters.
         */
        private final double logNumPoints;

        /** 
         * The number of clusters desired.
         */
        private final int numClusters;

        /**
         * The set of clusters.
         */
        private List<Cluster<T>> facilities;

        /**
         * A counter for generating item identifiers.
         */
        private final AtomicInteger idCounter;

        /**
         * The maximum total clustering cost, based on the constant values.
         */
        private final double costThreshold;

        /**
         * The maximum number of clusters, based on the constant values.
         */
        private final double facilityThreshold;

        /**
         * Creates a new instance of online KMeans clustering.
         */
        public StreamingKMeansClustering(double alpha, double beta, 
                                         double gamma, int numClusters,
                                         double logNumPoints) {
            // Create initial data structures.
            facilities = new CopyOnWriteArrayList<Cluster<T>>();
            idCounter = new AtomicInteger(1);
            firstKPoints = new ArrayList<T>(numClusters);

            // Save the constants.
            this.numClusters = numClusters;
            this.alpha = alpha;
            this.beta = beta;
            this.gamma = gamma;
            this.logNumPoints = logNumPoints;

            // Precompute the thresholds, which are constants as well.
            costThreshold = gamma;
            facilityThreshold = (1+logNumPoints) * numClusters;

            LCost = 1;
            facilityCost = LCost / (numClusters * (1 + logNumPoints));
            totalCost = 0;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized int addVector(T value) {
            // Get the id of the new data point.
            int id = idCounter.getAndAdd(1);

            // Try to assign the data point to a cluster.  If assigning the data
            // point causes either the cost threshold or number of clusters
            // threshold to be surpassed, try reclustering the centroids and
            // sampled data points, then cluster the data point again.  This may
            // take several iterations since the creation of new clusters is
            // done at random.
            if (addDataPoint(value, id) < 0) {
                // Reassign the centroid of each cluster.
                List<Cluster<T>> clusters = facilities;
                facilities = new ArrayList<Cluster<T>>();
                LCost *= beta;
                facilityCost = LCost/(numClusters*(1 + logNumPoints));

                // When reassigning each centroid, copy over the id of assigned
                // data points to the new cluster.
                for (Cluster<T> cluster : clusters) {
                    int assignment = addDataPoint(cluster.centroid(), 0);
                Cluster<T> newCluster = facilities.get(assignment);
                newCluster.dataPointIds().or(cluster.dataPointIds());
                }
            }
            return id;
        }

        /**
         * Assigns {@code value} to a cluster, or makes a new cluster with
         * {@code value} as the centroid, and returns the id of the cluster.
         * {@code -1} is returned if the data point cannot be assigned without
         * violating either the facility threshold or the cost threshold.
         *
         * @param value The value to cluster
         * @param id The unique identifier for {@code id}
         */
        private int addDataPoint(T value, int id) {
            // Find the cluster that is closest to value.
            double bestCost = Double.MAX_VALUE;
            int bestClusterId = 0;
            Cluster<T> bestCluster = null;
            int i = 0;
            for (Cluster<T> cluster : facilities) {
                double cost = cluster.compareWithVector(value);
                // Reverse the scale so that a high similarity corresponds to a
                // low cost and a low similarity corresponds to a high cost, but
                // is still from a 0 to 1 range.
                cost = -1*cost + 1;
                if (cost < bestCost) {
                    bestCost = cost;
                    bestCluster = cluster;
                    bestClusterId = i;
                }
                ++i;
            }

            // Determine whether or not a new facility, or cluster, should be
            // generated for this data point.  This based on the total cost of
            // serving this data point and the cost of creating a new
            // facility.
            double makeFacilityProb = Math.min(bestCost / facilityCost, 1);
            boolean makeFacility = facilities.size() == 0 || 
                                   Math.random() < makeFacilityProb;
              
            if (makeFacility) {
                Cluster<T> newCluster = new CentroidCluster<T>(
                  Vectors.instanceOf(value));
                newCluster.addVector(value, (id > 0) ? id : -1);
                facilities.add(newCluster);
                bestClusterId = facilities.size() - 1;
            } else {
                bestCluster.addVector(value, (id > 0) ? id : -1);
                totalCost += bestCost;
            }

            if (id != 0) {
                if (totalCost > gamma * LCost)
                    return -1;
                if (facilities.size() >= facilityThreshold)
                    return -2;
            }

            return bestClusterId;
        }

        /**
         * {@inheritDoc}
         */
        public Cluster<T> getCluster(int clusterIndex) {
            if (facilities.size() <= clusterIndex || clusterIndex < 0)
                throw new ArrayIndexOutOfBoundsException();
            return facilities.get(clusterIndex);
        }

        /**
         * {@inheritDoc}
         */
        public List<Cluster<T>> getClusters() {
            return facilities;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized int size() {
            return facilities.size();
        }

        /**
         * Returns a string describing this {@code ClusterMap}.
         */
        public String toString() {
            return "StreamingKMeansClustering-numClusters" + numClusters;
        }
    }
}
