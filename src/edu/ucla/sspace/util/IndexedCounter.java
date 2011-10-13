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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;


/** 
 * A utility for counting unique instance of an object.  Unlike an {@link
 * ObjectCounter}, this class uses an {@link Indexer} to identify object
 * equivalence and record the count.  This class is intended for use when
 * multiple {@link Counter} instances are needed to be run in parallel or to
 * keep a record of the counters without storing references to the object
 * instances in each of the {@code Counter} instances.  This results in a linear
 * space savings at the cost of some minor performance from needing to index the
 * objects.
 *
 * <p> This class supports iterating over the set of instances being counted as
 * well as the instances and counts together.  All collections views that are
 * returned are unmodifiable and will throw an exception if a mutating method is
 * called.
 *
 * <p> This class is <i>not</i> thread-safe
 *
 * @param T the type of object being counted.
 *
 * @see Indexer
 */
public class IndexedCounter<T> implements Counter<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from an object to the number of times it has been seen
     */
    private final Indexer<T> objectIndices;

    /**
     * The mapping from object indices to their counts
     */
    private final TIntIntMap indexToCount;

    /**
     * A flag for whether to add objects that aren't mapped to indices to the
     * {@code objectIndices} Indexer.
     */
    private final boolean allowNewIndices;
        
    /**
     * The total number of objects that have been counted
     */
    private int sum;

    /**
     * Creates an empty {@code Counter} that uses the provided indexer to map
     * countable values to corresponding indices.  Should an object be counted
     * that was not in the indexer, a new index will be added for it.
     */
    public IndexedCounter(Indexer<T> objectIndices) {
        this(objectIndices, true);
    }

    /**
     * Creates an empty {@code Counter} that uses the provided indexer to map
     * countable values to corresponding indices.  Objects wth no corresponding
     * indices will only be counted (and added to the Indexer) if {@code
     * allowNewIndices} is true.
     *
     * @param allowNewIndices {@code true} if objects with new index should be
     *        added to {@code objectIndices}
     */
    public IndexedCounter(Indexer<T> objectIndices, boolean allowNewIndices) {
        this.objectIndices = objectIndices;
        this.allowNewIndices = allowNewIndices;
        indexToCount = new TIntIntHashMap();
        sum = 0;
    }

    /**
     * Adds the counts from the provided {@code Counter} to the current counts,
     * adding new elements as needed.
     */
    public void add(Counter<? extends T> counter) {
        for (Map.Entry<? extends T,Integer> e : counter)
            count(e.getKey(), e.getValue());
    }
    
    /**
     * Counts the object, increasing its total count by 1.
     */
    public int count(T obj) {
        int objIndex = (allowNewIndices) 
            ? objectIndices.index(obj)
            : objectIndices.find(obj);
        if (objIndex < 0)
            return 0;
        int curCount = indexToCount.get(objIndex);
        indexToCount.put(objIndex, curCount + 1);
        sum++;
        return curCount + 1;
    }

    /**
     * Counts the object, increasing its total count by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(T obj, int count) {
        if (count < 1)
            throw new IllegalArgumentException("Count must be positive: " + count);
        int objIndex = (allowNewIndices) 
            ? objectIndices.index(obj)
            : objectIndices.find(obj);
        if (objIndex < 0)
            return 0;
        int curCount = indexToCount.get(objIndex);
        indexToCount.put(objIndex, curCount + count);
        sum += count;
        return curCount + count;
    }

    /**
     * {@inheritDoc}
     */
    public void countAll(Collection<? extends T> c) {
        for (T t : c)
            count(t);
    }


    public boolean equals(Object o) {
        throw new Error();
    }

    /**
     * Returns the number of times the specified object has been seen by this
     * counter.
     */
    public int getCount(T obj) {
        int objIndex = (allowNewIndices) 
            ? objectIndices.index(obj)
            : objectIndices.find(obj);
        return (objIndex < 0) ? 0 : indexToCount.get(objIndex);
    }

    /**
     * Returns the frequency of this object relative to the counts of all other
     * objects.  This value may also be interpreted as the probability of the
     * instance of {@code obj} being seen in the items that have been counted.
     */
    public double getFrequency(T obj) {
        double count = getCount(obj);
        return (sum == 0) ? 0 : count / sum;
    }

    public int hashCode() {
        throw new Error();
    }

    /**
     * Returns a view of the items currently being counted.  The returned view
     * is read-only; any attempts to modify this view will throw an {@link
     * UnsupportedOperationException}.
     */
    public Set<T> items() {
        return new ItemSet();
    }

    /**
     * Returns an interator over the elements that have been counted thusfar and
     * their respective counts.
     */
    public Iterator<Map.Entry<T,Integer>> iterator() {
        return new EntryIter();
    }
    
    /**
     * Returns the element that currently has the largest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public T max() {
        if (sum == 0)
            return null;
        int maxCount = -1;
        int maxIndex = -1;
        for (TIntIntIterator it = indexToCount.iterator(); it.hasNext(); ) {
            it.advance();
            if (it.value() > maxCount) {
                maxIndex = it.key();
                maxCount = it.value();
            }
        }
        return objectIndices.lookup(maxIndex);
    }

    /**
     * Returns the element that currently has the smallest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public T min() {
        if (sum == 0)
            return null;
        int minCount = Integer.MAX_VALUE;
        int minIndex = -1;
        for (TIntIntIterator it = indexToCount.iterator(); it.hasNext(); ) {
            it.advance();
            if (it.value() < minCount) {
                minIndex = it.key();
                minCount = it.value();
            }
        }
        return objectIndices.lookup(minIndex);
    }

    /**
     * {@inheritDoc}  Note that this does not affect the object indices.
     */
    public void reset() {
        indexToCount.clear();
        sum = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return indexToCount.size();
    }

    /**
     * {@inheritDoc}
     */
    public int sum() {
        return sum;
    }

    @Override public String toString() {
        return indexToCount.toString();
    }

    private class ItemSet extends AbstractSet<T> {

        @Override public boolean contains(Object o) {
            @SuppressWarnings("unchecked")
            T t = (T)o;
            int idx = objectIndices.find(t);
            return indexToCount.get(idx) > 0;
        }

        @Override public boolean isEmpty() {
            return IndexedCounter.this.size() == 0;
        }

        @Override public Iterator<T> iterator() {
            return new ItemIter();
        }
        
        @Override public int size() {
            return IndexedCounter.this.size();
        }
    }

    private class ItemIter implements Iterator<T> {

        private final Iterator<Map.Entry<T,Integer>> itemToIndex;
        
        private T next;

        public ItemIter() {
            itemToIndex = objectIndices.iterator();
            next = null;
            advance();
        }
        
        private void advance() {
            while (next == null && itemToIndex.hasNext()) {
                Map.Entry<T,Integer> e = itemToIndex.next();
                int count = indexToCount.get(e.getValue());
                if (count > 0)
                    next = e.getKey();
            }
        }
     
        public boolean hasNext() {
            return next != null;
        }
        
        public T next() {
            if (next == null)
                throw new NoSuchElementException();
            T t = next;
            advance();
            return t;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    private class EntryIter implements Iterator<Map.Entry<T,Integer>> {

        private final Iterator<Map.Entry<T,Integer>> itemToIndex;
        
        private Map.Entry<T,Integer> next;

        public EntryIter() {
            itemToIndex = objectIndices.iterator();
            next = null;
            advance();
        }
        
        private void advance() {
            next = null;
            while (next == null && itemToIndex.hasNext()) {
                Map.Entry<T,Integer> e = itemToIndex.next();
                int count = indexToCount.get(e.getValue());
                if (count > 0)
                    next = new AbstractMap.SimpleImmutableEntry<T,Integer>(
                        e.getKey(), count);
            }
        }
     
        public boolean hasNext() {
            return next != null;
        }
        
        public Map.Entry<T,Integer> next() {
            if (next == null)
                throw new NoSuchElementException();
            Map.Entry<T,Integer> e = next;
            advance();
            return e;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}