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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A space-optimized map for associating {@code char} keys with values.<p>
 *
 * This class makes a trade off for reduced space usage at the cost of decreased
 * performace.  The {@code put}, {@code get}, {@code containsKey} and {@code
 * get} operations are all logarithmic when the map is unmodified.  If a new
 * mapping is added, or one is removed, the operation is linear in the number of
 * mappings.  Both {@code size} and {@code isEmpty} are still constant time. <p>
 *
 * The {@code get} operation runs in logarithmic time.  The {@code set}
 * operation runs in consant time if setting an existing non-zero value to a
 * non-zero value.  However, if the {@code set} invocation sets a zero value to
 * non-zero, the operation is linear with the size of the array.<p>
 *
 * This map allows {@code null values}.<p>
 *
 * Iteration ordering follows the natural ordering of {@code char} values, with
 * the lowest {@code char} key value being returned first.
 *
 * <i>Implementation Note:</i> the {@code Iterator.remove()} method is currently
 * unsupported and will throw an exception when called.  However, a future
 * implementation will fix this.<p>
 *
 * @see TrieMap
 * @see IntegerMap
 * @see Map
 * 
 * @author David Jurgens
 */
public class CharMap<V> extends AbstractMap<Character,V> 
        implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * The keys stored in this map, in sorted order.  The index at which a key
     * is found corresponds to the index at which its value is found.
     */
    char[] keyIndices;

    /**
     * The values stored in this map.
     */
    Object[] values;

    /**
     * Creates a new map.
     */
    public CharMap() {
	keyIndices = new char[0];
	values = new Object[0];
    }

    /**
     * Creates a new map with the mappings contained in {@code m}.
     */
    public CharMap(Map<Character,? extends V> m) {
	// Pre-allocate the arrays for all the mappings needed instead of just
	// one at time
	keyIndices = new char[m.size()];
	values = new Object[m.size()];

	// Get all of the integer keys and sort them to their correct position
	// in the key array
	Iterator<Character> it = m.keySet().iterator();
	for (int i = 0; i < m.size(); ++i) {
	    keyIndices[i] = it.next();
	}
	Arrays.sort(keyIndices);

	// Then map the values to their respective positions
	for (int i = 0; i < keyIndices.length; ++i) {
	    values[i] = m.get(keyIndices[i]);
	}
    }
    
    /**
     * Checks that the key is non-{@code null} and is an {@code Integer} object,
     * and then returns its {@code int} value.
     */
    private char checkKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	} else if (!(key instanceof Character)) {
	    throw new IllegalArgumentException("key must be an Character");
	}
	else {
	    return ((Character)key).charValue();
	}
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
	keyIndices = new char[0];
	values = new Object[0];
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     */
    public boolean containsKey(Object key) {
	char k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);
	return index >= 0;
    }

    public boolean containsValue(Object value) {
	for (Object o : values) {
	    if (o == value || (o != null && o.equals(value))) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     */
    public Set<Entry<Character,V>> entrySet() {
	return new EntrySet();
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */  
    public V get(Object key) {
	char k = checkKey(key);
	return get(k);
    }


    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    @SuppressWarnings("unchecked")  
    public V get(char key) {
	int pos = Arrays.binarySearch(keyIndices, key);
	return (pos >= 0) ? (V)(values[pos]) : null;	
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     */
    public Set<Character> keySet() {
	return new KeySet();
    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerException if the key is {@code null}
     * @throws IllegalArgumentException if the key is not an instance of {@link
     *         Integer}
     */
    public V put(Character key, V value) {
	char k = checkKey(key);
        return put(k, value);
    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerException if the key is {@code null}
     * @throws IllegalArgumentException if the key is not an instance of {@link
     *         Integer}
     */
    @SuppressWarnings("unchecked")
    public V put(char key, V value) {
        char k = key;
	int index = Arrays.binarySearch(keyIndices, k);

	if (index >= 0) {
	    V old = (V)(values[index]);
	    values[index] = value;
	    return old;
	}
	else {
	    int newIndex = 0 - (index + 1);	    
	    Object[] newValues = Arrays.copyOf(values, values.length + 1);
	    char[] newIndices = Arrays.copyOf(keyIndices, values.length + 1);

	    // shift the elements down to make room for the new value
	    for (int i = newIndex; i < values.length; ++i) {
		newValues[i+1] = values[i];
		newIndices[i+1] = keyIndices[i];
	    }

	    // insert the new value
	    newValues[newIndex] = value;
	    newIndices[newIndex] = k;

	    // switch the arrays with the lengthed versions
	    values = newValues;
	    keyIndices = newIndices;
	    
	    return null;
	}
    }

    /**
     * Removes the mapping for a key from this map if it is present and returns
     * the value to which this map previously associated the key, or {@code
     * null} if the map contained no mapping for the key.
     *
     * @param key key whose mapping is to be removed from the map 
     *
     * @return the previous value associated with key, or {@code null} if there
     * was no mapping for key.
     */
    @SuppressWarnings("unchecked")    
    public V remove(Object key) {
	char k = checkKey(key);
        return remove(k);
    }

    /**
     * Removes the mapping for a key from this map if it is present and returns
     * the value to which this map previously associated the key, or {@code
     * null} if the map contained no mapping for the key.
     *
     * @param key key whose mapping is to be removed from the map 
     *
     * @return the previous value associated with key, or {@code null} if there
     * was no mapping for key.
     */
    @SuppressWarnings("unchecked")    
    public V remove(char key) {
        char k = key;
	int index = Arrays.binarySearch(keyIndices, k);

	if (index >= 0) {
	    V old = (V)(values[index]);

	    Object[] newValues = Arrays.copyOf(values, values.length - 1);
	    char[] newIndices = Arrays.copyOf(keyIndices, keyIndices.length - 1);

	    // shift the elements up to remove the values
	    for (int i = index; i < values.length - 1; ++i) {
		newValues[i] = values[i+1];
		newIndices[i] = keyIndices[i+1];
	    }

	    // update the arrays with the shorted versions
	    values = newValues;
	    keyIndices = newIndices;
	    return old;
	}

	return null;
    }

    /**
     * Returns the number of key-value mappings in this trie.
     */
    public int size() {
	return keyIndices.length;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values() {
	return new Values();
    }


    private class EntryIterator extends CharMapIterator<Map.Entry<Character,V>> {

	public Map.Entry<Character,V> next() {
	    return nextEntry();
	}
	
    }

    private class KeyIterator extends CharMapIterator<Character> {

	public Character next() {
	    return nextEntry().getKey();
	}
	
    }

    private class ValueIterator extends CharMapIterator<V> {

	public V next() {
	    return nextEntry().getValue();
	}
	
    }

    abstract class CharMapIterator<E> implements Iterator<E> {

	private int next;

	public CharMapIterator() {
	    next = 0;
	}

	public boolean hasNext() {
	    return next < size();
	}

	@SuppressWarnings("unchecked")
	public Entry<Character,V> nextEntry() {
	    if (next >= size()) {
		throw new NoSuchElementException("no further elements");
	    }
	    char key = keyIndices[next];
	    V value = (V)(values[next]);
	    next++;
	    return new CharEntry(key, value);
	}
	
	// REMINDER: this class needs to work with the actual indices for the
	// key and value to avoid the logrithmic lookup
	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    // REMINDER: this class needs to work with the actual indices for the key
    // and value to avoid the logrithmic lookup
    class CharEntry extends SimpleEntry<Character,V> {

	private static final long serialVersionUID = 1L;
	
	public CharEntry(char key, V value) {
	    super(key, value);
	}

	public V setValue(V newValue) {
	    return CharMap.this.put(getKey(), newValue);
	}
    }

    class EntrySet extends AbstractSet<Entry<Character,V>> {

	private static final long serialVersionUID = 1L;

	public void clear() {
	    CharMap.this.clear();
	}

	public boolean contains(Object o) {
	    if (o instanceof Map.Entry) {
		Map.Entry e = (Map.Entry)o;
		Object key = e.getKey();
		Object val = e.getValue();
		Object mapVal = CharMap.this.get(key);
		return mapVal == val || (val != null && val.equals(mapVal));
	    }
	    return false;
	}

	public Iterator<Map.Entry<Character,V>> iterator() {
	    return new EntryIterator();
	}
	
	public int size() {
	    return CharMap.this.size();
	}
    }

    class KeySet extends AbstractSet<Character> {

	private static final long serialVersionUID = 1L;
	
	public KeySet() { }
	
	public void clear() {
	    CharMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsKey(o);
	}

	public Iterator<Character> iterator() {
	    return new KeyIterator();
	}
	
	public boolean remove(Object o) {
	    return CharMap.this.remove(o) != null;
	}

	public int size() {
	    return CharMap.this.size();
	}

    }

    /**
     * A {@link Collection} view of the values contained in this trie.
     */
    private class Values extends AbstractCollection<V> {

	private static final long serialVersionUID = 1L;
	
	public void clear() {
	    CharMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsValue(o);
	}
	
	public Iterator<V> iterator() {
	    return new ValueIterator();
	}
	
	public int size() {
	    return CharMap.this.size();
	}
    }
    
}