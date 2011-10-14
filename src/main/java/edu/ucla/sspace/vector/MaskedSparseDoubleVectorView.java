/*
 * Copyright 2010 Keith Stevens 
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

import edu.ucla.sspace.util.BiMap;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Map;


/**
 * A decorator that masked view of a {@link SparseVector} through the use of a
 * mapping from new column indices to original column indices.  The size of the
 * new vector is based on the number of valid mappings.
 *
 * @author Keith Stevens
 */
public class MaskedSparseDoubleVectorView extends MaskedDoubleVectorView
                                          implements SparseDoubleVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link SparseVector} reference to the {@link DoubleVector} data backing
     * this view.
     */
    private final SparseVector sparseVector;

    /**
     * The mapping from new indices to old indices.
     */
    private final Map<Integer, Integer> reverseColumnMask;

    /**
     * Creates a new {@link SparseDoubleVector} view of the data in the provided
     * {@link SparseDoubleVector}.
     *
     * @param v the {@code DoubleVector} to view as containing double data.
     * @param columnMask A mapping from new indices to old indices.
     */
    public <T extends DoubleVector & SparseVector<Double>> 
            MaskedSparseDoubleVectorView(T v, 
                                         int[] columnMask,
                                         Map<Integer, Integer> reverseMask) {
        super(v, columnMask);
        sparseVector = v;
        this.reverseColumnMask = reverseMask; 
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        int[] indices = sparseVector.getNonZeroIndices();
        int[] newIndices = new int[indices.length];
        int i = 0;
        for (int index = 0; index < indices.length; ++index) {
            Integer newIndex = reverseColumnMask.get(indices[index]);
            if (newIndex == null)
                continue;
            newIndices[i++] = newIndex;
        }
        return Arrays.copyOf(newIndices, i);
    }
}
