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

package edu.ucla.sspace.vector;

import java.io.Serializable;

import java.util.Arrays;

/**
 * An {@code IntegerVector} class whose data is back by an array.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class DenseIntVector implements IntegerVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The array that contains the values of this vector
     */
    private final int[] vector;

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public DenseIntVector(int length) {
        vector = new int[length];
    }

    /**
     * Creates a new vector using the values of the specified vector.  The
     * created vector contains no references to the provided vector, so changes
     * to either will not be reflected in the other.
     *
     * @param v the intial values for this vector to have
     */
    public DenseIntVector(IntegerVector v) {
        vector = new int[v.length()];
        for (int i = 0; i < v.length(); ++i)
            vector[i] = v.get(i);
    }

    /**
     * Creates a new vector using the values of the specified array.  The
     * created vector contains no references to the provided array, so changes
     * to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public DenseIntVector(int[] values) {
        vector = Arrays.copyOf(values, values.length);
    }

    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        vector[index] += delta;
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public Integer getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        double m = 0;
        for (double i : vector)
            m += i * i;
        return Math.sqrt(m);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        vector[index] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray() {
        return Arrays.copyOf(vector, vector.length);
    }
}
