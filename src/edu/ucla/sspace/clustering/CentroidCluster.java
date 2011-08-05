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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.VectorIO;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class CentroidCluster<T extends DoubleVector> implements Cluster<T> {

    /**
     * The centroid of this {@link Cluster}.  This is the only data
     * representation stored for the {@link Cluster}.
     */
    private T centroid;

    /**
     * The set of data point id's that assigned to this {@link Cluster}.
     */
    private BitSet assignments;

    /**
     * Creates a new {@link CentroidCluster} that takes ownership of {@code
     * emptyVector} as the centroid for this {@link Cluster}. {@code
     * emptyVector} should have length equal to the length of vectors that will
     * be assigned to this {@link Cluster} and should be dense if a large number
     * of vectors, or any dense vectors, are expected to be assigned to this
     * {@link Cluster}.
     */
    public CentroidCluster(T emptyVector) {
        centroid = emptyVector;
        assignments = new BitSet();
    }

    /**
     * {@inheritDoc}
     */
    public void addVector(T vector, int id) {
        VectorMath.add(centroid, vector);
        if (id >= 0)
                assignments.set(id);
    }

    /**
     * {@inheritDoc}
     */
    public double compareWithVector(T vector) {
        return Similarity.cosineSimilarity(centroid, vector);
    }

    /**
     * {@inheritDoc}
     */
    public T centroid() {
        return centroid;
    }

    /**
     * {@inheritDoc}
     */
    public List<T> dataPointValues() {
        return new ArrayList<T>();
    }

    /**
     * {@inheritDoc}
     */
    public BitSet dataPointIds() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    public void merge(Cluster<T> other) {
        VectorMath.add(centroid, other.centroid());
        for (T otherDataPoint : other.dataPointValues())
            VectorMath.add(centroid, otherDataPoint);

        for (int i = other.dataPointIds().nextSetBit(0); i >= 0;
                 i = other.dataPointIds().nextSetBit(i+1))
            assignments.set(i);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return assignments.size();
    }
}
