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
 * A utility for counting unique instance of an object beyond what is
 * representable with a {@link Counter} instance.  Instances of this interface
 * are intended only for handling larg data sets containing more than {@value
 * Integer#MAX_VALUE} instances of a single item, or where the total collection
 * size exceeds that sum.  Note that this interfaces still does not suppport
 * storing counts for more than {@value Integer#MAX_VALUE} unique items.
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
public interface BigCounter<T> extends Iterable<Map.Entry<T,Long>> {

    /**
     * Adds the counts from the provided {@code BigCounter} to the current counts,
     * adding new elements as needed.
     *
     * @return the current count for the counted object
     */
    void add(BigCounter<? extends T> counter);

    /**
     * Adds the counts from the provided {@code BigCounter} to the current counts,
     * adding new elements as needed.
     *
     * @return the current count for the counted object
     */
    void add(Counter<? extends T> counter);
    
    /**
     * Counts the object, increasing its total count by 1.
     */
    long count(T obj);

    /**
     * Counts the object, increasing its total count by the specified positive
     * amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @return the current count for the counted object
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    long count(T obj, long count);

    /**
     * Counts all the elements in the collection.
     *
     * @param c a collection of elements to count
     */
    void countAll(Collection<? extends T> c);

    boolean equals(Object o);

    /**
     * Returns the number of times the specified object has been seen by this
     * counter.
     */
    long getCount(T obj);

    /**
     * Returns the frequency of this object relative to the counts of all other
     * objects.  This value may also be interpreted as the probability of the
     * instance of {@code obj} being seen in the items that have been counted.
     */
    double getFrequency(T obj);

    int hashCode();

    /**
     * Returns a view of the items currently being counted.  The returned view
     * is read-only; any attempts to modify this view will throw an {@link
     * UnsupportedOperationException}.
     */
    Set<T> items();

    /**
     * Returns an interator over the elements that have been counted thusfar and
     * their respective counts.
     */
    Iterator<Map.Entry<T,Long>> iterator();
    
    /**
     * Returns the element that currently has the largest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    T max();

    /**
     * Returns the element that currently has the smallest count.  If no objects
     * have been counted, {@code null} is returned.  Ties in counts are
     * arbitrarily broken.
     */
    T min();

    /**
     * Resets the counts for all objects.  The size of {@link #items()} will be
     * 0 after this call.
     */
    void reset();

    /**
     * Returns the number of unique instances that are currently being counted.
     */
    int size();

    /**
     * Returns the total number of instances that have been counted.
     */
    long sum();    
}