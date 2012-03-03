/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.hal;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;


/**
 * A sparse vector that represents the concatenations of two vectors together
 */
class ConcatenatedSparseDoubleVector implements SparseDoubleVector, 
                                                java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final SparseDoubleVector v1;

    private final SparseDoubleVector v2;

    public ConcatenatedSparseDoubleVector(SparseDoubleVector v1, 
                                          SparseDoubleVector v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double value) {
        return (index < v1.length())             
            ? v1.add(index, index)
            : v2.add(index - v1.length(), value);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return (index < v1.length()) 
            ? v1.get(index)
            : v2.get(index - v1.length());
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        int[] v1nz = v1.getNonZeroIndices();
        int[] v2nz = v2.getNonZeroIndices();
        int[] nz = new int[v1nz.length + v2nz.length];
        System.arraycopy(v1nz, 0, nz, 0, v1nz.length);
        // Include the values form v2 using the vector offset from v1's length
        for (int i = 0, j = v1nz.length; i < v2nz.length; ++i, ++j)
            nz[j] = v2nz[i] + v1.length();
        return nz;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return (index < v1.length()) 
            ? v1.getValue(index)
            : v2.getValue(index - v1.length());
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return v1.length() + v2.length();
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        double m = 0;
        for (int nz : v1.getNonZeroIndices()) {
            double d = v1.get(nz);
            m += d * d;
        }
        for (int nz : v2.getNonZeroIndices()) {
            double d = v2.get(nz);
            m += d * d;
        }
        return Math.sqrt(m);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        if (index < v1.length())             
            v1.set(index, index);
        else
            v2.set(index - v1.length(), value);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        if (index < v1.length())             
            v1.set(index, index);
        else
            v2.set(index - v1.length(), value);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[length()];
        for (int nz : v1.getNonZeroIndices())
            array[nz] = v1.get(nz);
        for (int nz : v2.getNonZeroIndices())
            array[nz + v1.length()] = v2.get(nz);
        return array;
    }
}
