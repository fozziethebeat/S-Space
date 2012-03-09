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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import gnu.trove.iterator.TLongIntIterator;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;


/** 
 * A utility for counting unique instance of pairs of objects.  This class
 * provides several overloads for callers to provide the objects themselves
 * without needing to explicitly create a {@link Pair} instance.
 *
 * <p> This class supports iterating over the set of instances being counted as
 * well as the instances and counts together.  All collections views that are
 * returned are unmodifiable and will throw an exception if a mutating method is
 * called.
 *
 * <p> This class is not thread-safe.  Unless otherwise noted, all methods will
 * throw a {@link NullPointerException} if passed a {@code null} element or a
 * {@code Pair} with a {@code null} element.
 *
 * @param T the type of object being counted.
 */
public class PairCounter<T> implements Counter<Pair<T>>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from the concatenated IDs of the pair's objects to the number
     * of times that pair has been seen
     */
    private final TLongIntMap counts;

    /**
     * A mapping from each element of a pair to the a corresponding index.  This
     * indexing scheme avoids mapping each {@link Pair} instance to an index,
     * and instead maps the concatenation of the pair's elements' IDs to a
     * count, which minizes the space required.
     */
    private final Indexer<T> elementIndices;

    /**
     * The total number of objects that have been counted
     */
    private int sum;

    /**
     * Creates an empty {@code Counter}.
     */
    public PairCounter() {
        counts = new TLongIntHashMap();
        elementIndices = new HashIndexer<T>();
        sum = 0;
    }

    /**
     * Creates a {@code Counter} whose initial state has counted all of the
     * specified items.
     */
    public PairCounter(Collection<? extends Pair<T>> items) {
        this();
        for (Pair<T> item : items)
            count(item);
    }      

    /**
     * Adds the counts from the provided {@code Counter} to the current counts,
     * adding new elements as needed.
     */
    public void add(Counter<? extends Pair<T>> counter) {
        for (Map.Entry<? extends Pair<T>,Integer> e : counter) {
            count(e.getKey(), e.getValue());
        }
    }

    /**
     * Returns the concatenated index of the pair's elements.
     */
    private long getIndex(Pair<T> p) {
        return getIndex(p.x, p.y);
    }

    /**
     * Returns the concatenated index of the two elements.
     */
    private long getIndex(T x, T y) {
        int i = elementIndices.index(x);
        int j = elementIndices.index(y);
        long index = (((long)i) << 32) | j;
        return index;
    }

    /**
     * Counts the pair of objects, increasing its total count by 1.
     */
    public int count(Pair<T> obj) {
        long index = getIndex(obj);
        int count = counts.get(index);
        count++;
        counts.put(index, count);
        sum++;
        return count;
    }

    /**
     * Counts the pair of objects, increasing its total count by 1.
     */
    public int count(T x, T y) {
        long index = getIndex(x, y);
        int count = counts.get(index);
        count++;
        counts.put(index, count);
        sum++;
        return count;
    }

    /**
     * Counts the pair, increasing its total count by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(Pair<T> obj, int count) {
        if (count < 1)
            throw new IllegalArgumentException("Count must be positive: " + count);
        long index = getIndex(obj);
        int oldCount = counts.get(index);
        int newCount = count + oldCount;
        counts.put(index, newCount);
        sum += count;
        return newCount;
    }

    /**
     * Counts the pair, increasing its total count by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(T x, T y, int count) {
        if (count < 1)
            throw new IllegalArgumentException("Count must be positive: " + count);
        long index = getIndex(x, y);
        int oldCount = counts.get(index);
        int newCount = count + oldCount;
        counts.put(index, newCount);
        sum += count;
        return newCount;
    }

    /**
     * {@inheritDoc}
     */
    public void countAll(Collection<? extends Pair<T>> c) {
        for (Pair<T> t : c)
            count(t);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof Counter) {
            Counter<?> c = (Counter<?>)o;
            if (counts.size() != c.size() || sum != c.sum())
                return false;
            for (Map.Entry<?,Integer> e : c) {
                Object k = e.getKey();
                if (!(k instanceof Pair))
                    return false;
                @SuppressWarnings("unchecked")
                Pair<T> p = (Pair<T>)o;
                int i = counts.get(getIndex(p));
                if (i != e.getValue())
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the number of times the specified pair of objects has been seen
     * by this counter.
     */
    public int getCount(Pair<T> obj) {
        // REMINDER: check for indexing?
        return counts.get(getIndex(obj));
    }

    /**
     * Returns the number of times the specified pair of objects has been seen
     * by this counter.
     */
    public int getCount(T x, T y) {
        // REMINDER: check for indexing?
        return counts.get(getIndex(x, y));
    }

    /**
     * Returns the frequency of this object pair relative to the counts of all other
     * object pair.  This value may also be interpreted as the probability of the
     * instance of {@code p} being seen in the items that have been counted.
     */
    public double getFrequency(Pair<T> p) {
        double count = getCount(p);
        return (sum == 0) ? 0 : count / sum;
    }

    public int hashCode() {
        return counts.hashCode();
    }

    /**
     * Returns a view of the pairs currently being counted.  The returned view
     * is read-only; any attempts to modify this view will throw an {@link
     * UnsupportedOperationException}.
     */
    public Set<Pair<T>> items() {
        return new PairSet();
    }

    /**
     * Returns an iterator over the pairs that have been counted thusfar and
     * their respective counts.
     */
    public Iterator<Map.Entry<Pair<T>,Integer>> iterator() {
        return new PairCountIterator();
    }
    
    /**
     * Returns the pair that currently has the largest count.  If no pairs
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public Pair<T> max() {
        int maxCount = 0;
        TLongIntIterator iter = counts.iterator();
        long maxIndex = 0;

        while (!iter.hasNext()) {
            iter.advance();

            int count = iter.value();
            if (count > maxCount) {
                maxIndex = iter.key();
                maxCount = count;
            }
        }
        if (maxCount == 0)
            return null;
        
        int i = (int)(maxIndex >>> 32);
        int j = (int)(maxIndex & 0xfffffff);
        return new Pair<T>(elementIndices.lookup(i), 
                           elementIndices.lookup(j));
    }

    /**
     * Returns the pair that currently has the smallest count.  If no pairs
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public Pair<T> min() {
        int minCount = Integer.MAX_VALUE;
        TLongIntIterator iter = counts.iterator();
        long minIndex = 0;

        while (!iter.hasNext()) {
            iter.advance();

            int count = iter.value();
            if (count < minCount) {
                minIndex = iter.key();
                minCount = count;
            }
        }
        if (minCount == Integer.MAX_VALUE)
            return null;
        
        int i = (int)(minIndex >>> 32);
        int j = (int)(minIndex & 0xfffffff);
        return new Pair<T>(elementIndices.lookup(i), 
                           elementIndices.lookup(j));
    }

    /**
     * Resets the counts for all pairs.  The size of {@link #items()} will be
     * 0 after this call.
     */
    public void reset() {
        counts.clear();
        sum = 0;
    }

    /**
     * Returns the number of pairs that are currently being counted.
     */
    public int size() {
        return counts.size();
    }

    /**
     * Returns the total number of pairs that have been counted.
     */
    public int sum() {
        return sum;
    }

    @Override public String toString() {
        // REMINDER: implement a more interpretable toString using the Iterator
        return counts.toString();
    }

    /**
     * A {@link Set} view of the pairs in this {@code Counter}.
     */
    private class PairSet extends AbstractSet<Pair<T>> {

        public boolean contains(Object o) {
            if (o instanceof Pair) {
                @SuppressWarnings("unchecked")
                Pair<T> p = (Pair<T>)o;
                return getCount(p) > 0;
            }
            return false;
        }

        public Iterator<Pair<T>> iterator() {
            return new PairIterator();
        }

        public boolean isEmpty() {
            return counts.isEmpty();
        }

        public int size() {
            return counts.size();
        }

    }

    /**
     * A {@link Iterator} over the pairs in this {@code Counter}.
     */
    private class PairIterator implements Iterator<Pair<T>> {

        private final TLongIntIterator iter;

        public PairIterator() {
            iter = counts.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public Pair<T> next() {
            if (!iter.hasNext())
                throw new NoSuchElementException();
            iter.advance();
            long key = iter.key();
            int i = (int)(key >>> 32);
            int j = (int)(key & 0xfffffff);
            return new Pair<T>(elementIndices.lookup(i), 
                               elementIndices.lookup(j));
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A {@link Iterator} over the pairs and their respective counts in this
     * {@code Counter}.
     */
    private class PairCountIterator 
            implements Iterator<Map.Entry<Pair<T>,Integer>> {

        private final TLongIntIterator iter;

        public PairCountIterator() {
            iter = counts.iterator();
        }

        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public Map.Entry<Pair<T>,Integer> next() {
            if (!iter.hasNext())
                throw new NoSuchElementException();
            iter.advance();
            long key = iter.key();
            int count = iter.value();
            int i = (int)(key >>> 32);
            int j = (int)(key & 0xfffffff);
            Pair<T> p = new Pair<T>(elementIndices.lookup(i), 
                                    elementIndices.lookup(j));
            return new AbstractMap.SimpleEntry<Pair<T>,Integer>(p, count);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}