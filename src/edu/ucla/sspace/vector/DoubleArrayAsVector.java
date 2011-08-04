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

package edu.ucla.sspace.vector;

import java.io.Serializable;

import java.util.Arrays;


/**
 * A {@code Vector} backed by a {@code double} array.  Any changes to the vector
 * are reflected in the backing array.  This class provides a link between
 * array-based and {@code Vector}-based computation.
 *
 * @author David Jurgens
*/
class DoubleArrayAsVector implements DoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The array whose values back this vector
     */
    private final double[] array;

    /**
     * Creates a new vector back by the provided array
     *
     * @param vector the values of this vector
     */
    public DoubleArrayAsVector(double[] array) {
        assert array != null : "wrapped array cannot be null";
        this.array = array;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        return (array[index] += delta);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        array[index] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        array[index] = value.doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return array[index];
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return array[index];
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        double m = 0;
        for (double d : array)
            m += d * d;
        return Math.sqrt(m);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        return Arrays.copyOf(array, array.length);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return array.length;
    }
}
