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
 * A synchronized decorator for {@link Cluster}s.
 *
 * @author Keith Stevens
 */
public class SynchronizedCluster<T extends DoubleVector> implements Cluster<T> {

    /**
     * The base {@link Cluster} that is being synchronized.
     */
    private Cluster<T> cluster;

    /**
     * Creates a synchronized accessor to {@code cluster}.
     */
    public SynchronizedCluster(Cluster<T> cluster) {
        this.cluster = cluster;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addVector(T vector, int id) {
        cluster.addVector(vector, id);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double compareWithVector(T vector) {
        return cluster.compareWithVector(vector);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized T centroid() {
        return cluster.centroid();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized List<T> dataPointValues() {
        return cluster.dataPointValues();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized BitSet dataPointIds() {
        return cluster.dataPointIds();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void merge(Cluster<T> other) {
        cluster.merge(other);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int size() {
        return cluster.size();
    }
}
