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

package edu.ucla.sspace.basis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A generic {@link BasisMapping} implementation that maps each unique element
 * of a type to a different dimension and returns the {@code String} value of
 * the dimension's associated element as its description.  Note that in order to
 * use this class all dimension keys must have a well-defined {@link
 * Object#equals(Object) equals} method.  This class supports mapping {@code
 * null} elements to a dimension as well.
 *
 * <p>Subclasses that wish to provide a more detailed description for each
 * dimension should override the {@link #describeDimension(Object)} method.
 *
 * <p><i>Implementation note</i>: the {@link #getDimensionDescription(int)}
 * method operates in amortized constant time.  Rather than continuously
 * updating a mapping as new dimensions are added, this value is only calcuated
 * for the upon the first call to the method after this basis function has been
 * modified, which takes O(n) time.  Any subsequent calls should will return the
 * description in constant time.
 *
 * @author David Jurgens
 */
public class GenericBasisMapping<T>
        implements BasisMapping<T,String>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A mapping from an object to its associated dimension.
     */
    private final Map<T,Integer> keyToDimension;
    
    /**
     * A cache of the reverse object-to-dimension mapping.  This field is only
     * updated on calls to {@link #getDimensionDescription(int)} when the
     * mapping has chanaged since the previous call.
     */
    private String[] indexToDescriptionCache;

    /**
     * Creates an empty {@code GenericBasisMapping}.
     */
    public GenericBasisMapping() {
        keyToDimension = new HashMap<T,Integer>();
        indexToDescriptionCache = new String[0];
    }

    /**
     * Returns a description of the dimension, which is mapped to the provided
     * key.
     */
    protected String describeDimension(int dimension, T dimensionKey) {
        return String.valueOf(dimensionKey);
    }

    /**
     * Returns the dimension number corresponding to the provided
     * object key.
     *
     * @param obj an object that is to be mapped to a specific dimension
     *
     * @return the dimension number associated with the provided object
     */
    public int getDimension(T obj) {       
        Integer index = keyToDimension.get(obj);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = keyToDimension.get(obj);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = keyToDimension.size();
                    keyToDimension.put(obj, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Returns the string value of the object mapped to the specified dimension.
     */
    public String getDimensionDescription(int dimension) {
        if (dimension < 0 || dimension > keyToDimension.size())
            throw new IllegalArgumentException(
                "invalid dimension: " + dimension);
        // If the cache is out of date, rebuild the reverse mapping.
        if (keyToDimension.size() > indexToDescriptionCache.length) {
            // Lock to ensure safe iteration
            synchronized(this) {
                indexToDescriptionCache = new String[keyToDimension.size()];
                for (Map.Entry<T,Integer> e 
                         : keyToDimension.entrySet()) {
                    indexToDescriptionCache[e.getValue()] = 
                        describeDimension(e.getValue(), e.getKey());
                }
            }
        }
        return indexToDescriptionCache[dimension];
    }

    /**
     * {@inheritDoc}
     */
    public int numDimensions() { 
        return keyToDimension.size();
    }
}