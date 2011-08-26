/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.basis;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An abstract base class for {@link BasisMapping}s that implements most of the
 * required functionality.  For any requested feature, the currently mapped
 * dimension is returned upon request, or a new dimension is generated if the
 * {@link BasisMapping} is not in read only mode.
 *
 * </p>
 *
 * All access to this {@link BasisMapping} is thread safe.
 *
 * @author Keith Stevens
 */
public abstract class AbstractBasisMapping<T, K> implements BasisMapping<T, K>,
                                                            Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The mapping from keys to dimension indices.
     */
    private Map<K, Integer> mapping;

    /**
     * A cache of the reverse {@code mapping} mapping.  This field is only
     * updated on calls to {@link #getDimensionDescription(int)} when the
     * mapping has chanaged since the previous call.
     */
    transient private List<K> indexToKeyCache;

    /**
     * Set to {@code true} when the {@link BasisMapping} should not create new
     * dimensions for unseen keys.
     */
    private boolean readOnly;

    /**
     * Creates a new {@link AbstractBasisMapping}.
     */
    public AbstractBasisMapping() {
        mapping = new HashMap<K, Integer>();
        readOnly = false;
        indexToKeyCache = new ArrayList<K>();
    }

    /**
     * {@inheritDoc}
     */
    public K getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension > mapping.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);

        // If the cache is out of date, rebuild the reverse mapping.
        if (mapping.size() > indexToKeyCache.size()) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToKeyCache = new ArrayList<K>(mapping.size());
                for (int i = 0; i < mapping.size(); ++i)
                    indexToKeyCache.add(null);
                for (Map.Entry<K,Integer> e : mapping.entrySet())
                    indexToKeyCache.set(e.getValue(), e.getKey());
            }
        }
        return indexToKeyCache.get(dimension);
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return mapping.keySet();
    }

    /**
     * Returns an integer corresponding to {@code key}.  If in read only mode,
     * -1 is returned for unseen keys.  Otherwise, unseen keys are assigned a
     *  new dimension.
     */
    protected int getDimensionInternal(K key) {
        Integer index = mapping.get(key);
        if (readOnly)
            return (index == null) ? -1: index;

        if (index == null) {     
            synchronized(this) {
                // Recheck to see if the key was added while blocking.
                index = mapping.get(key);

                // If another thread has not already added this key while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = mapping.size();
                    mapping.put(key, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the internal mapping from keys to indices.
     */
    protected Map<K, Integer> getMapping() {
        return mapping;
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() {
        return mapping.size();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReadOnly() {
        return readOnly;
    }
}
