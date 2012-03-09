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

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;


public class TroveIntSet extends AbstractIntSet
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The backing set for this instance
     */
    private final TIntSet set;
    
    /**
     * Creates a new, empty {@code TroveIntSet} with the default capacity (4).
     */
    public TroveIntSet() {
        this(4);
    }

    /**
     * Creates a new empty {@code TroveIntSet} with the specified capacity.
     */
    public TroveIntSet(int capacity) {
        set = new TIntHashSet(capacity);
    }

    /**
     * Creates a new {@code TroveIntSet} containing all the integers in the
     * provided collection
     */
    public TroveIntSet(Collection<? extends Integer> c) {
        this(c.size());
        addAll(c);
    }

    /**
     * Creates a new {@code TroveIntSet} containing all the integers in the
     * provided set.
     */
    public TroveIntSet(IntSet c) {
        this(c.size());
        addAll(c);
    }

    /**
     * Creates a new {@code TroveIntSet} containing all the integers in the
     * provided set.
     */
    private TroveIntSet(TIntSet set) {
        this.set = set;
    }

    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        return set.add(i);
    }

    public boolean contains(int i) {
        return set.contains(i);
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public IntIterator iterator() {
        return new TroveIterator();
    }

    public boolean remove(int i) {
        return set.remove(i);
    }

    public int size() {
        return set.size();
    }

    public static IntSet wrap(TIntSet set) {
        return new TroveIntSet(set);
    }

    private class TroveIterator implements IntIterator {

        private final TIntIterator iter;

        private boolean removed;

        public TroveIterator() {
            iter = set.iterator();
            removed = true;
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }

        public Integer next() {
            if (!iter.hasNext())
                throw new NoSuchElementException();
            removed = false;
            return iter.next();
        }

        public int nextInt() {
            if (!iter.hasNext())
                throw new NoSuchElementException();
            removed = false;
            return iter.next();
        }

        public void remove() {
            if (removed)
                throw new IllegalStateException();
            removed = true;
            iter.remove();
        }
    }
}