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

package edu.ucla.sspace.common;


/**
 * An interface for {@link SemanticSpace} instances that are meaningfully
 * interpretable.  In most cases, the dimensions will be understandable by human
 * viewers, but this interface provides support for mapping a dimension to a
 * generic {@code Object} for using the description in some programatic manner.
 */
public interface DimensionallyInterpretableSemanticSpace<T>
    extends SemanticSpace {

    /**
     * Returns a description of what features with which the specified dimension
     * corresponds.
     *
     * @param dimension a dimension number
     *
     * @return a description of the features for the dimension
     */
    T getDimensionDescription(int dimension);

}
