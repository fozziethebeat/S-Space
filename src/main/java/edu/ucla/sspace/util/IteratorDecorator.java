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

import java.util.Iterator;


/**
 * A decorator around an existing {@link Iterator} that is designed to
 * facilitate subclasses that need to extend the functionality of an existing
 * iterator.  For example, a subclass of this may need to perform additional
 * clean-up after a {@link #remove()} operation, or may need to ensure that the
 * current element meets some additional criteria by altering the behavior of
 * {@link #hasNext()}.
 *
 * <p> In its default behavior, this class performs identically to the backing
 * iterator, passing all method calls through and return the respective values.
 *
 * @author David Jurgens
 */
public class IteratorDecorator<T> implements Iterator<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The backing iterator 
     */
    private final Iterator<T> iter;

    /**
     * The element that was just returned or {@code null} if no elements have
     * been returned yet or if the current element was removed via {@link
     * #remove()}.
     */
    protected T cur;

    /**
     * Creates a new decorator around the existing iterator.
     */
    public IteratorDecorator(Iterator<T> iter) {
        if (iter == null)
            throw new NullPointerException();
        this.iter = iter;
    }

    /**
     * Returns {@code true} if this iterator has more elements to return
     */
    public boolean hasNext() {
        return iter.hasNext();
    }

    /**
     * Returns the next element
     */
    public T next() {
        cur = iter.next();
        return cur;
    }

    /**
     * Removes the current element if the backing iterator supports removal.
     */
    public void remove() {
        cur = null;
        iter.remove();
    }
}