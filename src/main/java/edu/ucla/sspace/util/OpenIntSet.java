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

package edu.ucla.sspace.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A {@link Set} implementation specialized for storing {@code int} values with
 * efficient storagage and look-up.  Internally, this class uses <a
 * href="http://en.wikipedia.org/wiki/Open_addressing">open addressing</a> to
 * represent the set of {@code int} values in the set.  This provides amortized
 * O(1) access to the {@code get}, {@code set} and {@code remove} operations.
 * In particular, the {@code remove} operation is designed for supporting access
 * behaviors where elements are frequently added and removed.
 *
 * <p> This class overloads the common set operations with primitive accessors.
 * This enables callers to avoid autoboxing {@link Integer} values to their
 * primitive equivalents.
 *
 * <p> This class is not thread safe.
 *
 * @author David Jurgens
 */
public class OpenIntSet extends AbstractSet<Integer> 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The value in the buckets that indicates an empty position where a new
     * value may be stored
     */
    private static final int EMPTY_MARKER = 0; // default array value

    /**
     * The value in the buckets that incidates the existing value has been
     * deleted and a new value may be stored.  
     */
    private static final int DELETED_MARKER = Integer.MAX_VALUE;
    
    /**
     * The buckets that store the non-negative values in this set.
     */
    private int[] buckets;
    
    /**
     * True if a value equal to the {@code EMPTY_MARKER} has been stored in this
     * set.
     */
    private boolean isEmptyMarkerValuePresent;

    /**
     * True if a value equal to the {@code DELETED_MARKER} has been stored in
     * this set.
     */
    private boolean isDeletedMarkerValuePresent;

    /**
     * The number of elements in this set
     */
    private int size;

    /**
     * Constructs an {@code OpenIntSet} with the default size (4).
     */
    public OpenIntSet() {
        this(4);
    }
    
    /**
     * Constructs an {@code OpenIntSet} with storage for the specified number of
     * elements.
     *
     * @param size the number of expected elements to be contained within this
     *        set
     *
     * @throw IllegalArgumentException if {@code size} is not a positive value
     */
    public OpenIntSet(int size) {
        if (size < 0)
            throw new IllegalArgumentException("size must be non-negative");
        // find the next power of two greater than the size
        int n = 1;
        for (int i = 2; i < 32; ++i) {
            if ((n << i) >= size) {
                buckets = new int[n << i];
                break;
            }
        }
        isEmptyMarkerValuePresent = false;
        isDeletedMarkerValuePresent = false;
        size = 0;
    }

    /**
     * Constructs an {@code OpenIntSet} that will contain all the values in the
     * provided set.
     */
    public OpenIntSet(Collection<Integer> ints) {
        this(ints.size());
        addAll(ints);
    }

    /**
     * Constructs an {@code OpenIntSet} that will contain all the values in the
     * provided set.
     */
    public OpenIntSet(OpenIntSet ints) {
        // Copy over the integer values
        int len = ints.buckets.length;
        buckets = new int[len];
        System.arraycopy(ints.buckets, 0, buckets, 0, len);
        // Then set the non-array based state
        this.size = ints.size;
        this.isEmptyMarkerValuePresent = ints.isEmptyMarkerValuePresent;
        this.isDeletedMarkerValuePresent = ints.isDeletedMarkerValuePresent;
    }

    /**
     * Adds the integer value to this set.
     */
    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        // Special case for the value that indicates an empty value in the
        // backing table
        if (i == EMPTY_MARKER) {
            if (!isEmptyMarkerValuePresent) {
                isEmptyMarkerValuePresent = true;
                size++;
                return true;
            }
            return false;
        }
        // Special case for the value that indicates that a value has been
        // deleted in the backing table
        if (i == DELETED_MARKER) {
            if (!isDeletedMarkerValuePresent) {
                isDeletedMarkerValuePresent = true;
                size++;
                return true;
            }
            return false;
        }


        int bucket = findIndex(buckets, i);
        while (bucket == -1) {
            rebuildTable();
            bucket = findIndex(buckets, i);
        }

        int curVal = buckets[bucket];
        if (curVal == i) 
            return false;
        else {
            assert (curVal == EMPTY_MARKER || curVal == DELETED_MARKER)
                : "overwriting existing value";
            buckets[bucket] = i;
            size++;
            return true;
        }
    }

    /**
     * Returns the index where {@code i} would be in the provided array (which
     * include the case where an open slot exists) or {@code -1} if the probe
     * was unable to find both {@code i} and any position where {@code i} might
     * be stored.
     */
    private static int findIndex(int[] buckets, int i) {      
        // Hash the value of i into a slot.  Note that because we're using the
        // bitwise-and to perform the mod, the slot will always be positive even
        // if i is negative.
        int slot = i & (buckets.length - 1);
        int initial = slot;

        int firstDeletedSeen = -1;
        do {
            int val = buckets[slot];
            // Record the first DELETED slot we see, but don't return until
            // after we've progressed to either an emtpy slot or the slot with
            // the value.  This ensures that if we somehow delete a sequence of
            // values preceding the slot with the desired value the iteration
            // continues.  However, by recording this slot, if we don't find the
            // value, we can return this index to indicate whether the slot
            // would go.
            if (val == DELETED_MARKER && firstDeletedSeen < 0)
                firstDeletedSeen = slot;
            // If we found the value itself, then return the slot
            else if (val == i)
                return slot;  
            // Otherwise, if we found an EMPTY slot, then check whether the
            // value should be placed (potentially) in this slot or in a prior
            // slot that contains a deleted value.
            else if (val == EMPTY_MARKER) {
                return (firstDeletedSeen < 0) ? slot : firstDeletedSeen;
            }
        } while ((slot = (slot + 1) % buckets.length) != initial);

        // If the linear probe has wrapped all the way around the array and if
        // so, return a negative index.  This should only happen if the array is
        // completely full and cannot be grown.
        return -1;           
    }
    
    @Override public boolean contains(Object o) {
        if (o instanceof Integer)
            return contains(((Integer)o).intValue());
        else 
            throw new ClassCastException();
    }

    public boolean contains(int i) {
        // Special cases for the two marker values
        if (i == EMPTY_MARKER)
            return isEmptyMarkerValuePresent;
        else if (i == DELETED_MARKER)
            return isDeletedMarkerValuePresent;

        // Otherwise, find which bucket this value should be in and check if the
        // value is in that bucket.
        int bucket = findIndex(buckets, i);
        return bucket >= 0 && buckets[bucket] == i;
    }

    @Override public void clear() {
        Arrays.fill(buckets, 0);
        isEmptyMarkerValuePresent = false;
        isDeletedMarkerValuePresent = false;
        size = 0;
    }

    @Override public boolean isEmpty() {
        return size == 0;
    }

    public Iterator<Integer> iterator() {
        return new IntIterator();
    }

    private void rebuildTable() {
        int newSize = buckets.length << 1;
        int[] newBuckets = new int[newSize];
        // Rehash all of the existing elements
        for (int i : buckets) {
            // For all non-empty, non-deleted cells, find the new index for that
            // cell's value in the new table
            if (i != EMPTY_MARKER && i != DELETED_MARKER) {
                int index = findIndex(newBuckets, i);
                newBuckets[index] = i;
            }
        }
        buckets = newBuckets;
    }

    public boolean remove(Integer i) {
        return remove(i.intValue());
    }
    
    public boolean remove(int i) {
        boolean wasPresent = false;
        // Special cases for the two marker values
        if (i == EMPTY_MARKER) {
            wasPresent = isEmptyMarkerValuePresent;
            isEmptyMarkerValuePresent = false;
        }
        else if (i == DELETED_MARKER) {
            wasPresent = isDeletedMarkerValuePresent;
            isDeletedMarkerValuePresent = false;
        }
        else {
            // Find where i would be located in the table
            int bucket = findIndex(buckets, i);
            // If the bucket contained the value to be removed, then perform a
            // lazy delete and just mark its index as deleted.  This saves time
            // having to shift over all the elements in the table.
            if (bucket >= 0 && buckets[bucket] == i) {
                buckets[bucket] = DELETED_MARKER;
                wasPresent = true;
            }
        }
        
        // If the value to be removed was present the shrink the size of the set
        if (wasPresent)
            size--;
        return wasPresent;
    }

    public int size() {
        return size;
    }

    private class IntIterator implements Iterator<Integer> {
        
        int cur;
        int next;
        int nextIndex;
        boolean alreadyRemoved;

        boolean returnedEmptyMarker = false;
        boolean returnedDeletedMarker = false;


        public IntIterator() {
            nextIndex = -1;
            cur = -1;
            next = -1;
            alreadyRemoved = true; // causes NSEE if remove() called first
            returnedEmptyMarker = false;
            returnedDeletedMarker = false;
            advance();
        }

        private void advance() {
            if (!returnedEmptyMarker && isEmptyMarkerValuePresent) {
                next = EMPTY_MARKER;
            }
            else if (!returnedDeletedMarker && isDeletedMarkerValuePresent) {
                next = DELETED_MARKER;
            }
            else {
                int j = nextIndex + 1;
                while (j < buckets.length && 
                       (buckets[j] == EMPTY_MARKER
                        || buckets[j] == DELETED_MARKER)) {
                    ++j;
                }
                    
                nextIndex = (j == buckets.length) ? -1 : j;
                next = (nextIndex >= 0) ? buckets[nextIndex] : -1;
            }
        }
        
        public boolean hasNext() {
            return nextIndex >= 0
                || (isEmptyMarkerValuePresent && !returnedEmptyMarker)
                || (isDeletedMarkerValuePresent && !returnedDeletedMarker);
        }

        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            cur = next;
            if (next == EMPTY_MARKER) {
                returnedEmptyMarker = true;
            }
            else if (next == DELETED_MARKER) {
                returnedDeletedMarker = true;
            }
            advance();
        
            alreadyRemoved = false;
            return cur;
        }

        public void remove() {
            if (alreadyRemoved)
                throw new IllegalStateException();
            alreadyRemoved = true;
            OpenIntSet.this.remove(cur);
        }
    }

}