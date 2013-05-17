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

import edu.ucla.sspace.util.IntegerEntry;
import edu.ucla.sspace.util.ObjectEntry;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Iterator;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;


/**
 * A {@code SparseVector} implementation backed by a {@code HashMap}.  This
 * provides amoritized constant time access to all get and set operations, while
 * using more space than the {@link CompactSparseVector} or {@link
 * AmortizedSparseVector} classes.
 *
 * <p> See {@see SparseHashArray} for implementation details.
 *
 * @author David Jurgens
 */
public class SparseHashIntegerVector extends AbstractIntegerVector
         implements SparseIntegerVector, Iterable<IntegerEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    private final TIntIntMap map;
    
    private final int length;

    private double magnitude;

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public SparseHashIntegerVector(int length) {
        this.length = length;
        map = new TIntIntHashMap();
        magnitude = -1;
    }

    /**
     * Creates a new vector using the non-zero values of the specified array.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public SparseHashIntegerVector(int[] values) {
        this(values.length);
        for (int i = 0; i < values.length; ++i)
            if (values[i] != 0)
                map.put(i, values[i]);
        magnitude = -1;
    }

    /**
     * Creates a new vector with a copy of the date in {@code values}.  This
     * method is preferable than individually adding elements as it can
     * preallocate the space required for the data and avoid rehashing.
     */
    public SparseHashIntegerVector(IntegerVector values) {
        length = values.length();
        magnitude = -1; 
        if (values instanceof SparseHashIntegerVector) {
            SparseHashIntegerVector v = (SparseHashIntegerVector)values;
            map = new TIntIntHashMap(v.map);
        }
        else if (values instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) values).getNonZeroIndices();
            map = new TIntIntHashMap(nonZeros.length);
            for (int index : nonZeros)
                map.put(index, values.get(index));
        } else {
            map = new TIntIntHashMap();
            for (int index = 0; index < values.length(); ++index) {
                int value = values.get(index);
                if (value != 0)
                    map.put(index, value);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        int val = map.get(index);
        int newVal = val + delta;
        if (newVal == 0)
            map.remove(index);
        else
            map.put(index, newVal);
        magnitude = -1;
        return newVal;
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return map.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        int[] nz = map.keys();
        Arrays.sort(nz);
        return nz;
    }

    /**
     * Returns an iterator over the non-{@code 0} values in this vector.  This
     * method makes no guarantee about the order in which the indices are
     * returned.
     */
    public Iterator<IntegerEntry> iterator() {
        return new IntegerIterator();
    }
    
    /**
     * {@inheritDoc}
     */
    public int length() {
        return length;
    }    

    /**
     * {@inheritDoc} This value is cached and recomputed as needed when the
     * vector changes.
     */
    @Override public double magnitude() {
        if (magnitude < 0) {
            magnitude = 0;
            TIntIterator iter = map.valueCollection().iterator();
            while (iter.hasNext()) {
                int i = iter.next();
                magnitude += i*i;
            }
            magnitude = Math.sqrt(magnitude);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        int cur = map.get(index);
        if (value == 0) {
            if (cur != 0)
                map.remove(index);
        }
        else 
            map.put(index, value);
        magnitude = -1;
    }

    /**
     * An iterator over the {@code int} values in the vector, wrapping the
     * backing {@code SparseHashArray}'s own iterator.
     */
    class IntegerIterator implements Iterator<IntegerEntry> {

        TIntIntIterator iter;

        public IntegerIterator() {
            iter = map.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public IntegerEntry next() {
            iter.advance();
            return new IntegerEntry() {
                public int index() { return iter.key(); }
                public int value() { return iter.value(); }
            };
        }

        public void remove() {
            throw new UnsupportedOperationException(
                "Cannot remove from vector");
        }
    }
}
