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
 * A view of a {@link DoubleVector} that that allows the backing data to be
 * resized and also viewed from an offset.  Furthermore, this class allows the
 * viewed data to be immutable, where all mutating operations throw {@link
 * UnsupportedOperationException}.
 *
 * </p>
 *
 * Note that the original {@code DoubleVector} may still be altered even if
 * this view is marked as immutable.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class DoubleVectorView extends VectorView<Double> implements DoubleVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link DoubleVector} reference to the backing vector
     */
    protected final DoubleVector doubleVector;
    
    /**
     * Creates a new {@link DoubleVector} view of the data in the provided
     * {@link DoubleVector}.
     *
     * @param v the {@code DoubleVector} to view as containing double data.
     */
    public DoubleVectorView(DoubleVector v) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} view of the data
     * in the provided {@link DoubleVector}.
     *
     * @param v the {@code DoubleVector} to view as containing double data.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector
     */
    public DoubleVectorView(DoubleVector v, boolean isImmutable) {
        this(v, 0, v.length(), isImmutable);
    }

    /**
     * Creates a new {@link DoubleVector} sub-view of the data in the provided
     * {@link DoubleVector} using the offset and length to specify a viewing
     * region.
     *
     * @param v the {@code Vector} to decorate.
     * @param offset the index at which values of {@code v} are stored in this
     *               {@code ViewIntegerAsDoubleVector}.
     * @param length the maximum length of this {@code
     *               ViewIntegerAsDoubleVector}.
     */
    public DoubleVectorView(DoubleVector v, int offset, int length) {
        this(v, offset, length, false);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} sub-view of the
     * data in the provided {@link DoubleVector} using the offset and length to
     * specify a viewing region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector.
     */
    public DoubleVectorView(DoubleVector v, int offset, int length, 
                                 boolean isImmutable) {
        super(v, offset, length, isImmutable);
        doubleVector = v;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        return doubleVector.add(getIndex(index), delta);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        doubleVector.set(getIndex(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return doubleVector.get(getIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return doubleVector.get(getIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            for (int i = vectorOffset; i < vectorOffset + vectorLength; ++i) {
                double d = doubleVector.get(i);
                m += d * d;
            }
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        if (vectorOffset > 0 || vectorLength != vector.length()) {
            double[] r = new double[vectorLength - vectorOffset];
            for (int i = vectorOffset; i < vectorLength; ++i)
                r[i] = doubleVector.get(i);
            return r;
        }
        else
            return doubleVector.toArray();
    }

    /**
     * Returns the original vector that this view is wrapping.  This is
     * primarily so that {@code Vectors.copyOf} can create a copy of the real
     * vector.
     */
    public DoubleVector getOriginalVector() {
        return doubleVector;
    }
}
