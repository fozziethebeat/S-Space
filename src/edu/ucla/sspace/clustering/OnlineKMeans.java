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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.Properties;

import java.util.Iterator;
import java.util.List;

import java.util.concurrent.CopyOnWriteArrayList;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A {@link Generator} class for generating a new {@code OnlineKMeansClustering}
 * instance. This class supports the following properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WEIGHTING_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WEIGHT}
 *
 * <dd style="padding-top: .5em">This variable sets the weight given to the mean
 * vector in a rolling average of vectors.</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #MERGE_THRESHOLD_PROPERTY }
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MERGE_THRESHOLD}
 *
 * <dd style="padding-top: .5em">This variable sets the threshold for merging
 * two clusters. </p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #NUM_CLUSTERS}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MAX_CLUSTERS}
 *
 * <dd style="padding-top: .5em">This variable sets the maximum number of
 * clusters used.</p>
 *
 * </dl>
 *
 * @author Keith Stevens
 */
public class OnlineKMeans<T extends DoubleVector>
        implements Generator<OnlineClustering<T>> {

    /**
     * A property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.cluster.OnlineKMeans";

    /**
     * The property for setting the threshold for merging two clusters.
     */
    public static final String MERGE_THRESHOLD_PROPERTY =
        PROPERTY_PREFIX + ".merge";

    /**
     * The default merge threshold.
     */
    public static final double DEFAULT_MERGE_THRESHOLD = .35;

    /**
     * The default number of clusters.
     */
    public static final int DEFAULT_MAX_CLUSTERS = 15;

    /**
     * The threshold for clustering
     */
    private final double clusterThreshold;

    /** 
     * The maximum number of clusters permitted.
     */
    private final int maxNumClusters;

    /**
     * Creates a new generator using the system properties.
     */
    public OnlineKMeans() {
        this(new Properties());
    }

    /**
     * Creates a new generator using the given properties.
     */
    public OnlineKMeans(Properties props) {
        clusterThreshold = props.getProperty(
                    MERGE_THRESHOLD_PROPERTY, DEFAULT_MERGE_THRESHOLD);
        maxNumClusters = props.getProperty(
            OnlineClustering.NUM_CLUSTERS_PROPERTY, DEFAULT_MAX_CLUSTERS);
    }

    /**
     * Generates a new instance of a {@code OnlineClustering} based on the
     * values used to construct this generator.
     */
    public OnlineClustering<T> generate() {
        return new OnlineKMeansClustering<T>(clusterThreshold, maxNumClusters);
    }

    public String toString() {
        return "OnLineKMeans_" + maxNumClusters + "c_";
    }

    /**
     * A simple online implementation of K-Means clustering for {@code Vector}s,
     * with the option to perform agglomerative clustering once all elements
     * have been clustered.
     *
     * @author Keith Stevens
     */
    public class OnlineKMeansClustering<T extends DoubleVector>
            implements OnlineClustering<T> {

        /**
         * The threshold for clustering
         */
        private final double clusterThreshold;

        /** 
         * The maximum number of clusters permitted.
         */
        private final int maxNumClusters;

        /**
         * The set of clusters.
         */
        private final List<Cluster<T>> elements;

        /**
         * A counter for generating item identifiers.
         */
        private final AtomicInteger idCounter;

        /**
         * Creates a new instance of online KMeans clustering.
         */
        public OnlineKMeansClustering(double mergeThreshold,
                                      int maxNumClusters) {
            elements = new CopyOnWriteArrayList<Cluster<T>>();
            idCounter = new AtomicInteger(0);

            this.clusterThreshold = mergeThreshold;
            this.maxNumClusters = maxNumClusters;
        }

        /**
         * {@inheritDoc}
         */
        public int addVector(T value) {
            int id = idCounter.getAndAdd(1);

            Iterator<Cluster<T>> elementIter = elements.iterator();

            // Find the centriod with the best similarity.
            Cluster<T> bestMatch = null;
            int bestIndex = elements.size();
            double bestScore = -1;
            double similarity = -1;
            int i = 0;
            while (elementIter.hasNext()) {
                Cluster<T> cluster = elementIter.next();
                similarity = cluster.compareWithVector(value);
                if (similarity > bestScore) {
                    bestScore = similarity;
                    bestMatch = cluster;
                    bestIndex = i;
                }
                ++i;
            }

            // Add the current term vector if the similarity is high enough, or
            // set it as a new centroid.        
            if (bestScore >= clusterThreshold || 
                elements.size() >= maxNumClusters) {
                bestMatch.addVector(value, id);
            } else {
                // lock to ensure that the number of clusters doesn't change
                // while we add this one
                synchronized(elements) {
                    // Perform an additional check to see whether the number of
                    // elements changed while we waiting on the lock
                    if (elements.size() < maxNumClusters) {
                        bestMatch = new SynchronizedCluster<T>(
                            new CentroidCluster<T>(Vectors.instanceOf(value)));
                        elements.add(bestMatch);
                    } 
                    if (bestMatch != null)
                        bestMatch.addVector(value, id);
                }
            }
            return id;
        }

        /**
         * {@inheritDoc}
         */
        public Cluster<T> getCluster(int clusterIndex) {
            if (elements.size() <= clusterIndex)
                return null;
            return elements.get(clusterIndex);
        }

        /**
         * {@inheritDoc}
         */
        public List<Cluster<T>> getClusters() {
            return elements;
        }

        /**
         * {@inheritDoc}
         */
        public synchronized int size() {
            return elements.size();
        }

        /**
         * Returns a string describing this {@code ClusterMap}.
         */
        public String toString() {
            return "OnlineKMeansClustering-maxNumClusters" + maxNumClusters +
                   "-threshold" + clusterThreshold;
        }
    }
}
