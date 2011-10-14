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
 * An decorator for a {@code Vector} that lets the backing data be viewed as a
 * {@link DoubleVector}.  In addition, the backing vector may be resized and
 * also viewed from an offset.  Furthermore, this class allows the viewed data
 * to be immutable, where all mutating operations throw {@link
 * UnsupportedOperationException}.  
 *
 * </p>
 *
 * The data-type of the backing vectors values is still preserved if mututaions
 * are allowed.  This class relies upon the {@link Vector#set(Number)
 * set(Number)} method of the backing vector to correctly convert a {@code
 * double} argument into the appropriate.  Note that in the event that the
 * backing data-type does not support {@code double} precision, this may result
 * in data loss.
 *
 * </p>
 *
 * Note that the original {@code Vector} may still be alterned even if this view
 * is marked as immutable. 
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class ViewVectorAsDoubleVector extends VectorView<Double> implements DoubleVector {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new {@link DoubleVector} view of the data in the provided
     * {@link Vector}.
     *
     * @param v the {@code Vector} to view as containing double data.
     */
    public ViewVectorAsDoubleVector(Vector v) {
        super(v);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} view of the data
     * in the provided {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector
     */
    public ViewVectorAsDoubleVector(Vector v, boolean isImmutable) {
        super(v, isImmutable);
    }

    /**
     * Creates a new {@link DoubleVector} sub-view of the data in the provided
     * {@link Vector} using the offset and length to specify a viewing
     * region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     */
    public ViewVectorAsDoubleVector(Vector v, int offset, int length) {
        super(v, offset, length);
    }

    /**
     * Creates a new, optionally immutable {@link DoubleVector} sub-view of the
     * data in the provided {@link Vector} using the offset and length to
     * specify a viewing region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector.
     */
    public ViewVectorAsDoubleVector(Vector v, int offset, int length,
                                    boolean isImmutable) {
        super(v, offset, length, isImmutable);
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        int mapped = getIndex(index);
        double value = vector.getValue(mapped).doubleValue();
        vector.set(mapped, value + delta);
        return value + delta;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");
        vector.set(getIndex(index), value);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.getValue(getIndex(index)).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return vector.getValue(getIndex(index)).doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] r = new double[vectorLength - vectorOffset];
        for (int i = vectorOffset; i < vectorLength; ++i)
            r[i] = vector.getValue(i).doubleValue();
        return r;
    }
}
