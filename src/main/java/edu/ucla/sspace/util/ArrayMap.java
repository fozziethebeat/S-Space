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

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A {@link Map} implementation for integer keys, which backed by an array.
 * This class provides a bridge between array-based data structures and
 * Collections-based APIs that require mappings from integer keys to values.
 *
 * <p>This map does not permit {@code null} values or keys.  Furthermore, all
 * keys must be in the range of [0,l] where l is the length of the original
 * array.  Attempts to add new mappings outside this range will cause an {@link
 * IllegalArgumentException}, all other operations that exceed this boundary
 * will return {@code null} or {@code false} where appropriate.
 *
 * <p>All mutating methods to the entry set and iterators will throw {@link
 * UnsupportedOperationException} if called.
 *
 * <p>The {@link #size()} operation runs in linear time upon first call and is
 * constant time for all subsequent calls.
 *
 * <p>This map is not thread safe.
 */
public class ArrayMap<T> extends AbstractMap<Integer,T> 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final T[] array;

    /**
     * The number of mappings in this {@code Map} or {@code -1} if the number of
     * mappings has yet to be calculated.  The latter condition occurs when an
     * array is initially passed in and hasn't been checked for {@code null}
     * elements, which are considered non-existant mappings.
     */ 
    private int size;

    public ArrayMap(T[] array, int sizeOfMap) {
        // Make a copy?
        this.array = Arrays.copyOf(array, sizeOfMap);
        size = -1;
    }

    public ArrayMap(T[] array) {
        if (array == null)
            throw new NullPointerException();
        // Make a copy?
        this.array = array;
        size = -1;
    }

    public Set<Map.Entry<Integer,T>> entrySet() {
        return new EntrySet();
    }

    @Override public boolean containsKey(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            return i >= 0 && i < array.length && array[i] != null;
        }
        return false;
    }

    @Override public boolean containsValue(Object key) {
        for (T t : array)
            if (t != null && t.equals(key))
                return true;
        return false;
    }

    @Override public T get(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            return (i < 0 || i >= array.length)
                ? null
                : array[i];
        }
        return null;
    }

    /**
     * Returns the largest {@link Integer} key allowed by this map.
     */
    public int maxKey() {
        return array.length - 1;
    }

    /**  
     * 
     * @throws IllegalArgumentException if {@code key} < 0 or {@code key} >
     * maxKey(), or if {@code value} is {@code null}.
     */
    @Override public T put(Integer key, T value) {
        if (key < 0 || key >= array.length)
            throw new IllegalArgumentException(
                "key goes beyond bounds of the array backing this Map:" + key);
        if (value == null)
            throw new IllegalArgumentException("null values are not supported");
        T t = array[key];
        array[key] = value;
        // If we have computed the size and are adding a new element, then
        // update the number of mappings
        if (size >= 0 && t == null)
            size++;
        return t;
    }

    @Override public T remove(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            if (i < 0 || i >= array.length)
                return null;
            T t = array[i];
            array[i] = null;
            if (size >= 0 && t != null)
                size--;
            return t;
        }
        return null;
    }

    @Override public int size() {
        if (size == -1) {
            size = 0;
            for (T t : array) {
                if (t != null)
                    size++;
            }
            System.out.printf("array: %s, size: %d%n", Arrays.toString(array), size);
        }
        return size;
    }

    private class EntrySet extends AbstractSet<Map.Entry<Integer,T>> {        

        public boolean add(Map.Entry<Integer,T> e) {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                T t = get(e.getKey());
                return t != null && t.equals(e.getValue());
            }
            return false;
        }

        public Iterator<Map.Entry<Integer,T>> iterator() {
            return new EntryIterator();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return size;
        }
        
        private class EntryIterator implements Iterator<Map.Entry<Integer,T>> {

            private int i;

            private Map.Entry<Integer,T> next;

            public EntryIterator() {
                i = 0;
                advance();
            }
            
            public void advance() {
                next = null;
                while (i < array.length) {
                    T t = array[i];
                    i++;
                    if (t != null) {
                        next = new SimpleImmutableEntry<Integer,T>(i, t);
                        break;
                    }
                }
            }

            public boolean hasNext() {
                return next != null;
            }

            public Map.Entry<Integer,T> next() {
                if (!hasNext())
                    throw new IllegalStateException();
                Map.Entry<Integer,T> n = next;
                advance();
                return n;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

    }

}