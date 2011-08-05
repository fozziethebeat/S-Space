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

import edu.ucla.sspace.vector.DoubleVector;

import java.util.BitSet;
import java.util.List;


/**
 * An interface for representing a single cluster, which is composed of several
 * data points.  Each cluster is expected to always be capable of generating a
 * centroid and a subset of vectors which were assigned to the cluster.  The
 * subset of assigned vectors may be empty.  In addition, each cluster records
 * the indentifier of every data point assigned to the cluster, allowing users
 * the option of storing all potential {@link Vector}s when the {@link Cluster}
 * itself may discard them and then performing additional operations on the
 * vectors.
 *
 * @author Keith Stevens
 */
public interface Cluster<T extends DoubleVector> {

    /**
     * Adds {@code vector} with {@code id} to this {@link Cluster}.
     */
    void addVector(T vector, int id);

    /**
     * Returns the similarity between this {@link Cluster} and {@code vector}.
     */
    double compareWithVector(T vector);

    /**
     * Returns the centroid of this cluster.
     */
    T centroid();

    /**
     * Returns the set of stored data points, if any, for this cluster.
     */
    List<T> dataPointValues();

    /**
     * Returns the set of identifiers for all data points assigned to this
     * cluster.
     */
    BitSet dataPointIds();

    /**
     * Merges the {@code other} {@link Cluster} with this {@link Cluster}.
     * This {@link Cluster} will absorb all data points from {@code other} and
     * update the centroid and data point assignments as needed.
     */
    void merge(Cluster<T> other);

    /**
     * Returns the total number of points assigned to this {@link Cluster}.
     */
    int size();
}
