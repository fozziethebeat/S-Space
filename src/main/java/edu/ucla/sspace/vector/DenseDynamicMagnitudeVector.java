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

package edu.ucla.sspace.vector;

import edu.ucla.sspace.util.DoubleEntry;

import java.io.Serializable;

import java.util.Arrays;


/**
 * A {@code Vector} where all values are held in memory. The underlying
 * implementation is simply an array of doubles.  This implementation updates
 * the magnitude everytime a value is modified.  This can be a benefit if the
 * vector regularly needs the magnitude to be computed but also has minor
 * changes to it between each computation.  <p>
 *
 * @author Keith Stevens
*/
public class DenseDynamicMagnitudeVector implements DoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The values of this {@code DenseDynamicMagnitudeVector}.
     */
    private double[] vector;

    /**
     * The magnitude of the vector or -1 if the value is currently invalid and
     * needs to be recomputed
     */
    private double magnitude;

    /**
     * Create an {@code DenseDynamicMagnitudeVector} with all values starting at 0 with
     * the given length.
     *
     * @param vectorLength The size of the vector to create.
     */
    public DenseDynamicMagnitudeVector(int vectorLength) {
        vector = new double[vectorLength];
        magnitude = 0;
    }

    /**
     * Create a {@code DenseDynamicMagnitudeVector} taking the values given by {@code vector}.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param vector The vector values to start with.
     */
    public DenseDynamicMagnitudeVector(double[] vector) {
        this.vector = Arrays.copyOf(vector, vector.length);
        magnitude = 0;
        for (double d : vector)
            magnitude += d * d;
    }
	
    /**
     * Create a {@code DenseDynamicMagnitudeVector} by copying the values from
     * another {@code Vector}.
     *
     * @param vector The {@code Vector} to copy from.
     */
    @SuppressWarnings("unchecked")
    public DenseDynamicMagnitudeVector(DoubleVector v) {
        this.vector = new double[v.length()];
        magnitude = v.magnitude();
        magnitude = magnitude * magnitude;
        if (v instanceof Iterable) {
            for (DoubleEntry e : ((Iterable<DoubleEntry>)v)) 
                vector[e.index()] = e.value();
        }
        else if (v instanceof SparseDoubleVector) {
            for (int i : ((SparseDoubleVector)v).getNonZeroIndices())
                vector[i] = v.get(i);
        }
        else {
            for (int i = 0; i < v.length(); ++i)
                vector[i] = v.get(i);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        magnitude -= vector[index] * vector[index];
        vector[index] += delta;
        magnitude += vector[index] * vector[index];
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        magnitude -= vector[index] * vector[index];
        vector[index] = value;
        magnitude += vector[index] * vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        return Math.sqrt(magnitude);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        return Arrays.copyOf(vector, vector.length);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length;
    }
}
