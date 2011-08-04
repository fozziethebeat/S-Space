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

package edu.ucla.sspace.util;


/**
 * A interface for array-like classes that use a sparse internal representation
 * for numeric values.  This interface exposes additional methods for
 * manipulating the array values, which may offer performance improvements when
 * compound operations are done in the internal reprentation.  The exact details
 * of performance are specified by the implementation themselves.<p>
 *
 * In addition, it is suggested that instances that wrap Java primitive-type
 * arrays provide additional methods that operate directly on the primitive
 * types rather than their object equivalents.  This allows users to save any
 * performance costs from auto-boxing the primitives.
 */
public interface SparseNumericArray<T extends Number> extends SparseArray<T> {

    /**
     * Adds the specified value to the index.
     *
     * @param index the position in the array
     * @param delta the change in value at the index
     */
    T add(int index, T delta);

}