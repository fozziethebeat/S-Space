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

import java.util.Collection;
import java.util.Collections;
import java.util.Random;
import java.util.Set;


/**
 * A collection of static utility methods for working with primitive
 * collections.  This class is analogous to {@link Collections}.
 */
public final class PrimitiveCollections {
    
    /**
     * A class local source of randomness, if one is not provided by the caller.
     */
    private static final Random RANDOM = new Random();

    // FIXME
    private static final IntSet EMPTY_INT_SET = 
        new UnmodifiableIntSet(new TroveIntSet());

    /**
     * Returns an immutable, empty {@link IntSet}.
     */
    public static IntSet emptyIntSet() {
        return EMPTY_INT_SET;
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(int[] arr) {
        shuffle(arr, RANDOM);
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(int[] arr, Random rand) {
        int size = arr.length;
        for (int i = size; i > 1; i--) {
            int tmp = arr[i-1];
            int r = rand.nextInt(i);
            arr[i-1] = arr[r];
            arr[r] = tmp;
        }
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(double[] arr) {
        shuffle(arr, RANDOM);
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(double[] arr, Random rand) {
        int size = arr.length;
        for (int i = size; i > 1; i--) {
            double tmp = arr[i-1];
            int r = rand.nextInt(i);
            arr[i-1] = arr[r];
            arr[r] = tmp;
        }
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(long[] arr) {
        shuffle(arr, RANDOM);
    }

    /**
     * Randomly shuffles the contents of the provided array
     */
    public static void shuffle(long[] arr, Random rand) {
        int size = arr.length;
        for (int i = size; i > 1; i--) {
            long tmp = arr[i-1];
            int r = rand.nextInt(i);
            arr[i-1] = arr[r];
            arr[r] = tmp;
        }
    }
    
    /**
     * Returns an immuable view of the provided {@link IntSet}.
     */
    public static IntSet unmodifiableSet(IntSet s) {
        return new UnmodifiableIntSet(s);
    }

    
    private static class UnmodifiableIntSet extends AbstractIntSet {

        private final IntSet set;

        public UnmodifiableIntSet(IntSet set) {
            this.set = set;
        }

        public boolean contains(Integer i) {
            return set.contains(i);
        }
            
        public boolean contains(int i) {
            return set.contains(i);
        }

        public boolean containsAll(IntSet c) {
            return set.containsAll(c);
        }

        public boolean containsAll(Collection<?> c) {
            return set.containsAll(c);
        }

        public boolean isEmpty() {
            return set.isEmpty();
        }

        public IntIterator iterator() {
            return new UnmodifiableIntIterator(set.iterator());
        }

        public int size() {
            return set.size();
        }
    }

    private static class UnmodifiableIntIterator implements IntIterator {

        private final IntIterator iter;

        public UnmodifiableIntIterator(IntIterator iter) {
            this.iter = iter;
        }

        public boolean hasNext() {
            return iter.hasNext();
        }

        public int nextInt() {
            return iter.nextInt();
        }

        public Integer next() {
            return iter.next();
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}