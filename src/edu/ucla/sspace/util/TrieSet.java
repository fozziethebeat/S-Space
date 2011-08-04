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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;


/**
 * A space-efficient set of {@link String} instances.  In addition to providing
 * reduced space usage, testing for contains may offer increased performance
 * over a {@link java.util.HashMap} of String instances due to not requiring the
 * {@link String#hashCode()} to be computed.
 *
 * <p>
 *
 * This class is not synchronized.  If concurrent updating behavior is required,
 * the map should be wrapped using {@link
 * java.util.Collections#synchronizedSet(Set)}.  This map will never throw a
 * {@link java.util.ConcurrentModificationException} during iteration.  The
 * behavior is unspecified if the map is modified while an iterator is being
 * used.
 *
 * @author David Jurgens
 */
public class TrieSet extends AbstractSet<String> {
    
    /**
     * The map that contains this sets value
     */
    private final TrieMap<Object> backingMap;

    /**
     * Creates an empty set of strings.
     */
    public TrieSet() {
        backingMap = new TrieMap<Object>();
    }

    /**
     * Creates a set of the strings contained in the collection.
     */
    public TrieSet(Collection<String> c) {
        this();
        for (String s : c)
            backingMap.put(s, Boolean.valueOf(true));
    }

    /**
     * Adds the specified element to this set if it is not already present.
     */
    public boolean add(String s) {
        return backingMap.put(s, Boolean.valueOf(true)) == null;
    }

    /**
     * Removes all of the elements from this set.
     */
    public void clear() {
        backingMap.clear();
    }
    
    /**
     * Returns {@code true} if this set contains the specified element.
     */
    public boolean contains(Object o) {
        return backingMap.containsKey(o);
    }

    /**
     * Returns {@code true} if this set contains no elements.
     */
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    /**
     * Returns an iterator over the elements in this set.
     */
    public Iterator<String> iterator() {
        return backingMap.keySet().iterator();
    }

    /**
     * Removes the specified element from this set if it is present.
     */
    public boolean remove(Object o) {
        return backingMap.remove(o) != null;
    }
    
    /**
     * Returns the number of elements in this set.
     */
    public int size() {
        return backingMap.size();
    }

    public String toString() {
        return backingMap.keySet().toString();
    }
}
