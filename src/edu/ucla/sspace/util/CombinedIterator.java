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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * An {@code Iterator} implemntation that combines multiple iterators into a
 * single instance.<p>
 *
 * This class is thread-safe.
 *
 * @author David Jurgens
 */ 
public class CombinedIterator<T> implements Iterator<T> {

    /**
     * The iterators to use
     */
    private final Queue<Iterator<T>> iters;

    /**
     * The curruent iterator from which elements are being drawn.
     */
    private Iterator<T> current;

    /**
     * The iterator that was last used to return an element.  This field is
     * needed to support {@code remove}
     */
    private Iterator<T> prev;

    /**
     * Constructs a {@code CombinedIterator} from all of the provided iterators.
     */
    public CombinedIterator(Iterator<T>... iterators) {
	this(Arrays.asList(iterators));
    }

    /**
     * Constructs a {@code CombinedIterator} from all of the iterators in the
     * provided collection.
     */
    public CombinedIterator(Collection<Iterator<T>> iterators) {
	iters = new ArrayDeque<Iterator<T>>();
	iters.addAll(iterators);
	current = iters.poll();
    }

    /**
     * Moves to the next iterator in the queue if the current iterator is out of
     * elements.
     */
    private void advance() {
	if (!current.hasNext()) {
	    prev = current;
	    current = iters.poll();
	}
    }

    /**
     * Returns true if there are still elements in at least one of the backing
     * iterators.
     */
    public synchronized boolean hasNext() {
	return current != null && current.hasNext();
    }

    /**
     * Returns the next element from some iterator.
     */
    public synchronized T next() {
	if (current == null) {
	    throw new NoSuchElementException();
	}
	T t = current.next();
	advance();
	return t;
    }

    /**
     * Removes the previously returned element using the backing iterator's
     * {@code remove} method.
     */
    public synchronized void remove() {
	if (prev == null) {
	    throw new NoSuchElementException();
	}	
	prev.remove();
    }
}