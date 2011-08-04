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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * An implementation of a sparse vector based on the Yale Sparse matrix format.
 * This class offers better access for modification efficiency over {@link
 * SparseVector} at the expense of increased memory usage, for certain usage
 * patterns. Only non-zero values in the vector are stored in an {@code
 * ArrayList}, which tracks both the index of the value, and the value itself.
 * Lookups for all indices are O(log n).  Writes to already existing indices are
 * O(log n).  Writes to non-existing indices are dependent on the insertion time
 * of a {@code ArrayList}, but are at a minimum O(log n).
 *
 * @author Keith Stevens
 */
public class AmortizedSparseVector implements SparseDoubleVector,
                                              Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * An arraylist of non zero values for this row, stored in the correct
     * delta order.
     */
    private List<IndexValue> values;

    /**
     * The comparator for a {@code IndexValue}.
     */
    private CellComparator comp;

    /**
     * The maximum length this vector can take on.
     */
    private int maxLength;

    /**
     * The maximum known length of this vector.
     */
    private int knownLength;

    /**
     * An {@code AmortizedSparseVector} with {@link Integer#MAX_VALUE}
     * dimensions.
     */
    public AmortizedSparseVector() {
        this(Integer.MAX_VALUE);
        knownLength = 0;
    }

    /**
     * Create {@code AmortizedSparseVector} of size @{code length}, which
     * initially has all dimensions set to 0.
     *
     * @param length The maximum length of the {@code Vector}.
     */
    public AmortizedSparseVector(int length) {
        knownLength = length;
        maxLength = length;
        values = new ArrayList<IndexValue>();
        comp = new CellComparator();
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        checkIndex(index);

        double value = get(index) + delta;
        set(index, value);
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        checkIndex(index);

        IndexValue item = new IndexValue(index, 0);
        int valueIndex = Collections.binarySearch(values, item, comp);
        return (valueIndex >= 0) ? values.get(valueIndex).value : 0.0;
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
    public double magnitude() {
        double m = 0;
        for (IndexValue v : values) 
            m += v.value * v.value;
        return Math.sqrt(m);
    }
    
    /**
     * {@inheritDoc}
     *
     * All read operations, and writes to indices which already exist are of
     * time O(log n).  Writers to new indices are of time O(log n).
     */
    public void set(int index, double value) {
        checkIndex(index);

        IndexValue item = new IndexValue(index, 0);
        int valueIndex = Collections.binarySearch(values, item, comp);
        if (valueIndex >= 0 && value != 0d) {
            // Replace a currently existing item with a non zero value.
            values.get(valueIndex).value = value;
        } else if (value != 0d) {
            // Add a new cell item into this row.
            item.value = value;
            values.add((valueIndex + 1) * -1, item);
        } else if (valueIndex >= 0) {
            // Remove the value since it's now zero.
            values.remove(valueIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     *
     * Note that any values which are 0 are left out of the vector.
     */
    public void set(double[] value) {
        checkIndex(value.length);

        for (int i = 0; i < value.length; ++i) {
            if (value[i] != 0d)
                set(i, value[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] dense = new double[length()];
        for (IndexValue item : values) {
            dense[item.index] = item.value;
        }
        return dense;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        int[] indices = new int[values.size()];
        for (int i = 0; i < values.size(); ++i)
            indices[i] = values.get(i).index;
        return indices;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return knownLength;
    }

    /**
     * If {@code length} is longer than the currently known reset the value of
     * {@code knownLength}.
     */
    private void checkIndex(int length) {
        if (maxLength == Integer.MAX_VALUE && knownLength < length)
            knownLength = length;
        else if (length < 0 || length >= maxLength)
            throw new IllegalArgumentException("Length must be non negative " +
                    "and less than the maximum length");
    }

    /**
     * A small struct to hold the index and value of an entry.  This should
     * offset the object creation costs from storing Integer and Double's in the
     * array lists.
     */
    private static class IndexValue {
        public int index;
        public double value;

        public IndexValue(int index, double value) {
            this.index = index;
            this.value = value;
        }
    }

    /**
     * Comparator class for IndexValues.  A IndexValue is ordered based on it's
     * index value.
     */
    private static class CellComparator implements Comparator<IndexValue>,
                                                   Serializable {

        private static final long serialVersionUID = 1;

        public int compare(IndexValue item1, IndexValue item2) {
            return item1.index - item2.index;
        }

        public boolean equals(Object o) {
            return this == o;
        }
    }
}
