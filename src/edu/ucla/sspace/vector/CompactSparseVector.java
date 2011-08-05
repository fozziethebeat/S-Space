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

import edu.ucla.sspace.util.DoubleEntry;
import edu.ucla.sspace.util.SparseDoubleArray;

import java.io.Serializable;

import java.util.Iterator;


/**
 * A {@code Vector} instance that keeps only the non-zero values in memory,
 * thereby saving space at the expense of time.
 *
 * <p> See {@link SparseDoubleArray} for details on how the sparse
 * representation is implemented.
 *
 * @author Keith Stevens
 */
public class CompactSparseVector
    implements SparseDoubleVector, Serializable, Iterable<DoubleEntry> {

    private static final long serialVersionUID = 1L;

    /**
     * The {@code SparseDoubleArray} which provides most of the functionality in
     * this class.
     */
    private SparseDoubleArray vector;

    /**
     * The magnitude of the vector or -1 if the value is currently invalid and
     * needs to be recomputed.
     */
    private double magnitude;

    /**
     * Creates a {@code CompactSparseVector} that grows to the maximum size set
     * by {@link Double#MAX_VALUE}.
     */
    public CompactSparseVector() {
        vector = new SparseDoubleArray();
        magnitude = 0;
    }

    /** 
     * Create a {@code CompactSparseVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code CompactSparseVector}.
     */
    public CompactSparseVector(int length) {
        vector = new SparseDoubleArray(length);
        magnitude = 0;
    }

    /**
     * Create a {@code CompactSparseVector} from an array, saving only the non
     * zero entries.
     *
     * @param array The double array to produce a sparse vector from.
     */
    public CompactSparseVector(double[] array) {
        vector = new SparseDoubleArray(array);
        magnitude = -1;
    }

    /**
     * Create a {@code CompactSparseVector} from an array, saving only the non
     * zero entries.
     *
     * @param array The double array to produce a sparse vector from.
     */
    public CompactSparseVector(SparseDoubleVector v) {
        int length = v.length();
        int[] nz = v.getNonZeroIndices();
        double[] values = new double[nz.length];
        for (int i = 0; i < nz.length; ++i)
            values[i] = v.get(nz[i]);
        vector = new SparseDoubleArray(nz, values, length);
        magnitude = -1;
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
     * @throw IllegalArgumentException if {@code indices} and {@code values}
     *        have different lengths or if {@code indices} contains duplicate
     *        elements or those not in sorted order
     */
    public CompactSparseVector(int[] nonZeroIndices, double[] values, 
                               int length) {
        vector = new SparseDoubleArray(nonZeroIndices, values, length);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        magnitude = -1;
        return vector.addPrimitive(index, delta);
    }

    /**
     * Returns an iterator over all the non-zero indices and values in this
     * vector.
     */
    public Iterator<DoubleEntry> iterator() {
        return vector.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        vector = new SparseDoubleArray(values);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.setPrimitive(index, value);
        magnitude = -1;
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
    public int[] getNonZeroIndices() {
        return vector.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            for (DoubleEntry e : this)
                m += e.value() * e.value();
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[vector.length()];
        return vector.toPrimitiveArray(array);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.getPrimitive(index);
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
    public int length() {
        return vector.length();
    }

    public String toString() {
        return vector.toString();
    }
}
