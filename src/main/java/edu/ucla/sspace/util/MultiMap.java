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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * An object that maps a key to one or more values.  A map cannot contain
 * duplicate keys and each key cannot map to duplicate values.
 *
 * @see Map
 */
public interface MultiMap<K,V> {

    /**
     * Removes all of the mappings from this multi-map.
     */
    public void clear();

    /**
     * Returns {@code true} if this multi-map contains a mapping for the
     * specified key.
     */
    public boolean containsKey(Object key);

    /**
     * Returns {@code true} if this multi-map contains from any key to the
     * specified value.
     */
    public boolean containsValue(Object key);

    /**
     * Returns a {@link Set} view of all the key-value mappings contained in
     * this multi-map.
     */
    public Set<Map.Entry<K,V>> entrySet();

    /**
     * Returns {@code true} if this multi-map maps one or more keys to the specified
     * value.
     */
    public Set<V> get(Object key);

    /**
     * Returns {@code true} if this multi-map contains no mappings.
     */
    public boolean isEmpty();

    /**
     * Returns a {@link Set} view of the mappings contained in this multi-map.
     */
    public Set<K> keySet();

    /**
     * Adds the specified value to the set of values associated with the
     * specified key in this map.
     *
     * @param key key with which the specified value is to be associated
     * @param value a value to be associated with the specified key
     *
     * @return {@code true} if the provided value was not already in the set of
     *         value mapped to the specified key
     */
    public boolean put(K key, V value);

    /**
     * Copies all of the mappings from the specified map to this mutli-map
     */
    public void putAll(Map<? extends K,? extends V> m);

    /**
     * Copies all of the mappings from the specified mulit-map to this mutli-map
     */
    public void putAll(MultiMap<? extends K,? extends V> m);

    /**
     * Adds all of the specified values to the set of values associated with the
     * specified key in this map.
     *
     * @param key key with which the specified value is to be associated
     * @param values a collection of values to be associated with the specified
     *        key
     *
     * @return {@code true} if at least one of the provided value was not
     *         already in the set of value mapped to the specified key
     */
    public boolean putMulti(K key, Collection<V> values);

    /**
     * Removes the mapping for a key from this multi-map if it is present
     */
    public Set<V> remove(K key);

    /**
     * Removes the specified mapping for a key if it is present.  If the key is
     * mapped to other values, it is retained, otherwise the key is removed from
     * the map.  This method provides a way to remove a specific mapping for a
     * key without removing all other existing mappings.
     *
     * @return {@code true} if the specified mapping was removed
     */
    public boolean remove(K key, V value);

    /**
     * Returns the number of values maped to keys.  Note that in the
     * bijective case, this will equal the value returned by {@link
     * #size()}.  However, in the case where a key is mapped to more
     * than one value, this method will always return a value strictly
     * larger than {@code size()}.
     *
     * @return the number of values maped to keys
     */
    public int range();

    /**
     * Returns the number of keys that are mapped to one or more values in this
     * multi-map.
     *
     * @see #range()
     */
    public int size();

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values();

}