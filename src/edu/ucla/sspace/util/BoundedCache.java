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

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * A special-purpose {@link Map} that acts as an LRU cache for a fixed number of
 * elements.  Classes may use this to retain a limited nubmer of frequently-used
 * mappings.  For more details see {@link LinkedHashMap} on its access-order.
 *
 * @see LinkedHashMap
 */
public class BoundedCache<K,V> extends LinkedHashMap<K,V> {

    private static final long serialVersionUID = 1L;
    
    /**
     * The maximum number of elements to retain in the map
     */
    private final int maxSize;

    /**
     * Creates a cache that retains at most {@code maxSize} elements
     */
    public BoundedCache(int maxSize) {
        super(maxSize, .75f, true);
        this.maxSize = maxSize;
    }
    
    /**
     * Returns {@code true} if the addition of some entry has caused the current
     * size of the map to exceed its maximum size.
     */
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxSize;
    }
}