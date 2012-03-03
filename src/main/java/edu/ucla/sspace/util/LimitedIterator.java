/*
 * Copyright 2009 Keith Stevens
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

import java.util.Iterator;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An iterator decorator that returns a limited number of string tokens.
 *
 * @author Keith Stevens
 */
public class LimitedIterator <T> implements Iterator<T> {

    /**
     * The base iterator to decorate.
     */
    private final Iterator<T> iter;

    /**
     * The current number of items returned by this iterator so far.
     */
    private int itemCount;

    /**
     * The maximum number of items to return by this iterator.
     */
    private final int maxItems;

    /**
     * Constructs an iterator for the first {@code maxItems} tokens contained in
     * given iterator. 
     */
    public LimitedIterator(Iterator<T> iter, int maxItems) {
        this.iter = iter;
        this.maxItems = maxItems;
        itemCount = 0;
    }

    /**
     * Returns {@code true} if there is another item to return.
     */
    public synchronized boolean hasNext() {
        return itemCount < maxItems && iter.hasNext();
    }

    /**
     * Returns the next item from the reader.
     */
    public synchronized T next() {
        itemCount++;
        return iter.next();
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }
}
