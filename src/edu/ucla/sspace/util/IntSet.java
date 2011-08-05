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
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A {@link BitSet}-backed {@link Set} implementation for storing non-negative
 * {@code int} values.  This set offers a space-efficient method for storing
 * small, or densly populated sets of {@code ints}.  
 *
 * <p>Because this class only supports storing non-negative values in the set,
 * any attempt to add a negative value will throw an {@link
 * IllegalArgumentException}.  The {@code contains} and {@code remove} methods
 * will also always return {@code false}.
 *
 * <p> This class provides overloads of the common {@code Set} operation with
 * the primitive {@code int} and {@code Integer} values.  Callers may use the
 * primitive overloads to avoid auto-boxing the {@code int} values.
 * Furthermore, this class provides overloads for the {@link #addAll(IntSet)
 * addAll}, {@link #removeAll(IntSet) removeAll}, and {@link #retainAll(IntSet)
 * retainAll} methos which operate on {@code IntSet} instances.  These
 * overloaded versions will still operate in {@code O(n)} time, but will be
 * <i>much</i> faster than the {@code Collection}-based versions.
 *
 * <p> This class also provides a {@link #wrap(BitSet)} method for wrapping an
 * existing {@link BitSet} as a {@link Set}, which acts as a bridge between
 * bit-based and Collections-based operations.
 *
 * <p> This class is not thread-safe.  This class and its iterator implement all
 * of the optional methods.
 *
 * @author David Jurgens
 */
public class IntSet extends AbstractSet<Integer> 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;
    
    /**
     * The bitset that contains the non-negative intergers in this set (include 0).
     */
    private final BitSet bitSet;

    /**
     * Creates a new, empty set of {@code int} values.
     */
    public IntSet() {
        this(new BitSet());
    }

    /**
     * Creates a new set backed by the provided bitset.
     *
     * @see #wrap(BitSet)
     */
    private IntSet(BitSet bitSet) {
        this.bitSet = bitSet;
    }

    /**
     * Creates a new set containing all of the specified {@code int} values.
     */
    public IntSet(Collection<Integer> ints) {
        this();
        for (Integer i : ints)
            bitSet.set(i);
    }

    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        if (i < 0)
            throw new IllegalArgumentException(
                "Cannot store negative values in an IntSet");
        boolean isPresent = bitSet.get(i);
        bitSet.set(i);
        return !isPresent;
    }

    /**
     * Adds to this set all of the elements that are contained in the specified
     * {@code IntSet} if not already present, using an {@code IntSet}-optimized
     * process.
     */
    public boolean addAll(IntSet ints) {
        int oldSize = size();
        bitSet.or(ints.bitSet);
        return oldSize != size();
    }

    public boolean contains(Integer i) {
        return contains(i.intValue());
    }

    public boolean contains(int i) {
        return i >= 0 && bitSet.get(i);
    }

    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    public Iterator<Integer> iterator() {
        return new BitSetIterator();
    }

    public boolean remove(Integer i) {
        return remove(i.intValue());
    }
    
    public boolean remove(int i) {
        if (i < 0)
            return false;
        boolean isPresent = bitSet.get(i);
        if (isPresent)
            bitSet.set(i, false);
        return isPresent;  
    }

    /**
     * Removes from this set all of the elements that are contained in the
     * specified {@code IntSet} using an {@code IntSet}-optimized process.
     */
    public boolean removeAll(IntSet ints) {
        int oldSize = size();
        bitSet.andNot(ints.bitSet);
        return oldSize != size();
    }

    /**
     * Retains only the elements in this set that are contained in the specified
     * {@code IntSet} using an {@code IntSet}-optimized process.
     */
    public boolean retainAll(IntSet ints) {
        int oldSize = size();
        bitSet.and(ints.bitSet);
        return oldSize != size();
    }

    public int size() {
        return bitSet.cardinality();
    }

    /**
     * Wraps the provided {@code BitSet} as a {@link Set} returning the result.
     * Any changes to the set will be reflected in {@code b} and vice-versa.
     */
    public static Set<Integer> wrap(BitSet b) {
        return new IntSet(b);
    }

    /**
     * An iterator over the integers in the backing {@code BitSet}.
     */
    private class BitSetIterator implements Iterator<Integer> {

        int next = -1;
        int cur = -1;

        public BitSetIterator() {
            advance();
        }

        private void advance() {
            if (next < -1)
                return;
            next = bitSet.nextSetBit(next + 1);
            // Keep track of when we finally go off the end
            if (next == -1)
                next = -2;
        }
        
        public boolean hasNext() {
            return next >= 0;
        }

        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            cur = next;
            advance();
            return cur;
        }

        public void remove() {            
            if (cur == -1)
                throw new IllegalStateException("Item already removed");
            bitSet.set(cur, false);
            cur = -1;
        }
    }
}