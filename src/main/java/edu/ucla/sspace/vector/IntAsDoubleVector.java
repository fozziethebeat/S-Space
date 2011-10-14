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


/**
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class IntAsDoubleVector extends VectorView<Double>
         implements DoubleVector, Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * An {@link IntegerVector} reference to the backing vector
     */
    private final IntegerVector intVector;

    /**
     * Creates a new {@link DoubleVector} view of the data in the provided
     * {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     */
    public IntAsDoubleVector(IntegerVector v) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} view of the data
     * in the provided {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector
     */
    public IntAsDoubleVector(IntegerVector v, boolean isImmutable) {
        this(v, 0, v.length(), isImmutable);
    }

    /**
     * Creates a new {@link DoubleVector} sub-view of the data in the provided
     * {@link IntegerVector} using the offset and length to specify a viewing
     * region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     */
    public IntAsDoubleVector(IntegerVector v, int offset, int length) {
        this(v, offset, length, false);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} sub-view of the
     * data in the provided {@link IntegerVector} using the offset and length to
     * specify a viewing region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector.
     */
    public IntAsDoubleVector(IntegerVector v, int offset, int length, 
                             boolean isImmutable) {
        super(v, offset, length, isImmutable);
        intVector = v;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        return intVector.add(getIndex(index), (int)delta);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        intVector.set(getIndex(index), (int)value);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return intVector.get(getIndex(index));
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
    public double[] toArray() {
        double[] r = new double[vectorLength - vectorOffset];
        for (int i = vectorOffset; i < vectorLength; ++i)
            r[i] = intVector.get(i);
        return r;
    }
}
