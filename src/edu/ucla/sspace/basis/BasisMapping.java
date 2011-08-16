/*
 * Copyright 2010 David Jurgens 
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

import java.util.Set;


/**
 * An interface for specifying how a set of features can be mapped to a vector
 * basis.  In the naive case, each feature is mapped it its own dimension.
 * However, many approaches may use information about the feature to represent
 * several different features using a single dimension.  For example, each word
 * may correspond to a unique dimension regardless of how it is grammatically
 * related.
 *
 * </p>
 *
 * This interface also provides support for describing dimensions.  The named of
 * the description is left open to the implementation.  For example, an
 * implementation may chose to return a {@code String} with a human-readable
 * description.  Another implementation may return a {@code Set} of features
 * that are represented by the dimension in order to facilitate further
 * processing.
 *
 * @param <T> the type of feature being mapped to a dimension.
 * @param <E> the type of object to be used as a description of each dimension.
 */
public interface BasisMapping<T,E> {

    /**
     * Returns the dimension number corresponding to the provided feature.
     *
     * @param feature a feature whose value can be used 
     *
     * @return the dimension for the occurrence of the last word in the path
     */
    int getDimension(T features);
   
    /**
     * Returns a description of the specified dimension.  
     *
     * @param dimension a dimension number
     *
     * @return a description of the dimension
     */
    E getDimensionDescription(int dimension);

    /**
     * Returns the set of keys known by this {@link BasisMapping}
     */
    Set<E> keySet();

    /**
     * Returns the number of dimensions currently represented in this basis
     * mapping.
     */
    int numDimensions();    

    /**
     * Sets the read only state of the basis mapping.  This is intended for when
     * the basis mapping is needed to map to a known set of values, and any
     * unknown values are left unmapped.
     */
    void setReadOnly(boolean readOnly);

    /**
     * Returns true if the {@link BasisMapping} is read only, false otherwise.
     */
    boolean isReadOnly();
}
