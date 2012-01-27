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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A refinement of the {@link Set} interface for implementations that store
 * {@code int} values.  Implementations are expected to provide increased
 * performance through primitive {@code int} methods and storage rather than
 * using {@link Integer}-based operations that the exiting Collections libraries
 * use.
 *
 * @author David Jurgens
 */
public interface IntSet extends Set<Integer>, IntCollection {

    boolean add(int i);

    boolean addAll(IntCollection ints);

    boolean contains(int i);

    boolean containsAll(IntCollection ints);

    IntIterator iterator();

    boolean remove(int i);

    boolean removeAll(IntCollection ints);

    /**
     * Retains only the elements in this set that are contained in the specified
     * {@code IntCollection}.
     */
    boolean retainAll(IntCollection ints);

    int[] toPrimitiveArray();
}