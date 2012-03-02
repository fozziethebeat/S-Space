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

package edu.ucla.sspace.util.primitive;

import java.io.Serializable;

import edu.ucla.sspace.util.MultiMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import gnu.trove.TIntCollection;
import gnu.trove.set.TIntSet;
import gnu.trove.map.TIntIntMap;


/**
 * A {@link MultiMap} subinterface for mapping {@code int} primitives as both
 * keys and values.
 */
public interface IntIntMultiMap extends MultiMap<Integer,Integer> {

    /**
     * Returns {@code true} if this multi-map contains a mapping for the
     * specified key.
     */
    boolean containsKey(int key);

    /**
     * Returns {@code true} if this multi-map contains a mapping from specified
     * key to the specified value.
     */
    boolean containsMapping(int key, int value);

    /**
     * Returns {@code true} if this multi-map contains from any key to the
     * specified value.
     */
    boolean containsValue(int key);

    /**
     * Returns the set of values mapped to this key or {@code null} of the key
     * is not mapped to any values
     */
    IntSet get(int key);

    /**
     * Returns a {@link Set} view of the mappings contained in this multi-map.
     */
    IntSet keySet();

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
    boolean put(int key, int value);

    /**
     * Copies all of the mappings from the specified map to this mutli-map
     */
     void putAll(IntIntMultiMap m);

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
    boolean putMany(int key, IntCollection values);

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
    boolean putMany(int key, Collection<Integer> values);

    /**
     * Removes the mapping for a key from this multi-map if it is present
     */
    IntSet remove(int key);

    /**
     * Removes the specified mapping for a key if it is present.  If the key is
     * mapped to other values, it is retained, otherwise the key is removed from
     * the map.  This method provides a way to remove a specific mapping for a
     * key without removing all other existing mappings.
     *
     * @return {@code true} if the specified mapping was removed
     */
    boolean remove(int key, int value);

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    IntCollection values();

}