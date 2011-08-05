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

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 *
 */
public class CombinedSet<T> extends AbstractSet<T> 
        implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * The sets that back this set combination
     */
    private final List<Set<T>> sets;

    @SuppressWarnings("unchecked")
    public CombinedSet(Set<T>... sets) {
        if (sets == null)
            throw new NullPointerException("Sets cannot be null");
        this.sets = new ArrayList<Set<T>>();
        for (Set<T> s : sets) {
            if (s == null)
                throw new NullPointerException("Cannot wrap null set");
            this.sets.add(s);
        }
    }

    public CombinedSet(Collection<? extends Set<T>> sets) {
        if (sets == null)
            throw new NullPointerException("Sets cannot be null");
        this.sets = new ArrayList<Set<T>>();
        for (Set<T> s : sets) {
            if (s == null)
                throw new NullPointerException("Cannot wrap null set");
            this.sets.add(s);
        }
    }

    /**
     * Adds the provided set to this {@code CombinedSet}.  Note that this does
     * <i>not</i> perform an element-wise addition of the contents of {@code
     * set}, but rather keeps a reference to {@code set} so that any changes to
     * {@code set} will be reflected in this {@code CombinedSet}.
     */
    public void append(Set<T> set) {
        if (set == null)
            throw new NullPointerException("set cannot be null");
        sets.add(set);
    }
    
    /**
     * Returns {@code true} if at least one of the sets in this {@code
     * CombinedSet} has the provided element.
     */
    public boolean contains(Object o) {
        for (Set<T> s : sets)
            if (s.contains(o))
                return true;
        return false;
    }

    /**
     * Clears the elements from all backing sets.
     */
    public void clear() {
        for (Set<T> s : sets)
            s.clear();
    }

    /**
     * Returns an iterator over all the unique items across all sets.
     */
    public Iterator<T> iterator() {
        return new SetIterator();
    }

    /**
     * Returns {@code true} if all the sets are empty.
     */
    public boolean isEmpty() {
        for (Set<T> s : sets)
            if (!s.isEmpty())
                return false;
        return true;        
    }

    /**
     * Removes the provided element from all the backing sets, returning {@code
     * true} if at least one set contained that element.
     */
    public boolean remove(Object o) {
        boolean removed = false;
        for (Set<T> s : sets)
            if (s.remove(o))
                removed = true;
        return removed;
    }

    /**
     * Returns the number of unique items across all sets.
     */
    public int size() {
        // The size isn't the sum of the sets' sizes, as there may be duplicate
        // items in more than one set.  Therefore, we rely on the iterator's
        // correctness.
        int size = 0;
        for (T t : this)
            size++;
        return size;
    }

    /**
     * An iterator over the elements in all of the sets that checks for
     * duplicates during iteration to ensure that no element is returned twice.
     */
    private class SetIterator implements Iterator<T> {
        
        /**
         * The index of the current set.  This index is used to check whether
         * the elements of the current set have been returned by a previous set.
         */
        int curSet = -1;
        
        /**
         * An iterator over the current set's elements
         */
        Iterator<T> curIterator;

        /**
         * The previously returned element or {@code null} if the element was
         * removed or no element has been returned
         */
        private T cur;

        /**
         * The next unique element to return 
         */
        private T next;
        
        /**
         * The sets whose elements have yet to be returned
         */
        private Iterator<Set<T>> setsToProcess;

        /**
         * Creates a new iterator over the elements in the set.
         */
        public SetIterator() {
            setsToProcess = sets.iterator();
            advance();
        }

        /**
         * Set the {@code next} field to the next element to return or {@code
         * null} if no further elements exist
         */
        private void advance() {
            next = null;
            do {
                while ((curIterator == null || !curIterator.hasNext())
                        && setsToProcess.hasNext()) {
                    curIterator = setsToProcess.next().iterator();
                    curSet++;
                }
            
                if (curIterator == null 
                        || (!curIterator.hasNext() && !setsToProcess.hasNext()))
                    return;
            
                T t = curIterator.next();
                // Check whether this element has already been returned from a
                // prior set
                boolean wasReturned = false;
                for (int i = 0; i < curSet; ++i) {
                    if (sets.get(i).contains(t)) {
                        wasReturned = true;
                        break;
                    }
                }
                if (!wasReturned)
                    next = t;
            } while (next == null);
        }

        /**
         * Returns true if there is still one item left to return
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * Returns the next unique item from the sets
         */
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();
            cur = next;
            advance();
            return cur;
        }
        
        /**
         * Throws an {@link UnsupportedOperationException} if called
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}