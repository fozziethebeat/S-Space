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
 * A utility class for mapping a set of objects to unique indices.  The indices
 * returned by this class will always being at {@code 0}.
 *
 * @see Counter
 */
public interface Indexer<T> extends Iterable<Map.Entry<T,Integer>> {

    /**
     * Adds the item and creates an index for it, returning that index.
     */
    int index(T item);

    /**
     * Removes all of the item-index mappings.
     */
    void clear();

    /**
     * Returns {@code true} if the item has a corresponding index
     */
    boolean contains(T item);

    /**
     * Returns an unmodifiable view of the items currently mapped to indices
     */
    Set<T> items();

    /**
     * Returns the index for the item or a negative value corresponding to the
     * index the object would have been assigned if it had been added.
     */
    int find(T item);
    
    /**
     * Returns the highest index to which any element is mapped.
     */
    int highestIndex();

    /**
     * Returns an iterator over all the objects and their corresponding indices.
     * The returned iterator does not support {@code remove}.
     */
    Iterator<Map.Entry<T,Integer>> iterator();

    /**
     * Returns the element to which this index is mapped or {@code null} if the
     * index has not been mapped
     */
    T lookup(int index);

    /**
     * Returns an unmodifiable view from each index to the object mapped to that
     * index.
     */
    Map<Integer,T> mapping();
    
    /**
     * Returns the number of items that are mapped to indices.
     */
    int size();
}