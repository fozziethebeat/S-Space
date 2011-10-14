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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/** 
 * A utility for counting unique instance of an object.  
 *
 * <p> This class supports iterating over the set of instances being counted as
 * well as the instances and counts together.  All collections views that are
 * returned are unmodifiable and will throw an exception if a mutating method is
 * called.
 *
 * <p> This class is not thread-safe
 *
 * @param T the type of object being counted.
 */
public class Counter<T> implements Iterable<Map.Entry<T,Integer>>,
                                   java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from an object to the number of times it has been seen
     */
    private final Map<T,Integer> counts;

    /**
     * The total number of objects that have been counted
     */
    private int sum;

    /**
     * Creates an empty {@code Counter}.
     */
    public Counter() {
        counts = new HashMap<T,Integer>();
        sum = 0;
    }

    /**
     * Creates a {@code Counter} whose initial state has counted all of the
     * specified items.
     */
    public Counter(Collection<? extends T> items) {
        this();
        for (T item : items)
            count(item);
    }      
    
    /**
     * Counts the object, increasing its total count by 1.
     */
    public int count(T obj) {
        Integer count = counts.get(obj);
        int newCount = (count == null) ? 1 : count + 1;
        counts.put(obj, newCount);
        sum++;
        return newCount;
    }

    /**
     * Counts the object, increasing its total count by {@code inc}.
     */
    public int count(T obj, int inc) {
        Integer count = counts.get(obj);
        int newCount = (count == null) ? inc : count + inc;
        counts.put(obj, newCount);
        sum += inc;
        return newCount;
    }
    public boolean equals(Object o) {
        return (o instanceof Counter)
            && ((Counter)o).counts.equals(counts);
    }

    /**
     * Returns the number of times the specified object has been seen by this
     * counter.
     */
    public int getCount(T obj) {
        Integer count = counts.get(obj);
        return (count == null) ? 0 : count;
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
        return counts.hashCode();
    }

    /**
     * Returns a view of the items currently being counted.  The returned view
     * is read-only; any attempts to modify this view will throw an {@link
     * UnsupportedOperationException}.
     */
    public Set<T> items() {
        return Collections.unmodifiableSet(counts.keySet());
    }

    /**
     * Returns an interator over the elements that have been counted thusfar and
     * their respective counts.
     */
    public Iterator<Map.Entry<T,Integer>> iterator() {
        return Collections.unmodifiableSet(counts.entrySet()).iterator();
    }
    
    /**
     * Returns the element that currently has the largest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public T max() {
        int maxCount = -1;
        T max = null;
        for (Map.Entry<T,Integer> e : counts.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                max = e.getKey();
            }
        }
        return max;
    }

    /**
     * Returns the element that currently has the smallest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public T min() {
        int minCount = -1;
        T min = null;
        for (Map.Entry<T,Integer> e : counts.entrySet()) {
            if (e.getValue() < minCount) {
                minCount = e.getValue();
                min = e.getKey();
            }
        }
        return min;
    }

    /**
     * Resets the counts for all objects.  The size of {@link #items()} will be
     * 0 after this call.
     */
    public void reset() {
        counts.clear();
        sum = 0;
    }

    /**
     * Returns the number of instances that are currently being counted.
     */
    public int size() {
        return counts.size();
    }

    /**
     * Returns the total number of instances that have been counted.
     */
    public int sum() {
        return sum;
    }

    @Override public String toString() {
        return counts.toString();
    }
}
