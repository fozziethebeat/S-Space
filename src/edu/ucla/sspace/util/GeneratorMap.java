/*
 * Copyright 2009 Keith Stevens 
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;


/**
 * A Mapping from Strings to object instances created by a {@link Generator}.
 * If a value does not exist for a given string, one will be automatically
 * generated using a provided {@code Generator}.
 *
 * @author Keith Stevens
 */
public class GeneratorMap<T> implements Map<String, T>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The {@code Generator} for generating new map values.
     */
    private final Generator<T> generator;

    /**
     * A mapping from terms to their instance
     */
    private final Map<String, T> termToItem;

    private boolean readOnly;

    /**
     * Creates a new {@link GeneratorMap} using a {@code ConcurrentHashMap}.
     * Items will be using the provided {@link Generator}.
     */
    public GeneratorMap(Generator<T> generator) {
        this(generator, new ConcurrentHashMap<String, T>());
    }

    /**
     * Creates a new {@link GeneratorMap} using a the provided map}.  Items will
     * be using the provided {@link Generator}.
     */
    public GeneratorMap(Generator<T> generator, Map<String, T> map) {
        termToItem = map;
        this.generator = generator;
        readOnly = false;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        termToItem.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return termToItem.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return termToItem.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<String, T>> entrySet() {
        return termToItem.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return termToItem.equals(o);
    }

    /**
     * Returns a {@code T} for the given term, if no mapping for
     * {@code term} then a new vaue is generated, stored, and returned.
     *
     * @param term The term specifying the item to return.
     *
     * @return A generated item corresponding to {@code term}.
     */
    public T get(Object term) {
        // Check that an index vector does not already exist.
        T v = termToItem.get(term);
        if (v == null && !readOnly) {
            synchronized (this) {
                // Confirm that some other thread has not created an index
                // vector for this term.
                v = termToItem.get(term);
                if (v == null) {
                    // Generate the index vector for this term and store it.
                    v = generator.generate();
                    termToItem.put((String) term, v);
                }
            }
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return termToItem.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return termToItem.isEmpty();
    }
    
    /**
     * {@inheritDoc}
     */
    public Set<String> keySet() {
        return termToItem.keySet();
    }

    /**
     * Unsupported.
     */
    public T put(String key, T vector) {
        throw new UnsupportedOperationException(
                "Items may not be inserted into this GeneratorMap.");
    }

    /**
     * Unsupported.
     */
    public void putAll(Map<? extends String, ? extends T> m) {
        throw new UnsupportedOperationException(
                "Items may not be inserted into this GeneratorMap.");
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return termToItem.size();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<T> values() {
        return termToItem.values();
    }

    /**
     * {@inheritDoc}
     */
    public T remove(Object key) {
        return termToItem.remove(key);
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }
}
