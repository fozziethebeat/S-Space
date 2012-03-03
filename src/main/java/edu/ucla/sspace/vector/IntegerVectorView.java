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


/**
 * A view of a {@link IntegerVector} that that allows the backing data to be
 * resized and also viewed from an offset.  Furthermore, this class allows the
 * viewed data to be immutable, where all mutating operations throw {@link
 * UnsupportedOperationException}.
 *
 * </p>
 *
 * Note that the original {@code IntegerVector} may still be altered even if
 * this view is marked as immutable.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class IntegerVectorView extends VectorView<Integer> implements IntegerVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link IntegerVector} reference to the backing vector
     */
    protected final IntegerVector intVector;

    /**
     * Creates a new {@link IntegerVector} view of the data in the provided
     * {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     */
    public IntegerVectorView(IntegerVector v) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a new, optionally immutable {@link IntegerVector} view of the
     * data in the provided {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector
     */
    public IntegerVectorView(IntegerVector v, boolean isImmutable) {
        this(v, 0, v.length(), isImmutable);
    }

    /**
     * Creates a new {@link IntegerVector} sub-view of the data in the provided
     * {@link IntegerVector} using the offset and length to specify a viewing
     * region.
     *
     * @param v the {@code Vector} to decorate.
     * @param offset the index at which values of {@code v} are stored in this
     *               {@code ViewIntegerAsIntegerVector}.
     * @param length the maximum length of this {@code
     *               ViewIntegerAsIntegerVector}.
     */
    public IntegerVectorView(IntegerVector v, int offset, int length) {
        this(v, offset, length, false);
    }

    /**
     * Creates a new, optionally immutable {@link IntegerVector} sub-view of the
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
    public IntegerVectorView(IntegerVector v, int offset, int length, 
                                 boolean isImmutable) {
        super(v, offset, length, isImmutable);
        intVector = v;
    }

    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        return intVector.add(getIndex(index), delta);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        intVector.set(getIndex(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return intVector.get(getIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    public Integer getValue(int index) {
        return intVector.get(getIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    @Override public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            for (int i = vectorOffset; i < vectorOffset + vectorLength; ++i) {
                int j = intVector.get(i);
                m += j * j;
            }
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray() {
        if (vectorOffset > 0 || vectorLength != vector.length()) {
            int[] r = new int[vectorLength - vectorOffset];
            for (int i = vectorOffset; i < vectorLength; ++i)
                r[i] = intVector.get(i);
            return r;
        }
        else
            return intVector.toArray();
    }
}
