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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MultiMap} that provides a total ordering for the keys.  All keys
 * intersted must implement the {@link Comparable} interface.  
 *
 * @see MultiMap
 * @see java.util.SortedMap
 */
public interface SortedMultiMap<K,V> extends MultiMap<K,V> {

    /**
     * Returns the comparator used to order the keys in this map, or null if
     * this map uses the {@link Comparable natural ordering} of its keys.
     */
    Comparator<? super K> comparator();

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     */
    K firstKey();

    /**
     * Returns a view of the portion of this map whose keys are less than {@code
     * toKey}.
     */
    SortedMultiMap<K,V> headMap(K toKey);

    /**
     * Returns the last (highest) key currently in this map.
     */
    K lastKey();

    /**
     * Returns a view of the portion of this map whose keys range from {@code
     * fromKey}, inclusive, to {@code toKey}, exclusive.
     */
    SortedMultiMap<K,V> subMap(K fromKey, K toKey);

    /**
     * Returns a view of the portion of this map whose keys are greater than or
     * equal to {@code fromKey}.
     */
    SortedMultiMap<K,V> tailMap(K fromKey);

}