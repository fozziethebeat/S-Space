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

import java.util.List;
import java.util.Map;


/**
 * An interface for any Online clustering implementation.  When a vector is
 * added to a specific cluster, implementations are suggested to return a unique
 * cluster id specifying what cluster the item was assigned to.  At any time,
 * users of an {@link OnlineCustering} instance may request one or all of the
 * centroids.  These centroids may be created per request or maintained
 * internally at all times.
 *
 * </p>
 *
 * Implementations are expected to be thread-safe.
 *
 * @author Keith Stevens
 */
public interface OnlineClustering<T extends DoubleVector> {

    public static final String PROPERTY_PREFIX =
      "edu.ucla.sspace.clustering.OnlineClustering";

    public static final String NUM_CLUSTERS_PROPERTY =
      PROPERTY_PREFIX + ".numClusters";

    /**
     * Adds {@code value} a cluster.  The cluster may exist already or a new one
     * may be generated.  A unique identifier for the vector being clustered is
     * returned.  This identifier is to be used later on to query the cluster to
     * which the value was assigned.
     */
    int addVector(T value);

    /**
     * Returns the {@link Cluster} with id {@code clusterIndex}.
     */
    Cluster<T> getCluster(int clusterIndex);

    /**
     * Returns the list of all {@link Cluster}s.
     */
    List<Cluster<T>> getClusters();

    /**
     * Returns the numeber of {@link Cluster}s.
     */
    int size();
}
