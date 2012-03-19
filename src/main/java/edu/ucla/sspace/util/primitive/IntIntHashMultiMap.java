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

import java.io.Serializable;

import edu.ucla.sspace.util.MultiMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import gnu.trove.TDecorators;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.procedure.TIntProcedure;

/**
 * A {@link MultiMap} implementation for mapping {@code int} primitives as both
 * keys and values using a hashing strategy.  This class offers a noticeable
 * performance improvement over the equivalent {@code
 * HashMultiMap&lt;Integer,Integer&gt;} by operating and representing the keys
 * and values only in their primitive state.
 */
public class IntIntHashMultiMap implements IntIntMultiMap {

    
    private static final long serialVersionUID = 1;

    /**
     * The backing map instance
     */
    private final TIntObjectMap<IntSet> map;

    /**
     * The number of values mapped to keys
     */
    private int range;

    public IntIntHashMultiMap() {
        map = new TIntObjectHashMap<IntSet>();
        range = 0;
    }
    
    /**
     * Constructs this map and adds in all the mapping from the provided {@code
     * Map}
     */
    public IntIntHashMultiMap(Map<Integer,Integer> m) {
        this();
        putAll(m);
    }

    public Map<Integer,Set<Integer>> asMap() {
        // An IntSet _is_ a Set<Integer>, but the JVM doesn't recognize
        // covaraince in the return type generics, so we must cast
        Map<Integer,?> m = TDecorators.wrap(map);
        @SuppressWarnings("unchecked")
        Map<Integer,Set<Integer>> m2 = (Map<Integer,Set<Integer>>)m;
        return m2;
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
    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        if (!(key instanceof Integer))
            return false;
        Integer k = (Integer)key;
        return containsKey(k.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsMapping(int key, int value) {
        IntSet s = map.get(key);
        return s != null && s.contains(value);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsMapping(Object key, Object value) {
        if (!(key instanceof Integer && value instanceof Integer))
            return false;
        Integer i = (Integer)key;
        Integer j = (Integer)value;
        return containsMapping(i.intValue(), j.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(int value) {
        for (IntSet s : map.valueCollection()) {
            if (s.contains(value)) {
                return true;
            }
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        if (!(value instanceof Integer))
            return false;
        Integer v = (Integer)value;
        return containsValue(v.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<Integer,Integer>> entrySet() {
        throw new Error();
        // return new EntryView();
    }

    /**
     * {@inheritDoc}
     */
    public IntSet get(int key) {
        IntSet vals = map.get(key);
        return (vals == null) ? new TroveIntSet() : vals;
    }

    /**
     * {@inheritDoc}
     */
    public IntSet get(Object key) {
        if (!(key instanceof Integer))
            return PrimitiveCollections.emptyIntSet();
        Integer k = (Integer)key;
        return get(k.intValue());
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
    public IntSet keySet() {
        return TroveIntSet.wrap(map.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public boolean put(int key, int value) {
        IntSet values = map.get(key);
        if (values == null) {
            values = new TroveIntSet();
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
    public boolean put(Integer key, Integer value) {
        return put(key.intValue(), value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends Integer,? extends Integer> m) {
        for (Map.Entry<? extends Integer,? extends Integer> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(MultiMap<? extends Integer,? extends Integer> m) {
        for (Map.Entry<? extends Integer,? extends Integer> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(IntIntMultiMap m) {
        // REMINDER: if IntIntMultiMap is every updated with a primitive based
        // EntrySet, use that
        IntIterator keys = m.keySet().iterator();
        while (keys.hasNext()) {
            int key = keys.nextInt();
            IntSet values = m.get(key);
            putMany(key, values);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean putMany(int key, Collection<Integer> values) {
        // Short circuit when adding empty values to avoid adding a key with an
        // empty mapping
        if (values.isEmpty())
            return false;
        IntSet vals = map.get(key);
        if (vals == null) {
            vals = new TroveIntSet(values.size());
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
    public boolean putMany(Integer key, Collection<Integer> values) {
        return putMany(key.intValue(), values);
    }

    /**
     * {@inheritDoc}
     */
    public boolean putMany(int key, IntCollection values) {
        // Short circuit when adding empty values to avoid adding a key with an
        // empty mapping
        if (values.isEmpty())
            return false;
        IntSet vals = map.get(key);
        if (vals == null) {
            vals = new TroveIntSet(values.size());
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
    public boolean putMany(Integer key, IntCollection values) {
        return putMany(key.intValue(), values);
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
    public IntSet remove(int key) {
        IntSet v = map.remove(key);
        if (v != null)
            range -= v.size();
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public IntSet remove(Integer key) {
        return remove(key.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(int key, int value) {
        IntSet values = map.get(key);
        // If the key was not mapped to any values
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
    public boolean remove(Object key, Object value) {
        if (!(key instanceof Integer && value instanceof Integer))
            return false;
        Integer i = (Integer)key;
        Integer j = (Integer)value;
        return remove(i.intValue(), j.intValue());
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
        TIntObjectIterator<IntSet> it = map.iterator();
         if (!it.hasNext()) {
             return "{}";
         }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        while (true) {
            int key = it.key();
            IntSet values = it.value();
            sb.append(key);
            sb.append("=[");
            IntIterator it2 = values.iterator();
            while (it2.hasNext()) {
                int value = it2.next();
                sb.append(value);
                if (it2.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
            if (!it.hasNext())
                return sb.append('}').toString();
            sb.append(", ");
            it.advance();
        }
    }

    /**
     * {@inheritDoc} The collection and its {@code Iterator} are backed by the
     * map, so changes to the map are reflected in the collection, and
     * vice-versa.
     */
    public IntCollection values() {
        throw new Error();
        // return new ValuesView();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<Set<Integer>> valueSets() {
        // An IntSet _is_ a Set<Integer>, but the JVM doesn't recognize
        // covaraince in the return type generics, so we must cast
        Collection<?> c = map.valueCollection();
        @SuppressWarnings("unchecked")
        Collection<Set<Integer>> c2 = (Collection<Set<Integer>>)c;            
        return c2;
    }

    /**
     * A {@link Collection} view of the values contained in a {@link MultiMap}.
     *
     * @see MultiMap#values()
     */
    class ValuesView implements TIntCollection, Serializable {

        private static final long serialVersionUID = 1;
        
        public ValuesView() { }

        public boolean add(int i) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends Integer> c) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int[] array) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(TIntCollection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            map.clear();
        }

        public boolean contains(int i) {
            return containsValue(i);
        }

        public boolean containsAll(int[] arr) {
            for (int i : arr)
                if (!containsValue(i))
                    return false;
            return true;
        }

        public boolean containsAll(Collection<?> c) {
            for (Object o : c) {
                if (!(o instanceof Integer)) 
                    return false;
                Integer i = (Integer)o;
                if (!containsValue(i))
                    return false;
            }
            return true;
        }

        public boolean containsAll(TIntCollection c) {
            throw new Error();
        }
        
        public boolean forEach(TIntProcedure p) {
            throw new Error();
        }

        public int getNoEntryValue() {
            throw new Error();
        }
        
        public int hashCode() {
            throw new Error();
        }

        public boolean isEmpty() {
            return IntIntHashMultiMap.this.isEmpty();
        }

        public TIntIterator iterator() {
            throw new Error();
        }
        
        public boolean remove(int i) {
            throw new UnsupportedOperationException();            
        }

        public boolean removeAll(Collection<?> c) {
            throw new Error();
        }

        public boolean removeAll(TIntCollection c) {
            throw new Error();
        }

        public boolean removeAll(int[] c) {
            throw new Error();
        }

        public boolean retainAll(Collection<?> c) {
            throw new Error();
        }

        public boolean retainAll(TIntCollection c) {
            throw new Error();
        }

        public boolean retainAll(int[] c) {
            throw new Error();
        }

        public int size() {
            return range();
        }

        public int[] toArray() {
            throw new Error();
        }

        public int[] toArray(int[] dest) {
            throw new Error();
        }
    }    

//     /**
//      * A {@link Set} view of the entries contained in a {@link MultiMap}.
//      *
//      * @see MultiMap#entrySet()
//      */
//     class EntryView extends AbstractSet<Map.Entry<K,V>> 
//             implements Serializable {

//         private static final long serialVersionUID = 1;
        
//         public EntryView() { }

//         /**
//          * {@inheritDoc}
//          */
//         public void clear() {
//             map.clear();
//         }

//         /**
//          * {@inheritDoc}
//          */
//         public boolean contains(Object o) {
//             if (o instanceof Map.Entry) {
//                 Map.Entry<?,?> e = (Map.Entry<?,?>)o;
//                 IntSet vals = TIntIntHashMultiMap.this.get(e.getKey());
//                 return vals.contains(e.getValue());
//             }
//             return false;
//         }
        
//         /**
//          * {@inheritDoc}
//          */
//         public Iterator<Map.Entry<K,V>> iterator() {
//             return new EntryIterator();
//         }

//         /**
//          * {@inheritDoc}
//          */
//         public int size() {
//             return range();
//         }
//     }

//     /**
//      * An iterator of all the key-value mappings in the multi-map.
//      */
//     class EntryIterator implements Iterator<Map.Entry<K,V>>, Serializable {
        
//         private static final long serialVersionUID = 1;

//         K curKey;
//         Iterator<V> curValues;
//         Iterator<Map.Entry<K,IntSet>> multiMapIterator;
        
//         Map.Entry<K,V> next;
//         Map.Entry<K,V> previous;
        
//         public EntryIterator() {
//             multiMapIterator = map.entrySet().iterator();
//             if (multiMapIterator.hasNext()) {
//                 Map.Entry<K,IntSet> e = multiMapIterator.next();
//                 curKey =  e.getKey();
//                 curValues = e.getValue().iterator();
//                 advance();
//             }

//         }
               
//         private void advance() {
//             // Check whether the current key has any additional mappings that
//             // have not been returned
//             if (curValues.hasNext()) {
//                 next = new MultiMapEntry(curKey, curValues.next());
//                 //System.out.println("next = " + next);
//             }
//             else if (multiMapIterator.hasNext()) {
//                 Map.Entry<K,IntSet> e = multiMapIterator.next();
//                 curKey =  e.getKey();
//                 curValues = e.getValue().iterator();
//                 // Assume that the map correct manages the keys and values such
//                 // that no key is ever mapped to an empty set
//                 assert curValues.hasNext() : "key is mapped to no values";
//                 next = new MultiMapEntry(curKey, curValues.next());
//                 //System.out.println("next = " + next);
//             } else {
//                 next = null;                
//             }
//         }

//         public boolean hasNext() {
//             return next != null;
//         }

//         public Map.Entry<K,V> next() {
//             Map.Entry<K,V> e = next;
//             previous = e;
//             advance();
//             return e;
//         }

//         public void remove() {
//             if (previous == null) {
//                 throw new IllegalStateException(
//                     "No previous element to remove");
//             }
//             TIntIntHashMultiMap.this.remove(previous.getKey(), previous.getValue());
//             previous = null;
//         }

//         /**
//          * A {@link Map.Entry} implementation that handles {@link MultiMap}
//          * semantics for {@code setValue}.
//          */
//         private class MultiMapEntry extends AbstractMap.SimpleEntry<K,V> 
//                 implements Serializable {
            
//             private static final long serialVersionUID = 1;
            
//             public MultiMapEntry(int key, int value) {
//                 super(key, value);
//             }

//             public V setValue(int value) {
//                 IntSet values = TIntIntHashMultiMap.this.get(getKey());
//                 values.remove(getValue());
//                 values.add(value);
//                 return super.setValue(value);
//             }
//         }
//     }
}
