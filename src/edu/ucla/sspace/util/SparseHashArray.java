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

package edu.ucla.sspace.util;

import java.io.Serializable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * A sparse array backed by a {@link HashMap}.  This class offers amortized
 * constant time access to array indices at the expense of space.<p>
 *
 * Instance offer a space savings of retaining only the non-zero indices and
 * values.  For large array with only a few values set, this offers a huge
 * savings.  However, as the cardinality of the array grows in relation to its
 * size, a dense array will offer better performance in both space and time.
 * This is especially true if the sparse array instance approaches a cardinality
 * to size ratio of {@code .5}.<p>
 *
 * This class offers much better performance than {@link IntegerMap}, but will
 * use significantly more space as the cardinality increases.  In addition, this
 * class will marshall primitive types into their reified object forms.
 *
 * @see SparseArray
 * @see IntegerMap
 */
public class SparseHashArray<T> 
        implements SparseArray<T>, Iterable<ObjectEntry<T>>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The maximum length of this array
     */
    private final int maxLength;
  
    /**
     * A mapping from an array index to its value
     */
    private Map<Integer,T> indexToValue;

    /**
     * A memoized array of the non-zero indices that is only set after a call to
     * {@link #getElementIndices()} and is set back to {@code null} if any
     * mutating operation occur subsequently.
     */
    private int[] indices;

    /**
     * Creates a sparse array that grows to the maximum size set by {@link
     * Integer#MAX_VALUE}.
     */
    public SparseHashArray() {
	this(Integer.MAX_VALUE);
    }

    /**
     * Creates a sparse array with a fixed length
     */
    public SparseHashArray(int length) {
	if (length < 0)
	    throw new IllegalArgumentException("length must be non-negative");
	maxLength = length;
        indices = null;
        indexToValue = new HashMap<Integer,T>();
    }

    /**
     * Creates a sparse array copy of the provided array, retaining only the
     * non-zero values.  The length of the provided array is used to set the
     * maximum size of this sparse array.
     */
    public SparseHashArray(T[] array) {
	maxLength = array.length;
        indices = null;
	// Find how many non-zero elements there are
	int nonZero = 0;
	for (int i = 0; i < array.length; ++i) {
	    if (array[i] != null)
		indexToValue.put(i, array[i]);
	}
    }

    /**
     * {@inheritDoc}
     */
    public int cardinality() {
	return indexToValue.size();
    }

    /**
     * {@inheritDoc}
     */ 
    public T get(int index) {
	return indexToValue.get(index);
    }

    /**
     * Returns the indices of the array that contain non-{@code 0} values.
     *
     * @return the indices that contain values
     */
    public int[] getElementIndices() {
        if (indices != null)
            return indices;
        Integer[] objIndices = indexToValue.keySet().toArray(new Integer[0]);
        indices = new int[objIndices.length];
        for (int i = 0; i < objIndices.length; ++i)
            indices[i] = objIndices[i].intValue();
        // sort the indices
        Arrays.sort(indices);
        return indices;
    }
    
    /**
     * Returns an iterator over the non-{@code null} values in this array.  This
     * method makes no guarantee about the order in which the indices are
     * returned.
     */
    public Iterator<ObjectEntry<T>> iterator() {
        return new SparseHashArrayIterator();
    }   
     
    /**
     * Returns the maximum length of this array.
     */
    public int length() {
	return maxLength;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, T value) {
        // If we are setting a non-null value, then always add it to the array
        if (value != null) {
            // Check whether the put call actually modified the map, and if so
            // invalidate any previous set of indices;
            if (indexToValue.put(index, value) == null)
                indices = null;
        }
        // Otherwise, check whether an existing element was there and if so,
        // remove that index from the array, thereby maintaining sparseness.
        else if (indexToValue.remove(index) != null) {                
            // Since we removed something from the map, invalidate the
            // memoized indices
            indices = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <E> E[] toArray(E[] array) {
	for (int i = 0; i < array.length; ++i) {
            T j = indexToValue.get(i);
            if (j != null)
                array[i] = (E)j;
	}
	return array;
    }

    /**
     * A private iterator over the non-zero values of the array.  Note that this
     * iterator is <i>not</i> thread safe.
     */
    private class SparseHashArrayIterator implements Iterator<ObjectEntry<T>> {
        
        Iterator<Map.Entry<Integer,T>> arrayIter;
      
        public SparseHashArrayIterator() {
            arrayIter = indexToValue.entrySet().iterator();
        }

        public boolean hasNext() {
            return arrayIter.hasNext();
        }

        public ObjectEntry<T> next() {
            final Map.Entry<Integer,T> e = arrayIter.next();
            // Return a one-off instance of the entry
            return new ObjectEntry<T>() {
                public int index() { return e.getKey(); }
                public T value() { return e.getValue(); }
            };
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
}
