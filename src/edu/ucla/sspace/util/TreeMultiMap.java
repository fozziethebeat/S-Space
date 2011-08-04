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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A Red-Black tree {@link SortedMultiMap} implementation.  Map elements are
 * sorted according to the {@link Comparable natural ordering} of the keys, or
 * by a {@link Comparator} provided to the constructor.<p>
 *
 * This implementation provides guaranteed log(n) time cost for the {@code
 * containsKey}, {@code get}, {@code put} and {@code remove} operations.  The
 * remaining operations run in constant time.  The only exception is the {@code
 * range} method, which runs in constant time except for the first time it is
 * invoked on any sub-map returned by {@code headMap}, {@code tailMap}, or
 * {@code subMap}, when it runs in linear time.<p>
 *
 * This map is not thread-safe.
 * 
 * @see Map
 * @see SortedMap
 * @see HashMultiMap
 * @see MultiMap
 */
public class TreeMultiMap<K,V> 
        implements SortedMultiMap<K,V>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The backing map instance
     */
    private final SortedMap<K,Set<V>> map;

    /**
     * The number of values mapped to keys
     */
    private int range;
    
    /**
     * Whether the current range needs to be recalcuated before the next {@link
     * #range()} call can return the correct result.  An invalid range is the
     * result of a submap being created.
     */
    private boolean recalculateRange;

    /**
     * The super-map of this instance if it is a sub map, or {@code null} if
     * this is the original map.
     */
    private final TreeMultiMap<K,V> parent;

    /**
     * Constructs this map using the natural ordering of the keys.
     */
    public TreeMultiMap() {
	map = new TreeMap<K,Set<V>>();
	range = 0;
	recalculateRange = false;
	parent = null;
    }

    /**
     * Constructs this map where keys will be sorted according to the provided
     * comparator.
     */
    public TreeMultiMap(Comparator<? super K> c) {
	map = new TreeMap<K,Set<V>>(c);
	range = 0;
	recalculateRange = false;
	parent = null;
    }

    /**
     * Constructs this map using the natural ordering of the keys, adding all of
     * the provided mappings to this map.
     */
    public TreeMultiMap(Map<? extends K,? extends V> m) {
	this();
	putAll(m);
    }

    /**
     * Constructs a subset of an existing map using the provided map as the
     * backing map.
     *
     * @see #headMap(Object)
     * @see #subMap(Object,Object)
     * @see #tailMap(Object)
     */
    private TreeMultiMap(SortedMap<K,Set<V>> subMap, TreeMultiMap<K,V> parent) {
	map = subMap;
	// need to compute the range, but lazily compute this on demand.
	// Calculuating it now turns the subMap operations into O(n) instead of
	// the O(1) call it currently is.
	range = -1;
	recalculateRange = true;
	this.parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
	map.clear();
	if (parent != null) {
	    int curRange = range();
	    parent.updateParentRange(-curRange);
	}
	range = 0;
    }

    /**
     * Recursively updates the range value for the the super-map of this sub-map
     * if it exists.
     */
    private void updateParentRange(int valueDifference) {
	range += valueDifference;
	if (parent != null) {
	    parent.updateParentRange(valueDifference);
	}
    }

    /**
     * {@inheritDoc}
     */
    public Comparator<? super K> comparator() {
	return map.comparator();
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
    public K firstKey() {
	return map.firstKey();
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
    public SortedMultiMap<K,V> headMap(K toKey) {
	return new TreeMultiMap<K,V>(map.headMap(toKey), this);
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
    public K lastKey() {
	return map.lastKey();
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
	    if (parent != null) {
		parent.updateParentRange(1);
	    }
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
	    vals = new HashSet<V>();
	    map.put(key, vals);
	}
	int oldSize = vals.size();
	boolean added = vals.addAll(values);
	range += (vals.size() - oldSize);
	return added;
    }

    /**
     * {@inheritDoc} This method runs in constant time, except for the first
     * time called on a sub-map, when it runs in linear time to the number of
     * values.
     */
    public int range() {
	// the current range isn't accurate, loop through the values and count
	if (recalculateRange) {
	    recalculateRange = false;
	    range = 0;
	    for (V v : values()) 
		range++;
	}
	return range;
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> remove(Object key) {
	Set<V> v = map.remove(key);
	if (v != null)
	    range -= v.size();
	if (parent != null) {
	    parent.updateParentRange(-(v.size()));
	}

	return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean remove(K key, V value) {
	Set<V> values = map.get(key);
	boolean removed = values.remove(value);
	if (removed) {
	    range--;
	    if (parent != null) {
		parent.updateParentRange(-1);
	    }
	}
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
     * {@inheritDoc}
     */
    public SortedMultiMap<K,V> subMap(K fromKey, K toKey) {
	return new TreeMultiMap<K,V>(map.subMap(fromKey, toKey), this);
    }

    /**
     * {@inheritDoc}
     */
    public SortedMultiMap<K,V> tailMap(K fromKey) {
	return new TreeMultiMap<K,V>(map.tailMap(fromKey), this);
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
     * A {@link Collection} view of the values contained in a {@link MultiMap}.
     *
     * @see MultiMap#values()
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
		Set<V> vals = TreeMultiMap.this.get(e.getKey());
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
	    }

	    advance();
	}
	       
	private void advance() {
	    // Check whether the current key has any additional mappings that
	    // have not been returned
	    if (curValues != null && curValues.hasNext()) {
		next = new MultiMapEntry(curKey, curValues.next());
		//System.out.println("next = " + next);
	    }
	    else if (multiMapIterator.hasNext()) {
		Map.Entry<K,Set<V>> e = multiMapIterator.next();
		curKey =  e.getKey();
		curValues = e.getValue().iterator();
		// Assume that the map correct manages the keys and values such
		// that no key is ever mapped to an empty set
		next = new MultiMapEntry(curKey, curValues.next());
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
	    TreeMultiMap.this.remove(previous.getKey(), previous.getValue());
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
		Set<V> values = TreeMultiMap.this.get(getKey());
		values.remove(getValue());
		values.add(value);
		return super.setValue(value);
	    }
	}
    }




}
