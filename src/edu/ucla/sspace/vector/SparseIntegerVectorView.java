/*
 * Copyright 2011 David Jurgens 
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

import java.io.Serializable;

import java.util.Arrays;


/**
 * A view of a sparse {@link IntegerVector} that that allows the backing data to
 * be resized and also viewed from an offset.  Furthermore, this class allows
 * the viewed data to be immutable, where all mutating operations throw {@link
 * UnsupportedOperationException}.
 * 
 * @authod David Jurgens
 */
class SparseIntegerVectorView extends IntegerVectorView
        implements SparseIntegerVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link SparseVector} reference to the {@link IntegerVector} data backing
     * this view.
     */
    private final SparseVector sparseVector;

    /**
     * Creates a new {@link IntegerVector} view of the data in the provided
     * {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     */
    public <T extends IntegerVector & SparseVector<Integer>> 
            SparseIntegerVectorView(T v) {
        this(v, 0, v.length(), false);
    }

    /**
     * Creates a new, optionally immutable {@link IntegerVector} view of the data
     * in the provided {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     * @param isImmutable {@code true} if this view should not allow mutating
     *        operations to change the state of the backing vector
     */
    public <T extends IntegerVector & SparseVector<Integer>> 
            SparseIntegerVectorView(T v, boolean isImmutable) {
        this(v, 0, v.length(), isImmutable);
    }

    /**
     * Creates a new {@link IntegerVector} sub-view of the data in the provided
     * {@link IntegerVector} using the offset and length to specify a viewing
     * region.
     *
     * @param v the {@code Vector} whose data is reflected in this view.
     * @param offset the index of {@code v} at which the first index of this
     *               view starts
     * @param length the length of this view.
     */
    public <T extends IntegerVector & SparseVector<Integer>> 
            SparseIntegerVectorView(T v, int offset, int length) {
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
    public <T extends IntegerVector & SparseVector<Integer>>
           SparseIntegerVectorView(T v, int offset, int length, 
                                   boolean isImmutable) {
        super(v, offset, length, isImmutable);
        sparseVector = v;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        if (vectorOffset == 0 && vectorLength == sparseVector.length())
            return sparseVector.getNonZeroIndices();
        // If the sparse vector is a sub-view, calculate which indices are
        // reflected in this view
        else {
            int inRange = 0;
            int[] indices = sparseVector.getNonZeroIndices();
            for (int nz : indices) {
                if (nz >= vectorOffset && nz < vectorOffset + vectorLength)
                    inRange++;
            }
            int[] arr = new int[inRange];
            int idx = 0;
            for (int nz : indices) {
                if (nz >= vectorOffset && nz < vectorOffset + vectorLength)
                    arr[idx++] = nz;
            }
            return arr;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked") @Override 
    public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            double m = 0;
            // Special case if we can iterate in time linear to the number of
            // non-zero values
            if (sparseVector instanceof Iterable) {
                for (IntegerEntry e : (Iterable<IntegerEntry>)sparseVector) {
                    int idx = e.index();
                    if (idx >= vectorOffset 
                            && idx < vectorOffset + vectorLength) {
                        int i = e.value();
                        m += i * i;
                    }
                }
            }
            else {
                for (int nz : sparseVector.getNonZeroIndices()) {
                    if (nz >= vectorOffset 
                            && nz < vectorOffset + vectorLength) {
                        int j = intVector.get(nz);
                        m += j * j;
                    }
                }
            }
            magnitude = Math.sqrt(m);
        }
        return magnitude;
    }
}
