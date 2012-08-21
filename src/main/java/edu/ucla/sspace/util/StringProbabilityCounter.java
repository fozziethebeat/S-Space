/*
 * Copyright 2012 Keith Stevens
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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;


/** 
 * A utility for counting unique instance of an object which have real values
 * attached to each observation.  This is a direct copy of the {@link Counter}
 * interface however allows for aggregating counts as {@code doubles} instead of
 * {@code integers}.  
 *
 * </p>
 *
 * This class supports iterating over the set of instances being counted as
 * well as the instances and counts together.  All collections views that are
 * returned are unmodifiable and will throw an exception if a mutating method is
 * called.
 *
 * @author Keith Stevens
 */
public class StringProbabilityCounter implements Iterable<Map.Entry<String, Double>> {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from an object to the number of times it has been seen
     */
    private final TObjectDoubleMap<String> counts;

    /**
     * The total number of objects that have been counted
     */
    private double sum;

    /**
     * Creates an empty {@code Counter}.
     */
    public StringProbabilityCounter() {
        counts = new TObjectDoubleHashMap<String>();
        sum = 0;
    }

    /**
     * Creates a {@code Counter} whose initial state has counted all of the
     * specified items.
     */
    public StringProbabilityCounter(Collection<String> items) {
        this();
        for (String item : items)
            count(item);
    }      

    /**
     * Adds the counts from the provided {@code Counter} to the current counts,
     * adding new elements as needed.
     */
    public void add(StringProbabilityCounter counter) {
        for (Map.Entry<String,Double> e : counter)
            count(e.getKey(), e.getValue());
    }
    
    /**
     * Counts the object, increasing its total count by 1.
     */
    public double count(String obj) {
        return count(obj, 1d);
    }

    /**
     * Counts the object, increasing its total count by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public double count(String obj, double count) {
        double oldCount = counts.get(obj);
        double newCount = count + oldCount;
        counts.put(obj, newCount);
        sum += count;
        return newCount;
    }

    /**
     * {@inheritDoc}
     */
    public void countAll(Collection<String> c) {
        for (String  t : c)
            count(t);
    }

    public boolean equals(Object o) {
        if (o instanceof StringProbabilityCounter) {
            StringProbabilityCounter c = (StringProbabilityCounter) o;
            if (counts.size() != c.size() || sum != c.sum())
                return false;
            for (Map.Entry<String,Double> e : c)
                if (e.getValue() != counts.get(e.getKey()))
                    return false;
            return true;
        }
        return false;
    }

    /**
     * Returns the number of times the specified object has been seen by this
     * counter.
     */
    public double getCount(String obj) {
        return counts.get(obj);
    }

    /**
     * Returns the frequency of this object relative to the counts of all other
     * objects.  This value may also be interpreted as the probability of the
     * instance of {@code obj} being seen in the items that have been counted.
     */
    public double getFrequency(String obj) {
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
    public Set<String> items() {
        return Collections.unmodifiableSet(counts.keySet());
    }

    /**
     * Returns an interator over the elements that have been counted thusfar and
     * their respective counts.
     */
    public Iterator<Map.Entry<String,Double>> iterator() {
        return Collections.unmodifiableSet(
            TDecorators.wrap(counts).entrySet()).iterator();
    }
    
    /**
     * Returns the element that currently has the largest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public String max() {
        TObjectDoubleIterator<String> iter = counts.iterator();
        double maxCount = -1;
        String max = null;
        while (iter.hasNext()) {
            iter.advance();
            if (iter.value() > maxCount) {
                max = iter.key();
                maxCount = iter.value();
            }
        }
        return max;
    }

    /**
     * Returns the element that currently has the smallest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    public String min() {
        TObjectDoubleIterator<String> iter = counts.iterator();
        Double minCount = Double.MAX_VALUE;
        String min = null;
        while (iter.hasNext()) {
            iter.advance();
            if (iter.value() < minCount) {
                min = iter.key();
                minCount = iter.value();
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
    public double sum() {
        return sum;
    }

    @Override public String toString() {
        return counts.toString();
    }
}
