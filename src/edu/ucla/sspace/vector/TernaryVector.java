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

import java.util.Arrays;


/**
 * An unmodifiable vector with ternary (+1, 0, -1) values.  This class is
 * intended to be lightweight and avoids copying arrays for all operations.
 * Therefore, any changes to such arrays will be reflected in this vector.  Any
 * mutating methods will throw an {@link UnsupportedOperationException} if
 * called.
 *
 * @author Keith Stevens
 * @author David Jurgens
 */
public class TernaryVector 
    implements SparseVector<Integer>, IntegerVector, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The indices which are all set to +1.
     */
    protected int[] positiveDimensions;

    /**
     * The indices which are all set to -1.
     */
    protected int[] negativeDimensions;

    /**
     * The length of the created {@code FixedTernaryVector}.
     */
    private int length;

    /**
     * Create an {@code TernaryVector} with the specified length, and postive
     * and negative dimensions.  Note that the provided arrays are <i>not</i>
     * safe.  This vector retains references to the arrays and so any
     * modification to their contents will result in a change to this vector.
     *
     * @param length the number of elements in this vector.
     * @param positiveIndices all indices whose values are +1.
     * @param negativeIndices all indices whose values are -1.
     */
    public TernaryVector(int length,
                         int[] positiveIndices,
                         int[] negativeIndices) {
        this.length = length;
        positiveDimensions = positiveIndices;
        negativeDimensions = negativeIndices;
        // Sort the dimensions to ensure that the get() operations work
        Arrays.sort(positiveDimensions);
        Arrays.sort(negativeDimensions);        
    }

    /**
     * Throws {@link UnsupportedOperationException} if called.
     */
    public int add(int index, int delta) {
        throw new UnsupportedOperationException(
                "TernaryVector instances cannot be modified");        
    }

    /**
     * {@inheritDoc}.
     */
    public int get(int index) {
        if (index < 0 || index > length)
            throw new IndexOutOfBoundsException(
                "index not within vector: " + index);
        if (Arrays.binarySearch(positiveDimensions, index) >= 0)
            return 1;
        if (Arrays.binarySearch(negativeDimensions, index) >= 0)
            return -1;
        return 0;
    }

    /**
     * {@inheritDoc} Note that this method is <i>not</i> constant time; this
     * method runs in time proportional the the number of non-zero indices.
     */
    public int[] getNonZeroIndices() {
        int[] nz = new int[negativeDimensions.length 
                           + positiveDimensions.length];
        System.arraycopy(negativeDimensions, 0, nz, 
                         0, negativeDimensions.length);
        System.arraycopy(positiveDimensions, 0, nz, 
                         negativeDimensions.length, positiveDimensions.length);
        return nz;
    }

    /**
     * {@inheritDoc}.
     */
    public Integer getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}.
     */
    public int length() {
        return length;
    }

    /**
     * Returns the indices at which this vector is valued {@code -1} in sorted
     * order.  Note that the return array is <i>not</i> safe.  Any modification
     * to its contents will result in a change to this vector.
     *
     * @return An array of indices which have negative values.
     */
    public int[] negativeDimensions() {
        return negativeDimensions;
    }
    
    /**
     * Returns the indices at which this vector is valued {@code +1} in sorted
     * order.  Note that the return array is <i>not</i> safe.  Any modification
     * to its contents will result in a change to this vector.
     *
     * @return An array of indices which have positive values.
     */
    public int[] positiveDimensions() {
        return positiveDimensions;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        return Math.sqrt(positiveDimensions.length + negativeDimensions.length);
    }

    /**
     * Throws {@link UnsupportedOperationException} if called.
     */
    public void set(int index, int value) {
        throw new UnsupportedOperationException(
                "TernaryVector instances cannot be modified");        
    }

    /**
     * Throws {@link UnsupportedOperationException} if called.
     */
    public void set(int index, Number value) {
        throw new UnsupportedOperationException(
                "TernaryVector instances cannot be modified");        
    }
    
    /**
     * {@inheritDoc}.
     */
    public int[] toArray() {
        int[] array = new int[length];
        for (int p : positiveDimensions)
            array[p] = 1;
        for (int n : negativeDimensions)
            array[n] = -1;
        return array;
    }
}
