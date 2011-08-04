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

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * A {@code Map} implementation that grows to a fixed size and then retains only
 * a fixed number of the highest (largest) keys.  All keys used in this class
 * must implements {@link Comparable}.
 *
 * @see BoundedSortedMultiMap
 */
public class BoundedSortedMap<K,V> extends TreeMap<K,V> {

    private static final long serialVersionUID = 1;

    /**
     * The maximum number of mappings to retain.
     */
    private final int bound;
    
    /**
     * Creatins an instance that will only retain the specified number of the
     * largest (highest) keys.
     *
     * @param bound the number of mappings to retain
     */
    public BoundedSortedMap(int bound) {
	this(bound, true);
    }

    /**
     * Creatins an instance that will only retain the specified number of keys,
     * where the largest (highest) keys.
     *
     * @param bound the number of mappings to retain
     * @param retainHighest {@code true} if the highest elements are to be
     *        retained, {@code false} if the lowest keys are to be retained
     */
    public BoundedSortedMap(int bound, boolean retainHighest) {
	super(((retainHighest) ? null : new ReverseComparator<K>()));
	this.bound = bound;
    }

    /**
     * Adds the key-value mapping to this map, and if the total number of
     * mappings exceeds the bounds, removes either the currently lowest element,
     * or if reversed, the currently highest element.
     *
     * @param key {@inheritDoc}
     * @param value {@inheritDoc}
     */
    public V put(K key, V value) {
	V old = super.put(key, value);
	if (size() > bound) {
	    remove(firstKey());
	}
	return old;
    }

    /**
     * Adds all of the key-value mapping to this map, and if the total number of
     * mappings exceeds the bounds, removes mappings until the size is within
     * bounds.
     *
     * @param m {@inheritDoc}
     */
    public void putAll(Map<? extends K,? extends V> m) {
	for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
	    put(e.getKey(), e.getValue());
	}
    }

    /**
     * A comparator that results in the opposite ordering of the natural
     * ordering from {@link Comparator#compareTo(Object,Object) compareTo}.
     */
    static final class ReverseComparator<K> 
            implements Comparator<K>, java.io.Serializable {

        private static final long serialVersionUID = 1;
	
	// Assume that if the comparator is being used that the objects are
	// instances of Comparable
	@SuppressWarnings("unchecked")
	public int compare(K c1, K c2) {
	    return -(((Comparable)c1).compareTo(c2));
	}
    }
}
