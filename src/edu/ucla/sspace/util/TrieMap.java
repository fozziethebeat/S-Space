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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A trie-based {@link Map} implementation that uses {@link String} instances as
 * keys.  Keys are returned in alphabetical order.  This class is optimized for
 * space efficiency.  For a string of length {@code n}, operations are at worst
 * case {@code n * log(k)}, where {@code k} is the number of unique characters.
 * In most cases, the cost will be much closer to linear in the number of
 * characters in the string.
 *
 * <p>
 *
 * This class does not permit {@code null} keys or values.  However, this class
 * does permit the use of the empty string (a {@code String} of length
 * {@code 0}).
 *
 * <p>
 *
 * This class is not synchronized.  If concurrent updating behavior is required,
 * the map should be wrapped using {@link
 * java.util.Collections#synchronizedMap(Map)}.  This map will never throw a
 * {@link java.util.ConcurrentModificationException} during iteration.  The
 * behavior is unspecified if the map is modified while an iterator is being
 * used.
 *
 * @author David Jurgens
 */
public class TrieMap<V> extends AbstractMap<String,V> 
        implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * The root node of this trie
     */
    private final RootNode<V> rootNode;

    /**
     * The size of this trie
     */
    private int size = 0;
    
    /**
     * Constructs an empty trie
     */
    public TrieMap() {
	// create the root mapping with an alphabetically sorted order
	rootNode = new RootNode<V>();
	size = 0;
    }

    /**
     * Constructs this trie, adding all of the provided mappings
     */
    public TrieMap(Map<String,? extends V> m) {
	this();
	if (m == null) {
	    throw new NullPointerException("map cannot be null");
	}
	putAll(m);
    }

    /**
     * Throws the appropriate {@code Exception} if the provided key is {@code
     * null}, is not an instance of {@code String}, or is the empty
     * string.
     */
    private void checkKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("keys cannot be null");
	}
	if (!(key instanceof String)) {
	    throw new ClassCastException("key not an instance of String");
	}
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
	rootNode.clear();
	size = 0;
    }
    
    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @throws NullPointerException if key is {@code null}
     * @throws ClassCastException if key is not an instance of {@link
     *         String}
     */
    public boolean containsKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	}
	else if (key instanceof String) {
	    Node<V> n = lookup((String)key);
	    return n != null && n.isTerminal();
	}
	else {
	    throw new ClassCastException("The provided key does not implement" +
					 " String: " + key);
	}
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     */
    public Set<Map.Entry<String,V>> entrySet() {
	return new EntryView();
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    public V get(Object key) {
	checkKey(key);
	
	String cs = (String)key;
	Node<V> n = lookup(cs);
	return (n == null) ? null : n.value;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     */
    public Set<String> keySet() {
	return new KeyView();
    }

    /**
     * Returns the trie node that maps to the provided key or {@code null} if
     * the key is not currently mapped.
     */
    private Node<V> lookup(String key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	}

	int keyLength = key.length();
        // Base case: the node to start the recursive search begins at the root
        // node
	Node<V> n = rootNode;

        // For each index in the key, see how many characters match either the
        // preceding characters to a node, or the trailing characters.
	for (int curKeyIndex = 0; curKeyIndex <= keyLength; ++curKeyIndex) {	    
	    
	    CharSequence nodePrefix = n.getPrefix();

	    // Check whether the current node has a prefix (i.e. the characters
	    // in the trie that lead to it).  If so, determine whether all of
	    // the prefix matches the subsequence of the key starting at the
	    // current key index.  Note that a node will only have a prefix if
	    // it is an intermediate node in the tree.
	    if (nodePrefix.length() > 0) {
		int charOverlap = countOverlap(key, curKeyIndex, nodePrefix, 0);
		int prefixLength = nodePrefix.length();

		// If this this key did not match then entire prefix, then it
		// must not be mapped to any node.
		if (charOverlap < prefixLength) {
		    return null;
		}

		// Otherwise, if all of the characters overlapped, then lookup
		// the transition to the next node based on the next character
		// after the matching prefix
		curKeyIndex += prefixLength;
	    }

	    // If we have exhausted all the characters in the key, then the
	    // current node is associated with the key.
	    if (curKeyIndex == keyLength) {
		return n;
	    }
	    	    
	    // Otherwise, more characters exist to be matched in the key, so
	    // check to see if there is a transition from the next sequence of
	    // the key to node.  If so, we use this to keep searching for the
	    // key's node
	    else {
		Node<V> child = n.getChild(key.charAt(curKeyIndex));

		// if there was no other node to transition to, then the the key
		// must not map to any node in the trie.
		if (child == null) {
		    return null;
		}

		// otherwise, update the current node to the child and repeat
		// the search process
		else {
		    n = child;
		}
	    }	    
	}
	
	// NOTE: we should never reach this case, as the one of the conditions
	// in the for loop will determine where the key goes.
        assert false;
	return null;
    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    public V put(String key, V value) {

	if (key == null || value == null) {
	    throw new NullPointerException("keys and values cannot be null");
	}

	int keyLength = key.length();
	Node<V> n = rootNode;

	for (int curKeyIndex = 0; curKeyIndex <= keyLength; ++curKeyIndex) {	    

	    CharSequence nodePrefix = n.getPrefix();

	    int nextCharIndex = curKeyIndex + 1;

	    // if the current node is an intermediate node, then we need to
	    // match all of the prefix characters to use its children.  
	    if (nodePrefix.length() > 0) {
		int charOverlap = countOverlap(key, curKeyIndex, nodePrefix,0);
		int prefixLength = nodePrefix.length();
		
		// if 0 ore more characters overlapped, add this node to
		// somewhere in the middle
		if (charOverlap < prefixLength) {
		    addIntermediateNode(n,
					charOverlap,
					key,
					curKeyIndex,
					value);
		    size++;
		    return null;
		}

		// if all of the characters overlapped, then lookup the
		// transition to the next node based on the next character after
		// the matching prefix
		curKeyIndex += prefixLength;
		nextCharIndex = curKeyIndex + 1;
	    }

	    // If we have exhausted all the characters in the key, then the
	    // current node should map to the value
	    if (curKeyIndex == keyLength) {
		return replaceValue(n, value);
	    }
	    	    
	    // Otherwise, more characters exist, so check to see if there is a
	    // transition from the next sequence of the key to node.  If so, we
	    // use this to keep searching for the key's node
	    else {
		Node<V> child = n.getChild(key.charAt(curKeyIndex));
		
		// if there was no other node to transition to, then the
		// remaining portion of the key is used to form a child node of
		// the current node.  Since this is a new mapping, we can return
		// null immediately.
		if (child == null) {
		    addChildNode(n, key, curKeyIndex, value);
		    return null;
		}
		// otherwise, update the current node to the child and repeat
		// the search process
		else {
		    n = child;
		}
	    }	    
	}

	// NOTE: we should never reach this case, as the one of the conditions
	// in the for loop will determine where the key goes.
	return null;
    }

    /**
     * Returns the number of overlapping characters between {@code c1} and
     * {@code c2} starting and the provided indices into the sequences.
     *
     * @param c1 a character sequence
     * @param start1 the index into {@code c1} at which the overlap test should
     *        start
     * @param c2 a character sequence
     * @param start2 the index into {@code c2} at which the overlap test should
     *        start
     *
     * @return the number of characters shared by both sequences when viewed at
     *         the provided starting indices.
     */
    private int countOverlap(CharSequence c1, int start1, 
                             CharSequence c2, int start2) {
        // The maxium overlap is the number of characters in the sequences
	int maxOverlap = Math.min(c1.length() - start1, c2.length() - start2);
	int overlap = 0;
	for (; overlap < maxOverlap; ++overlap) {
	    if (c1.charAt(overlap + start1) != c2.charAt(overlap + start2)) {
		break;
	    }
	}
	return overlap;
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
    public V remove(Object key) {
	checkKey(key);
	
	String cs = (String)key;
	Node<V> n = lookup(cs);
	if (n != null && n.isTerminal()) {
	    V old = n.value;
	    n.value = null;
	    size--;
	    return old;	    
	}
	else {
	    return (n == null) ? null : n.value;
	}
    }
    
    /**
     * Replaces the value of the provided {@code Node} and returns the old value
     * or {@code null} if one was not set.
     *
     * @param node
     * @param newValue
     *
     * @return
     */
    private V replaceValue(Node<V> node, V newValue) {
        // Note: a node is terminal if it already has a value
	if (node.isTerminal()) {
	    V old = node.value;
	    node.value = newValue;
	    return old;
	}
	// the node wasn't already a terminal node (i.e. this char sequence is a
	// substring of an existing sequence), assign it a value
	else {
	    node.value = newValue;
	    size++;
	    return null; // no old value
	}
    }

    /**
     * Returns the number of key-value mappings in this trie.
     */
    public int size() {
	return size;
    }


    /**
     * Adds a child {@link Node} node to the provided parent using the {@code
     * char} at the transition index to determine the link.
     *
     * @param parent the node to which the child will be added
     * @param key the key that is being mapped to the provided value
     * @param transitionCharIndex the character index in {@key} to which the new
     *        node should be linked from the parent node
     * @param value the value being mapped to the provided key
     */
    private void addChildNode(Node<V> parent,
			      String key,
			      int transitionCharIndex,
			      V value) {
	char transitionChar = key.charAt(transitionCharIndex);
	Node<V> child = new Node<V>(key, transitionCharIndex + 1, value);
	parent.addChild(transitionChar, child);
	size++;
    }

    /**
     * Creates a series of children under the provided node, moving the value
     * that was mapped to this node to the appropriate terminal node in the
     * series and finally creating a new node at the end to hold the new
     * key-value mapping.
     *
     * @param node a node under which a new key-value mapping is being added
     * @param nodesToCreate the number of characters that overlap between the
     *        key that maps to {@code} and the new key.  This determines the
     *        number of nodes that are needed to distinguish the two keys.
     * @param newTail the remaining portion of the new key that is being added,
     *        which will be appended to a new child node at the end of the
     *        sequence.
     * @param value the value for the new key-value mapping being added to the
     *        map
     */
    private void addIntermediateNode(Node<V> original, 
				     int numOverlappingCharacters,
				     String key, 
				     int indexOfStartOfOverlap,
				     V value) {	

	// get the current prefix for the node
	char[] originalPrefix = original.prefix;
		    
	// create the new prefix for the original node, which will be all the
	// non-overlapping characters.  Note that the first distinguish
	// character will be used as the map, so the prefix is shorter.
	char distinguishing = originalPrefix[numOverlappingCharacters];
	char[] remainingPrefix = 
	    Arrays.copyOfRange(originalPrefix, numOverlappingCharacters + 1,
			       originalPrefix.length);
	char[] overlappingPrefix =
	    Arrays.copyOfRange(originalPrefix, 0, numOverlappingCharacters);
	
	// Create a new Node, which will be a copy of the original node with
	// the remaining prefix.  This new Node will become a child once the
	// new key-value mapping is put in place
	Node<V> child = new Node<V>(remainingPrefix, original.value);
	// copy over the children as well
	child.children = original.children;

	// 
	original.prefix = overlappingPrefix;
	original.children = new CharMap<Node<V>>();
	original.addChild(distinguishing, child);

	// Determine whether the remaining portion of the key was a substring of
	// the original prefix, or whether the two keys diverge but shared a
	// common, overlapping prefix
	int remainingKeyChars = key.length() - indexOfStartOfOverlap;
	
	// if the key was a substring, rework the original node to have the new
	// value and shorter prefix consisting of the overlapping characters
	if (numOverlappingCharacters == remainingKeyChars) {

	    original.value = value;	    
	}
	// Otherwise, the keys diverge, so create a new intermediate node with
	// no value mapping but that points to both the old original node and
	// a new node that contains the new value
	else {
	    int prefixStart = indexOfStartOfOverlap + 
		numOverlappingCharacters + 1;
	    char mappingKey = key.charAt(indexOfStartOfOverlap + 
					 numOverlappingCharacters);
	    char[] remainingKey = new char[key.length() - prefixStart];
	    for (int i = 0; i < remainingKey.length; ++i) {
		remainingKey[i] = key.charAt(prefixStart + i);
	    }
	    Node<V> newMapping = new Node<V>(remainingKey, value);
	    original.addChild(mappingKey, newMapping);

	    original.value = null;
	}	       
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values() {
	return new ValueView();
    }

    /**
     * The internal node class for creating the trie.
     */
    private static class Node<V> implements Serializable {

	private static final long serialVersionUID = 1;

	/**
	 * If this this node is a leaf node in the trie, these characters are
	 * the suffix of the string ending at this node; else, these characters
	 * a common substring prefix shared by the children of this node.
	 */
	private char[] prefix;

	/**
	 * The value mapped to the key that is represented by the path to this
	 * node.
	 */
	private V value;

	/**
	 * A mapping from each character transition to the child that has a key
	 * with that character.  If this node has node children, this map may be
	 * {@code null}.
	 */
	protected Map<Character,Node<V>> children;

	/**
	 * Constructs a new {@code Node} using only the characters in {@code
	 * seq} that occur at or after {@code prefixStart}.
	 * 
	 * @param seq the {@code String} to be stored at this node in the
	 *        trie
	 * @param prefixStart the index into {@code seq} that denotes the
	 *        preceding characters of {@code seq} that are not a part of the
	 *        path to this node
	 * @param value the value associated with the key {@code seq}
	 */
	// IMPLEMENTATION NOTE: we use seq and tailStart instead of passing in
	// the remaining characters to avoid an unnecessary copy from
	// subSequence.  This class stores the remaining characters in a char[]
	// to save wasted space from unnecessary object overhead.
	Node(String seq, int prefixStart, V value) {
	    this(toArray(seq, prefixStart), value);
	}

        /**
	 * Constructs a new {@code Node} that is reached by the characters in
	 * {@code prefix} and has the associated value.
         */
	Node(char[] prefix, V value) {
	    this.prefix = prefix;
	    this.value = value;
	    children = null;
	}

	public Node(String prefix, V value) {
	    this(prefix, 0, value);
	}

	public void addChild(char c, Node<V> child) {
	    if (children == null) {
		children = new CharMap<Node<V>>();
	    }
	    children.put(c, child);
	}
	
	public Node<V> getChild(char c) {
	    return (children == null) ? null : children.get(c);
	}

	public Map<Character,Node<V>> getChildren() {
	    return (children == null) 
		? new HashMap<Character,Node<V>>() : children;
	}

	public CharSequence getPrefix() {
	    return new ArraySequence(prefix);
	}
	
	public boolean isTerminal() {
	    return value != null;
	}

	void setTail(String seq) {
	    prefix = toArray(seq);
	}

	public V setValue(V newValue) {
	    if (newValue == null) {
		throw new NullPointerException("TrieMap values cannot be null");
	    }
	    V old = value;
	    value = newValue;
	    return old;
	}

	boolean prefixMatches(String seq) {
	    if (seq.length() == prefix.length) {
		for (int i = 0; i < prefix.length; ++i) {
		    if (seq.charAt(i) != prefix[i]) {
			return false;
		    }
		}
		return true;
	    }
	    return false;
	}

	private static char[] toArray(String seq) {
	    return toArray(seq, 0);
	}

	private static char[] toArray(String seq, int start) {
	    char[] arr = new char[seq.length() - start];
	    for (int i = 0; i < arr.length; ++i) {
		arr[i] = seq.charAt(i + start);
	    }
	    return arr;
	}

	public String toString() {
	    return "(" +
		((prefix.length == 0) ? "\"\"" : new String(prefix))
		+ ": " + value + ", children: " + children + ")";
	}
    }

    /**
     * A special-case subclass for the root node of the trie, whose key the
     * empty string.
     */
    private static class RootNode<V> extends Node<V> {
	
	private static final long serialVersionUID = 1;

	public RootNode() {
	    super("", null);
	    children = new CharMap<Node<V>>();
	}

	public void clear() {
	    children.clear();
	}

	void setTail(String seq) {
	    throw new IllegalStateException("cannot set tail on root node");
	}

	public V setValue(V newValue) {
	    return super.setValue(newValue);
	}

	boolean tailMatches(String seq) {
	    return seq.length() == 0;
	}	
    }

    /**
     * An immutable {@link CharSequence} implementation backed by an array.
     * This class is used internally instead of {@link String} to save space,
     * is equivalent in the character sequence representation.
     */
    private static class ArraySequence 
            implements CharSequence, Serializable {

        private static final long serialVersionUID = 1;

	private final char[] sequence;

	public ArraySequence(char[] sequence) {
	    this.sequence = sequence;
	}
	
	public char charAt(int i) {
	    return sequence[i];
	}

	public boolean equals(Object o) {
            // NOTE: this class intentionally bends the rules for Java's
            // equals() contract; an ArraySequence is equal to a String
            // instance, but the reverse is not true, thereby breaking the
            // associativity.  However, ArraySequence is always internal to a
            // TrieMap (they are converted to Strings when exposed in the API),
            // which means that a String will never be compared to an instance
            // of this class.
	    if (o instanceof String) {
		String cs = (String)o;
		if (cs.length() != sequence.length) {
		    return false;
		}
		for (int i = 0; i < sequence.length; ++i) {
		    if (cs.charAt(i) != sequence[i]) {
			return false;
		    }
		}
		return true;
	    }
	    return false;
	}

	public int hashCode() {
	    return Arrays.hashCode(sequence);
	}

	public int length() {
	    return sequence.length;
	}

	public CharSequence subSequence(int start, int end) {
	    return new ArraySequence(Arrays.copyOfRange(sequence, start, end));
	}
	
	public String toString() {
	    return new String(sequence);
	}
    }
    
    /**
     * An internal decorator class on {@link TrieMap.Node} that records that
     * {@code Node} path and associated {@link String} prefix that leads
     * to a {@code Node}.  This class is only used by
     * {@link TrieMap.TrieIterator} class for constructing correct {@link
     * Map.Entry} instances.
     *
     * <p>
     *
     * This class allows for full on-the-fly recovery of the key of a map based
     * on a path in the trie.  We add a field in this class instead of directly
     * in the {@code Node} class to save memory.  The addition of a prefix would
     * negate all the memory savings of the trie.  However, {@code
     * AnnotatedNode} instances are short-lived (typically, only during
     * iteration), and therefore incur little memory overhead for reconstructing
     * the full key.
     *
     * @see TrieMap.TrieIterator
     */
    private static class AnnotatedNode<V> {

        /**
         * The full sequence of characters leading from the root of the trie to
         * this node.
         */
	private final String prefix;
	
	private final Node<V> node;
	
	public AnnotatedNode(Node<V> node, String prefix) {
	    this.prefix = prefix;
            this.node = node;
	}

	public String toString() {
	    return node.toString();
	}
    }    

    /**
     *
     */
    private abstract class TrieIterator<E>
	implements Iterator<E> {
	
	/**
	 * The a queue of nodes that reflect the current state of the
	 * depth-first traversal of the trie that is being done by this
	 * iterator.
	 */
	private final Deque<AnnotatedNode<V>> dfsFrontier;

	/**
	 * The next entry to return or {@code null} if there are no further
	 * entries.
	 */
	private Map.Entry<String,V> next;

	/**
	 * The node previously returned used for supporting the {@code remove}
	 * operation.
	 */
	private Map.Entry<String,V> prev;

	public TrieIterator() {
	    
	    dfsFrontier = new ArrayDeque<AnnotatedNode<V>>();
	    for (Entry<Character,Node<V>> child : 
		     rootNode.getChildren().entrySet())
		dfsFrontier.offer(
                    new AnnotatedNode<V>(child.getValue(),
                                         child.getKey().toString()));
	    next = null;
	    prev = null;

	    // search for the first termial node
	    advance();
	}

	/**
	 * Increments the current state of the depth-first traversal of the trie
	 * and sets {@link TrieMap.TrieIterator#next} to the next terminal node
	 * in the trie or {@code null} if no such node exists.
	 */
	private void advance() {

	    AnnotatedNode<V> n = dfsFrontier.pollFirst();

	    // repeatedly expand a new frontier until we either run out of nodes
	    // or we find a terminal node
	    while (n != null && !n.node.isTerminal()) {
		// remove the top of the stack and add its children in
		// alphabetical order.  Because the character mapping is sorted
		// alphabetically, temporarily copy them to an array so we can
		// push them on the stack in reverse order.
                @SuppressWarnings("unchecked")
                Entry<Character,Node<V>>[] reversed = 
                    (Entry<Character,Node<V>>[])
                    new Entry[n.node.getChildren().size()];
                int i = 1;
 		for (Entry<Character,Node<V>> child : 
			 n.node.getChildren().entrySet()) {
                    reversed[reversed.length - i] = child;
                    i++;
                }
                
                for (Entry<Character,Node<V>> child : reversed) {
 		    dfsFrontier.push(new AnnotatedNode<V>(
				     child.getValue(), n.prefix 
				     + n.node.getPrefix() + child.getKey()));
 		}
		n = dfsFrontier.pollFirst();
	    } 
		
	    if (n == null) {
		next = null;
	    }
	    else {
		next = createEntry(n);
		// remove the top of the stack and add its children in
		// alphabetical order.  Because the character mapping is sorted
		// alphabetically, temporarily copy them to an array so we can
		// push them on the stack in reverse order.
                @SuppressWarnings("unchecked")
                Entry<Character,Node<V>>[] reversed = 
                    (Entry<Character,Node<V>>[])
                    new Entry[n.node.getChildren().size()];
                int i = 1;
 		for (Entry<Character,Node<V>> child : 
			 n.node.getChildren().entrySet()) {
                    reversed[reversed.length - i] = child;
                    i++;
                }

 		for (Entry<Character,Node<V>> child : reversed) {
 		    dfsFrontier.push(new AnnotatedNode<V>(
			child.getValue(), n.prefix 
			+ n.node.getPrefix() + child.getKey()));
 		}
	    }
	}

	/**
	 * Creates a new {@code Entry} that is backed by the provided {@code
	 * AnnotatedNode}.  Changes to the returned entry are pass through to
	 * the node.
	 */
	private Map.Entry<String,V> createEntry(AnnotatedNode<V> node) {
	    // determine the String key that makes up this entry based on what
	    // nodes have been traversed thus far.
	    String key = node.prefix + node.node.getPrefix();
	    return new TrieEntry<V>(key, node.node);
	}
	
	/**
	 * Returns {@code true} if this iterator has more elements.
	 */
	public boolean hasNext() {
	    return next != null;
	}

	/**
	 * Returns the next {@code Entry} from the trie.
	 */
	public Map.Entry<String,V> nextEntry() {
	    if (next == null) {
		throw new NoSuchElementException("no further elements");
	    }
	    prev = next;
	    advance();
	    return prev;
	}

	/**
	 * Removes from the underlying collection the last element returned by
	 * the iterator.
	 *
	 * @throws IllegalStateException if the {@code next} method has not yet
	 *         been called, or the {@code remove} method has already been
	 *         called after the last call to the {@code next} method.
	 */
	public void remove() {
	    if (prev == null) {
		throw new IllegalStateException();
	    }
	    TrieMap.this.remove(prev.getKey());
	    prev = null;
	}
    }


    private class EntryIterator 
	    extends TrieIterator<Map.Entry<String,V>> {

	public Map.Entry<String,V> next() {
	    return nextEntry();
	}
	
    }

    private class KeyIterator extends TrieIterator<String> {

	public String next() {
	    return nextEntry().getKey();
	}
	
    }

    private class ValueIterator extends TrieIterator<V> {

	public V next() {
	    return nextEntry().getValue();
	}
	
    }
    
    /**
     * An implementation of {@link Map.Entry} backed by a {@link TrieMap.Node}
     * instance.  Changes to instances of this class are reflected in the trie.
     */
    private static class TrieEntry<V> 
	    extends AbstractMap.SimpleEntry<String,V> {
	
	private static final long serialVersionUID = 1;

	private final Node<V> node;

	public TrieEntry(String key, Node<V> node) {
	    super(key, node.value);
	    this.node = node;
	}

	public V getValue() {
	    return node.value;
	}

	public V setValue(V newValue) {
	    return node.setValue(newValue);
	}
    }
    
    /**
     * A {@link Set} view of the keys contained in this trie.
     */
    private class KeyView extends AbstractSet<String> {
	
	public void clear() {
	    TrieMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsKey(o);
	}

	public Iterator<String> iterator() {
	    return new KeyIterator();
	}
	
	public boolean remove(Object o) {
	    return TrieMap.this.remove(o) != null;
	}

	public int size() {
	    return size;
	}
    }

    /**
     * A {@link Collection} view of the values contained in this trie.
     */
    private class ValueView extends AbstractCollection<V> {
	
	public void clear() {
	    TrieMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsValue(o);
	}
	
	public Iterator<V> iterator() {
	    return new ValueIterator();
	}
	
	public int size() {
	    return size;
	}
    }

    /**
     * A {@link Set} view of the key value mappings contained in this trie.
     */
    private class EntryView extends AbstractSet<Map.Entry<String,V>> {
	
	public void clear() {
	    TrieMap.this.clear();
	}

	public boolean contains(Object o) {
	    if (o instanceof Map.Entry) {
		Map.Entry e = (Map.Entry)o;
		Object key = e.getKey();
		Object val = e.getValue();
		Object mapVal = TrieMap.this.get(key);
		return mapVal == val || (val != null && val.equals(mapVal));
	    }
	    return false;
	}

	public Iterator<Map.Entry<String,V>> iterator() {
	    return new EntryIterator();
	}
	
	public int size() {
	    return size;
	}
    }   
}