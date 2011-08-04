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


/**
 * An generalized interface for vectors.  This interface allows implementations
 * to implement the vector with any kind of underlying data type, but the input
 * and output data types must be doubles.
 *
 * <p>Methods which modify the state of a {@code Vector} are optional.
 * Implementations that are not modifiable should throw an {@code
 * UnsupportedOperationException} if such methods are called.  These methods are
 * marked as "optional" in the specification for the interface.
 *
 * @author Keith Stevens
 */
public interface DoubleVector extends Vector<Double> {

    /**
     * Changes the value in this vector by a specified amount (optional
     * operation).  If there is not a value set at index, delta should be set to
     * the actual value.
     *
     * @param index index to change.
     * @param delta the amount to change by.
     * @return the resulting value at the index
     */
    double add(int index, double delta);

    /**
     * Returns the value of this vector at the given index.
     *
     * @param index index to retrieve.
     * @return value at index.
     */
    double get(int index);

    /**
     * Returns the value of the vector at the given index as a {@code Double}.
     *
     * @param {@inheritDoc}
     * @return {@inheritDoc}
     */
    Double getValue(int index);

    /**
     * Sets the length in this vector (optional operation).
     *
     * @param index index to set.
     * @param value value to set in the vector.
     */
    void set(int index, double value);

    /**
     * Returns a double array representing this vector.  The returned array will
     * be "safe" in that no changes to the array will be reflected in the
     * vector, and likewise for changes to to the vector.  The caller is thus
     * free to modify the returned array.
     *
     * @return a {@code double} array of this vector.
     */
    double[] toArray();
}
