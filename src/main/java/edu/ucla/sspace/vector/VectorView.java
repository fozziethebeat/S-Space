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
 * An decorator for a {@code Vector} that allows the backing data to be resized
 * and also viewed from an offset.  Furthermore, this class allows the viewed
 * data to be immutable, where all mutating operations throw {@link
 * UnsupportedOperationException}.
 *
 * </p>
 *
 * Note that the original {@code Vector} may still be altered even if this view
 * is marked as immutable.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class VectorView<T extends Number> implements Vector<T>, Serializable  {

    private static final long serialVersionUID = 1L;
    
    /**
     * Whether the vector data backing this instance is immutable
     */
    protected final boolean isImmutable;

    /**
     * The actual vector this {@code ViewDoubleAsDoubleVector} is decorating.
     */
    protected final Vector vector;

    /**
     * A fixed length for this {@code Vector}.  This length may be longer or
     * less than that of {@code vector}.
     */
    protected final int vectorLength;

    /**
     * The index at which the values {@code vector} are stored.
     */
    protected final int vectorOffset;

    /**
     * The magnitude of the vector or -1 if the value is currently invalid needs
     * to be recomputed
     */
    protected double magnitude;

    /**
     * Creates a new mutable view of the {@code Vector}
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     */
    public VectorView(Vector v) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a optionally immutable view of the {@code Vector}
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector.
     */
    public VectorView(Vector v, boolean isImmutable) {
        this(v, 0, v.length(), isImmutable);
    }

    /**
     * Creates a new view of the provided {@code Vector} using the offset and
     * length to specify a viewing region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code offset} is greater than or equal to the length
     *         of {@code v}<li>the sum of {@code offset} plus {@code length} is
     *         greater than the length of {@code v}</ul>
     */
    public VectorView(Vector v, int offset, int length) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a new, optionally immutable view of the provided {@code Vector}
     * using the offset and length to specify a viewing region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector.
     *
     * @throws IllegalArgumentException if <ul><li>{@code offset} is
     *         negative<li>{@code length} is less than zero<li>the sum of {@code
     *         offset} plus {@code length} is greater than the length of {@code
     *         v}</ul>
     */
    public VectorView(Vector v, int offset, int length, boolean isImmutable) {
        if (v == null) {
            throw new NullPointerException(
                "Cannot create a view of a null vector");
        }
        vector = v;
        vectorOffset = offset;
        vectorLength = length;
        this.isImmutable = isImmutable;
        // REMINDER: if this is going to stay a package-private class, it might
        // be more prudent to convert these to asserts since we would be
        // guaranteeing that the conditions would never occur
        if (length < 0)
            throw new IllegalArgumentException("Cannot have negative length");
        if (offset < 0)
            throw new IllegalArgumentException("Offset cannot be negative");
        if (offset + length > v.length())
            throw new IllegalArgumentException(
                "Cannot create view larger than vector");
        magnitude = -1;
    }

    /**
     * Throws {@code UnsupportedOperationException} if called
     */
    public void set(int index, Number value) {
        if (isImmutable) 
            throw new UnsupportedOperationException(
                "Cannot modify an immutable vector");           
        vector.set(getIndex(index), value);
        magnitude = -1;
    }

    /**
     * Returns the possibly shifted index for the requested index.
     */
    protected int getIndex(int index) {
        if (index < 0 || index > vectorLength)
            throw new IllegalArgumentException("Invalid index: " + index);

        return index + vectorOffset;
    }

    /**
     * {@inheritDoc}
     */
    public Number getValue(int index) {
        return vector.getValue(getIndex(index));
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vectorLength;
    }


    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            for (int i = vectorOffset; i < vectorOffset + vectorLength; ++i) {
                Number j = vector.getValue(i);
                m += j.doubleValue() * j.doubleValue();
            }
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }

    public String toString() {
        return vector.toString();
    }
}

