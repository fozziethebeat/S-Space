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

import java.io.Serializable;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A hash table based implementation of the {@code MultiMap} interface.  This
 * implementation permits both {@code null} keys and values. 
 *
 * <p>
 *
 * This implementation provides constant time operations for the basic
 * operations {@code put} and {@code get}, assuming a uniform distribution of
 * hash keys.
 *
 * @see HashMap
 */
public class HashMultiMap<K,V> implements MultiMap<K,V>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The backing map instance
     */
    private final Map<K,Set<V>> map;

    /**
     * The number of values mapped to keys
     */
    private int range;

    public HashMultiMap() {
        map = new HashMap<K,Set<V>>();
        range = 0;
    }
    
    /**
     * Constructs this map and adds in all the mapping from the provided {@code
     * Map}
     */
    public HashMultiMap(Map<? extends K,? extends V> m) {
        this();
        putAll(m);
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        map.clear();
        range = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        for (Set<V> s : map.values()) {
            if (s.contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<K,V>> entrySet() {
        return new EntryView();
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> get(Object key) {
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return map.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(K key, V value) {
        Set<V> values = map.get(key);
        if (values == null) {
            values = new HashSet<V>();
            map.put(key, values);
        }
        boolean added = values.add(value);
        if (added) {
            range++;
        }
        return added;
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K,? extends V> m) {
        for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(MultiMap<? extends K,? extends V> m) {
        for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putMulti(K key, Collection<V> values) {
        // Short circuit when adding empty values to avoid adding a key with an
        // empty mapping
        if (values.isEmpty())
            return false;
        Set<V> vals = map.get(key);
        if (vals == null) {
            vals = new HashSet<V>(values.size());
            map.put(key, vals);
        }
        int oldSize = vals.size();
        boolean added = vals.addAll(values);
        range += (vals.size() - oldSize);
        return added;
    }

    /**
     * {@inheritDoc}
     */
    public int range() {
        return range;
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> remove(Object key) {
        Set<V> v = map.remove(key);
        if (v != null)
            range -= v.size();
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(K key, V value) {
        Set<V> values = map.get(key);
        // If the value has already been removed, return early.
        if (values == null)
            return false;

        boolean removed = values.remove(value);
        if (removed)
            range--;
        // if this was the last value mapping for this key, remove the
        // key altogether
        if (values.size() == 0)
            map.remove(key);
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns the string form of this multi-map
     */
    public String toString() {
        Iterator<Map.Entry<K,Set<V>>> it = map.entrySet().iterator();
        if (!it.hasNext()) {
            return "{}";
        }
   
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            Map.Entry<K,Set<V>> e = it.next();
            K key = e.getKey();
            Set<V> values = e.getValue();
            sb.append(key   == this ? "(this Map)" : key);
            sb.append("=[");
            Iterator<V> it2 = values.iterator();
            while (it2.hasNext()) {
                V value = it2.next();
                sb.append(value == this ? "(this Map)" : value);
                if (it2.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
            if (!it.hasNext())
                return sb.append('}').toString();
            sb.append(", ");
        }
    }

    /**
     * {@inheritDoc} The collection and its {@code Iterator} are backed by the
     * map, so changes to the map are reflected in the collection, and
     * vice-versa.
     */
    public Collection<V> values() {
        return new ValuesView();
    }

    /**
     * A {@link Collection} view of the values contained in a {@link MultiMap}.
     *
     * @see MultiMap#values()
     */
    class ValuesView extends AbstractCollection<V> implements Serializable {

        private static final long serialVersionUID = 1;
        
        public ValuesView() { }

        /**
         * {@inheritDoc}
         */
        public void clear() {
            map.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(Object o) {
            return containsValue(o);
        }
        
        /**
         * {@inheritDoc}
         */
        public Iterator<V> iterator() {
            // combine all of the iterators for the entry sets
            Collection<Iterator<V>> iterators = 
                new ArrayList<Iterator<V>>(size());
            for (Set<V> s : map.values()) {
                iterators.add(s.iterator());
            }
            // NOTE: because the iterators are backed by the internal map and
            // because the CombinedIterator class supports remove() if the
            // backing class does, calls to remove from this iterator are also
            // supported.
            return new CombinedIterator<V>(iterators);
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return range();
        }
    }    

    /**
     * A {@link Set} view of the entries contained in a {@link MultiMap}.
     *
     * @see MultiMap#entrySet()
     */
    class EntryView extends AbstractSet<Map.Entry<K,V>> 
            implements Serializable {

        private static final long serialVersionUID = 1;
        
        public EntryView() { }

        /**
         * {@inheritDoc}
         */
        public void clear() {
            map.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                Set<V> vals = HashMultiMap.this.get(e.getKey());
                return vals.contains(e.getValue());
            }
            return false;
        }
        
        /**
         * {@inheritDoc}
         */
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return range();
        }
    }

    /**
     * An iterator of all the key-value mappings in the multi-map.
     */
    class EntryIterator implements Iterator<Map.Entry<K,V>>, Serializable {
        
        private static final long serialVersionUID = 1;

        K curKey;
        Iterator<V> curValues;
        Iterator<Map.Entry<K,Set<V>>> multiMapIterator;
        
        Map.Entry<K,V> next;
        Map.Entry<K,V> previous;
        
        public EntryIterator() {
            multiMapIterator = map.entrySet().iterator();
            if (multiMapIterator.hasNext()) {
                Map.Entry<K,Set<V>> e = multiMapIterator.next();
                curKey =  e.getKey();
                curValues = e.getValue().iterator();
                advance();
            }

        }
               
        private void advance() {
            // Check whether the current key has any additional mappings that
            // have not been returned
            if (curValues.hasNext()) {
                next = new MultiMapEntry(curKey, curValues.next());
                //System.out.println("next = " + next);
            }
            else if (multiMapIterator.hasNext()) {
                Map.Entry<K,Set<V>> e = multiMapIterator.next();
                curKey =  e.getKey();
                curValues = e.getValue().iterator();
                // Assume that the map correct manages the keys and values such
                // that no key is ever mapped to an empty set
                assert curValues.hasNext() : "key is mapped to no values";
                next = new MultiMapEntry(curKey, curValues.next());
                //System.out.println("next = " + next);
            } else {
                next = null;                
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public Map.Entry<K,V> next() {
            Map.Entry<K,V> e = next;
            previous = e;
            advance();
            return e;
        }

        public void remove() {
            if (previous == null) {
                throw new IllegalStateException(
                    "No previous element to remove");
            }
            HashMultiMap.this.remove(previous.getKey(), previous.getValue());
            previous = null;
        }

        /**
         * A {@link Map.Entry} implementation that handles {@link MultiMap}
         * semantics for {@code setValue}.
         */
        private class MultiMapEntry extends AbstractMap.SimpleEntry<K,V> 
                implements Serializable {
            
            private static final long serialVersionUID = 1;
            
            public MultiMapEntry(K key, V value) {
                super(key, value);
            }

            public V setValue(V value) {
                Set<V> values = HashMultiMap.this.get(getKey());
                values.remove(getValue());
                values.add(value);
                return super.setValue(value);
            }
        }
    }

}
