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

import java.io.Serializable;

import java.util.Map;


/**
 * A decorator that masked view of a {@link Vector} through the use of a mapping
 * from new column indices to original column indices.  The size of the new
 * vector is based on the number of valid mappings.
 *
 * @author Keith Stevens
 */
public class MaskedDoubleVectorView extends VectorView<Double> 
                                    implements DoubleVector {

    private static final long serialVersionUID = 1L;

    /**
     * A {@link DoubleVector} reference to the backing vector
     */
    private final DoubleVector doubleVector;

    /**
     * The mapping from new indices to old indices.
     */
    private final int[] columnMask;

    /**
     * Creates a new {@link DoubleVector} view of the data in the provided
     * {@link DoubleVector}.
     *
     * @param v the {@code DoubleVector} to view as containing double data.
     * @param columnMask A mapping from new indices to old indices.
     */
    public MaskedDoubleVectorView(DoubleVector v,
                                  int[] columnMask) {
        super(v, 0, columnMask.length, false);
        this.doubleVector = v;
        this.columnMask = columnMask;
    }

    /**
     * Returns the new index for a given column, or -1 if the column is not
     * mapped.
     */
    protected int getIndex(int index) {
        if (index < 0 || index > columnMask.length)
            throw new IllegalArgumentException("The given index is not " +
                    "within the bounds of the masked vector");
        return columnMask[index];
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        int newIndex = getIndex(index);
        return (newIndex == -1) ? 0 : doubleVector.add(newIndex, delta);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        int newIndex = getIndex(index);
        if (newIndex == -1)
            return;
        doubleVector.set(newIndex, value);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        int newIndex = getIndex(index);
        return (newIndex == -1) ? 0 : doubleVector.get(newIndex);
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
        double[] r = new double[length()];
        for (int i = 0; i < length(); ++i)
            r[i] = get(i);
        return r;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return columnMask.length;
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
