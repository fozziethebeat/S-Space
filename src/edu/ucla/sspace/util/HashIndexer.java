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

import java.lang.reflect.Array;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gnu.trove.TDecorators;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/** 
 * A utility class for mapping a set of objects to unique indices based on
 * object equality.  The indices returned by this class will always being at
 * {@code 0}.
 *
 * @see Counter
 */
public class HashIndexer<T> implements Indexer<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from each item to its index.
     */
    private final TObjectIntHashMap<T> indices;

    /**
     * The inverse mapping from indices to the object to which they are mapped.
     * This lookup is computed as-needed and cached so long as the index mapping
     * remains valid.
     */
    private transient T[] indexLookup;

    /**
     * Creates an empty {@code HashIndexer} with no mappings.
     */
    public HashIndexer() {
        indices = new TObjectIntHashMap<T>();
    }

    /**
     * Creates an {@code HashIndexer} with indices for all of the provided items.
     */
    public HashIndexer(Collection<? extends T> items) {
        this();
        for (T item : items)
            index(item);
    }

    /**
     * Creates an {@code HashIndexer} with indices for all of the provided items.
     */
    public HashIndexer(Indexer<? extends T> indexed) {
        this();
        for (Map.Entry<? extends T,Integer> e : indexed)
            indices.put(e.getKey(), e.getValue());
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        indices.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean contains(T item) {
        return indices.containsKey(item);
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> items() {
        return Collections.unmodifiableSet(indices.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public int find(T item) {
        return (indices.containsKey(item)) 
            ? indices.get(item) : -indices.size();
    }

    /**
     * {@inheritDoc}
     */
    public int highestIndex() {
        return indices.size() - 1;
    }

    /**
     * {@inheritDoc}
     */
    public int index(T item) {
        if (indices.containsKey(item)) 
            return indices.get(item);
        synchronized(indices) {
            // Double check that the item we are currently trying to index
            // wasn't added while we were blocking
            if (indices.containsKey(item)) 
                return indices.get(item);
            int index = indices.size();
            indices.put(item, index);
            return index;
        }
    }

    /**
     * {@inheritDoc} The returned iterator does not support {@code remove}.
     */
    public Iterator<Map.Entry<T,Integer>> iterator() {
        // TODO: optimize this
        return Collections.unmodifiableSet(
            TDecorators.wrap(indices).entrySet()).iterator();
    }

    /**
     * {@inheritDoc}
     */
    public T lookup(int index) {
        if (index < 0 || index >= indices.size())
            return null;
        assert indices.size() > 0;
        // If the current lookup table is invalid or has yet to be computed,
        // recalcuate it
        if (indexLookup == null || indexLookup.length != indices.size()) {
            T t = indices.keySet().iterator().next();
            @SuppressWarnings("unchecked")
            T[] tmp = (T[])(Array.newInstance(t.getClass(), indices.size()));
            indexLookup = tmp;
            TObjectIntIterator<T> iter = indices.iterator();
            while (iter.hasNext()) {
                iter.advance();
                indexLookup[iter.value()] = iter.key();
            }
        }
        return indexLookup[index];
    }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,T> mapping() {
        throw new Error();
    }
    
    /**
     * {@inheritDoc}
     */
    public int size() {
        return indices.size();
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return indices.toString();
    }
}