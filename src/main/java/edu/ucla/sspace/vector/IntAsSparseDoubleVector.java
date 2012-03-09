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

import java.util.Arrays;

/**
 * A view of a sparse {@link IntegerVector} as a sparse {@link DoubleVector}.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
class IntAsSparseDoubleVector extends IntAsDoubleVector
        implements SparseDoubleVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link SparseVector} reference to the {@link IntegerVector} data
     * backing this view.
     */
    private final SparseVector sparseVector;

    /**
     * Creates a new {@link DoubleVector} view of the data in the provided
     * {@link IntegerVector}.
     *
     * @param v the {@code IntegerVector} to view as containing double data.
     */
    public <T extends IntegerVector & SparseVector<Integer>> 
            IntAsSparseDoubleVector(T v) {
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
    public <T extends IntegerVector & SparseVector<Integer>> 
            IntAsSparseDoubleVector(T v, boolean isImmutable) {
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
    public <T extends IntegerVector & SparseVector<Integer>> 
            IntAsSparseDoubleVector(T v, int offset, int length) {
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
    public <T extends IntegerVector & SparseVector<Integer>>
           IntAsSparseDoubleVector(T v, int offset, int length, 
                                   boolean isImmutable) {
        super(v, offset, length, isImmutable);
        sparseVector = v;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        if (vectorOffset == 0)
            return sparseVector.getNonZeroIndices();
        // If the sparse vector is a sub-view, calculate which indices are
        // reflected in this view
        else {
            int[] full = sparseVector.getNonZeroIndices();
            Arrays.sort(full);
            int startIndex = 0;
            int endIndex = full.length;
            for (int i = 0; i < full.length; ++i) {
                if (full[i] < vectorOffset)
                    startIndex++;
                else if (full[i] > vectorOffset + vectorLength) {
                    endIndex = i - 1;
                    break;
                }
            }
            if (startIndex == endIndex)
                return new int[0];
            int[] range = new int[endIndex - startIndex];
            System.arraycopy(full, startIndex, range, 0, range.length);
            return range;
        }
    }
}
