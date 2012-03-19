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

package edu.ucla.sspace.util;


/**
 * A interface for array-like classes that use a sparse internal representation
 * to save space.  The exact details of performance are specified by the
 * implementation themselves.<p>
 *
 * Although interfaces cannot enforce constructors, it is suggested that
 * instances implement an additional constructor with an array parameter of the
 * required type.  This constructor should make a copy of the array so that
 * changes to one are not propagated.<p>
 *
 * In addition, it is suggested that instances that wrap Java primitive-type
 * arrays provide two additional methods, {@code getPrimitive} and {@code
 * setPrimitive}, which operate directly on the primitive types rather than
 * their object equivalents.  This allows users to save any performance costs
 * from auto-boxing the primitives.
 */
public interface SparseArray<T> {

    /**
     * Returns the number of non-zero values in this sparse array
     */
    int cardinality();

    /**
     * Returns the value of this array at the index.
     *
     * @param index the position in the array
     *
     * @return the object at the position
     */
    T get(int index);

    /**
     * Returns the indices of the array that contain non-{@code null} values.
     *
     * @return the indices that contain values
     */
    int[] getElementIndices();
    
    /**
     * Returns length of this array.  Note that implementations may define this
     * according to a preset, fixed length, or using the highest index set in
     * this array.
     */
    int length();

    /**
     * Sets the object as the value at the index.
     *
     * @param index an index in the array
     * @param obj the value
     */
    void set(int index, T obj);

    /**
     * Fills the provided array with the values contained in this array that fit
     * within the length of the provided array.
     *
     * @throws ClassCastException if the contents of this sparse array are not
     *         compatible with the type of the provided array
     */
    <E> E[] toArray(E[] array);
}