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

import edu.ucla.sspace.util.DoubleEntry;
import edu.ucla.sspace.util.ObjectEntry;

import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Iterator;


/**
 * A {@code SparseVector} implementation backed by a {@code Map}.  This provides
 * amoritized constant time access to all get and set operations, while using
 * more space than the {@link CompactSparseVector} or {@link
 * AmortizedSparseVector} classes.
 *
 * @author David Jurgens
 */
public class SparseHashDoubleVector 
        implements SparseDoubleVector, Serializable, Iterable<DoubleEntry> {

    private static final long serialVersionUID = 1L;

    private TIntDoubleHashMap vector;

    private int maxLength;

    private double magnitude;

    /**
     * Creates a new vector with the maximum possible length.
     */
    public SparseHashDoubleVector() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public SparseHashDoubleVector(int length) {
        maxLength = length;
        vector = new TIntDoubleHashMap();
    }

    /**
     * Creates a new vector using the non-zero values of the specified array.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public SparseHashDoubleVector(double[] values) {
        maxLength = values.length;
        vector = new TIntDoubleHashMap();
        magnitude = 0;
        for (int i = 0; i < values.length; ++i) {
            if (values[i] != 0) {
                magnitude += values[i] * values[i];
                vector.put(i, values[i]);
            }
        }
        magnitude = Math.sqrt(magnitude);
    }

    /**
     * Create a {@code CompactSparseVector} using the indices and their
     * respecitve values.
     *
     * @param indices an sorted array of positive values representing the
     *        non-zero indices of the array
     * @param values an array of values that correspond their respective indices
     * @param length the total length of the array
     *
     * @throw IllegalArgumentException if {@code nonZeros} and {@code values}
     *        have different lengths or if {@code length} is less than any index
     *        found in {@code nonZeros}.
     */
    public SparseHashDoubleVector(int[] nonZeros, double[] values, int length) {
        if (nonZeros.length != values.length)
            throw new IllegalArgumentException(
                    "Length of the given nonZeros and values arrays must " +
                    "match.  Given: " + 
                    nonZeros.length + " and " + values.length);

        this.maxLength = length;
        this.vector = new TIntDoubleHashMap();
        this.magnitude = 0;
        for (int i = 0; i < nonZeros.length; ++i) {
            if (nonZeros[i] >= maxLength)
                throw new IllegalArgumentException(
                    "Length must be larger than the largest " +
                    "non zero index provided.  " +
                    "Length: " + length + ", index: " + nonZeros[i]);

            magnitude += values[i] * values[i];
            vector.put(nonZeros[i], values[i]);
        }
        magnitude = Math.sqrt(magnitude);
    }

    public SparseHashDoubleVector(DoubleVector values) {
        maxLength = values.length();
        vector = new TIntDoubleHashMap();
        magnitude = values.magnitude();
        if (values instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) values).getNonZeroIndices();
            for (int index : nonZeros)
                vector.put(index, values.get(index));
        } else {
            for (int index = 0; index < values.length(); ++index) {
                double value = values.get(index);
                if (value != 0d)
                    vector.put(index, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        double val = get(index) + delta;
        set(index, val);
        return val;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * Returns an iterator over the non-{@code 0} values in this vector.  This
     * method makes no guarantee about the order in which the indices are
     * returned.
     */
    public Iterator<DoubleEntry> iterator() {
        return new DoubleIterator();
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
    public void set(int index, double value) {
        if (value == 0d)
            vector.remove(index);
        else
            vector.put(index, value);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[length()];
        for (int i : vector.keys())
            array[i] = vector.get(i);
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return maxLength;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        if (magnitude < 0) {
            magnitude = 0;
            for (double d : vector.values())
                magnitude += d*d;
            magnitude = Math.sqrt(magnitude);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.keys();
    }

    /**
     * An iterator over the {@code double} values in the vector, wrapping the
     * backing {@code SparseHashArray}'s own iterator.
     */
    class DoubleIterator implements Iterator<DoubleEntry> {

        TIntDoubleIterator it;
        DoubleEntry entry;

        public DoubleIterator() {
            it = vector.iterator();
            entry = new DoubleEntry(0, 0);
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public DoubleEntry next() {
            it.advance();
            entry.index = it.key();
            entry.value = it.value();
            return entry;
        }

        public void remove() {
            throw new UnsupportedOperationException(
                "Cannot remove from vector");
        }
    }
}
