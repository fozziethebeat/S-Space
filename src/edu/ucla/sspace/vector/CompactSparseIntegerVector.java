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

import edu.ucla.sspace.util.IntegerEntry;
import edu.ucla.sspace.util.SparseIntArray;

import java.io.Serializable;

import java.util.Iterator;

/**
 * A sparse {@code IntegerVector} class whose data is back by a compact sparse
 * array.  Access to the elements of this vector is {@code O(log(n))}.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class CompactSparseIntegerVector
        implements SparseIntegerVector, Serializable, Iterable<IntegerEntry> {

    private static final long serialVersionUID = 1L;

    /**
     * The sparse array whose data backs this vector
     */
    private final SparseIntArray intArray;

    /**
     * The magnitude of the vector or -1 if the value is currently invalid needs
     * to be recomputed
     */
    private double magnitude;

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public CompactSparseIntegerVector(int length) {
        intArray = new SparseIntArray(length);
        magnitude = 0;
    }

    /**
     * Creates a new vector using the values of the specified vector.  The
     * created vector contains no references to the provided vector, so changes
     * to either will not be reflected in the other.
     *
     * @param v the intial values for this vector to have
     */
    public CompactSparseIntegerVector(IntegerVector v) {
        intArray = new SparseIntArray(v.length());
        if (v instanceof SparseVector) {
            SparseVector sv = (SparseVector)v;
            for (int i : sv.getNonZeroIndices())
                intArray.set(i, v.get(i));
        }
        else {
            for (int i = 0; i < v.length(); ++i)
                intArray.set(i, v.get(i));
        }
        magnitude = -1;
    }

    /**
     * Creates a new vector using the non-zero values of the specified array.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public CompactSparseIntegerVector(int[] values) {
        intArray = new SparseIntArray(values);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        magnitude = -1;
        return intArray.addPrimitive(index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return intArray.getPrimitive(index);
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
    public int[] getNonZeroIndices() {
        return intArray.getElementIndices();
    }

    /**
     * Returns an iterator over all the non-zero indices and values in this
     * vector.
     */
    public Iterator<IntegerEntry> iterator() {
        return intArray.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return intArray.length();
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            for (int nz : getNonZeroIndices()) {
                int i = intArray.get(nz);
                m += i * i;
            }
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        intArray.set(index,  value);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.intValue());
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray() {
        int[] array = new int[intArray.length()];
        for (int i : intArray.getElementIndices())
            array[i] = intArray.getPrimitive(i);
        return array;
    }

    public String toString() {
        return intArray.toString();
    }
}
