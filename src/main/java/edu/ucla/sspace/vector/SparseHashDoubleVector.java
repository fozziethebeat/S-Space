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

import edu.ucla.sspace.util.DoubleEntry;
import edu.ucla.sspace.util.ObjectEntry;

import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Iterator;


/**
 * A {@code SparseVector} implementation backed by a {@code Map}.  This
 * provides amoritized constant time access to all get and set operations, while
 * using more space than the {@link CompactSparseVector} or {@link
 * AmortizedSparseVector} classes.
 *
 * <p> See {@see SparseHashArray} for implementation details.
 *
 * @author David Jurgens
 */
public class SparseHashDoubleVector 
        extends AbstractDoubleVector
        implements SparseDoubleVector, Serializable { //Iterable<DoubleEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private final TIntDoubleHashMap vector;

    private int maxLength;

    private double magnitude;

    private int[] nonZeroIndices;

    /**
     * Creates a new {@link SparseHashDoubleVector} with an unbounded length.
     */
    public SparseHashDoubleVector() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public SparseHashDoubleVector(int length) {
        maxLength = length;
        vector = new TIntDoubleHashMap();
    }

    /**
     * Creates a new vector using the non-zero values of the specified array.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public SparseHashDoubleVector(double[] values) {
        maxLength = values.length;
        vector = new TIntDoubleHashMap();
        nonZeroIndices = null;
        magnitude = -1;
        for (int i = 0; i < values.length; ++i) {
            if (values[i] != 0) {
                vector.put(i, values[i]);
            }
        }
    }

    /**
     * Creates a new vector with a copy of the date in {@code values}.  This
     * method is preferable than individually adding elements as it can
     * preallocate the space required for the data and avoid rehashing.
     */
    public SparseHashDoubleVector(DoubleVector values) {
        maxLength = values.length();
        nonZeroIndices = null;
        magnitude = -1; 
        if (values instanceof SparseHashDoubleVector) {
            SparseHashDoubleVector v = (SparseHashDoubleVector)values;
            vector = new TIntDoubleHashMap(v.vector);
        }
        else if (values instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) values).getNonZeroIndices();
            vector = new TIntDoubleHashMap(nonZeros.length);
            for (int index : nonZeros)
                vector.put(index, values.get(index));
        } else {
            vector = new TIntDoubleHashMap();
            for (int index = 0; index < values.length(); ++index) {
                double value = values.get(index);
                if (value != 0d)
                    vector.put(index, value);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        double val = vector.get(index) + delta;
        if (val == 0) 
            vector.remove(index);        
        else
            set(index, val);
        nonZeroIndices = null;
        magnitude = -1;
        return val;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * Returns an iterator over the non-{@code 0} values in this vector.  This
     * method makes no guarantee about the order in which the indices are
     * returned.
    public Iterator<DoubleEntry> iterator() {
        return new DoubleIterator();
    }
     */

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        double old = vector.get(index);
        if (value == 0)
            vector.remove(index);
        else 
            vector.put(index, value);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[length()];
        for (int i : vector.keys())
            array[i] = vector.get(i);
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return maxLength;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        if (magnitude < 0) {
            magnitude = 0;
            TDoubleIterator iter = vector.valueCollection().iterator();
            while (iter.hasNext()) {
                double d = iter.next();
                magnitude += d*d;
            }
            magnitude = Math.sqrt(magnitude);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        if (nonZeroIndices == null) {
            nonZeroIndices = vector.keys();
            Arrays.sort(nonZeroIndices);
        }
        return nonZeroIndices;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector instanceCopy() {
        return new SparseHashDoubleVector(maxLength);
    }

    /**
     * An iterator over the {@code double} values in the vector, wrapping the
     * backing {@code SparseHashArray}'s own iterator.
     */
    // class DoubleIterator implements Iterator<DoubleEntry> {

    //     Iterator<ObjectEntry<Number>> it;

    //     public DoubleIterator() {
    //         it = vector.iterator();
    //     }

    //     public boolean hasNext() {
    //         return it.hasNext();
    //     }

    //     public DoubleEntry next() {
    //         final ObjectEntry<Number> e = it.next();
    //         return new DoubleEntry() {
    //             public int index() { return e.index(); }
    //             public double value() { return e.value().doubleValue(); }
    //         };
    //     }

    //     public void remove() {
    //         throw new UnsupportedOperationException(
    //             "Cannot remove from vector");
    //     }
    // }
}
