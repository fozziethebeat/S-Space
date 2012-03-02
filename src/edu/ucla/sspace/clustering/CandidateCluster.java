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

import edu.ucla.sspace.util.primitive.IntSet;
import edu.ucla.sspace.util.primitive.TroveIntSet;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

/**
 * A shared utility class for representing a cluster to which data points have
 * been assigned.
 */
class CandidateCluster {    
        
    /**
     * The set of data identifiers that have currently been assigned to the
     * cluster
     */
    private final IntSet indices;

    /**
     * The sum of the data vectors that were assigned to the cluster
     */
    private DoubleVector sumVector;
    
    /**
     * The mean vector of the data vectors that have been assigned
     */
    private DoubleVector centroid;

    public CandidateCluster() {
        indices = new TroveIntSet();
        centroid = null;
    }

    /**
     * Returns the average data point assigned to this candidate cluster
     */
    public DoubleVector centerOfMass() {
        // Handle lazy initialization
        if (centroid == null) {
            if (indices.size() == 1)
                centroid = sumVector;
            else {
                // Update the centroid by normalizing by the number of elements.
                // We expect that the centroid vector might be compared with
                // other vectors multiple times. If we used a ScaledVector here,
                // we would be re-doing this multiplication each time, which is
                // wasted. The centerOfMass is already lazily instantiated, so
                // we know that if we do the computation here we'll be using the
                // results at least once.  Therefore do the normalization here
                // once to save cost.
                int length = sumVector.length();
                double d = 1d / indices.size();
                if (sumVector instanceof SparseVector) {
                    centroid = new SparseHashDoubleVector(length);
                    SparseVector sv = (SparseVector)sumVector;
                    for (int nz : sv.getNonZeroIndices())
                        centroid.set(nz, sumVector.get(nz) * d);
                }
                else {
                    centroid = new DenseVector(length);
                    for (int i = 0; i < length; ++i) 
                        centroid.set(i, sumVector.get(i) * d);                    
                }
            }
        }
        return centroid;
    }

    /**
     * Adds the data point with the specified index to the facility
     */
    public void add(int index, DoubleVector v) {
        boolean added = indices.add(index);
        assert added : "Adding duplicate indices to candidate facility";
        if (sumVector == null) {
            sumVector = (v instanceof SparseVector)
                ? new SparseHashDoubleVector(v)
                : new DenseVector(v);
        }
        else {
            VectorMath.add(sumVector, v);
            centroid = null;
        }           
    }
    
    public int hashCode() {
        return indices.hashCode();
    }
    
    public boolean equals(Object o) {
        if (o instanceof CandidateCluster) {
            CandidateCluster f = (CandidateCluster)o;
            return indices.equals(f.indices);
        }
        return false;
    }
    
    /**
     * Returns the set of indices for vectors in this cluster
     */
    public IntSet indices() {
        return indices;
    }

    /**
     * Merges the elements assigned to the other cluster into this one.
     */
    public void merge(CandidateCluster other) {
        indices.addAll(other.indices);
        VectorMath.add(sumVector, other.sumVector);
        centroid = null;
    }

    /**
     * Returns the number of elements that have been assigned to this cluster.
     */
    public int size() {
        return indices.size();
    }

    /**
     * Returns the unnormalized sum of the vectors for data points assigned to
     * this cluster.
     */
    public DoubleVector sum() {
        return sumVector;
    }
}

