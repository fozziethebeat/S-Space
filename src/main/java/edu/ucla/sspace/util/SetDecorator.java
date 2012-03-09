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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;


/**
 * A wrapper around an existing {@link Set} implementation that allows classes
 * to extend the functionality of a set by overriding some of its methods.  For
 * example, a subclass could enforce a size capacity on an existing set by
 * overriding {@code add} and {@code addAll} to not add elements beyond a
 * certain number.
 */
public class SetDecorator<T> implements Set<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected final Set<T> set;

    public SetDecorator(Set<T> set) {
        if (set == null)
            throw new NullPointerException();
        this.set = set;
    }

    public boolean add(T e) {
        return set.add(e);
    }

    public boolean addAll(Collection<? extends T> c) {
        return set.addAll(c);
    }
    
    public void clear() {
        set.clear();
    }
    
    public boolean contains(Object o) {
        return set.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return set.containsAll(c);
    }

    public boolean equals(Object o) {
        return set.equals(o);
    }
    
    public int hashCode() {
        return set.hashCode();
    }

    public boolean isEmpty() {
        return set.isEmpty();
    }

    public Iterator<T> iterator() {
        return set.iterator();
    }

    public boolean remove(Object o) {
        return set.remove(o);
    }

    public boolean removeAll(Collection<?> c) {
        return set.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return set.retainAll(c);
    }
    
    public int size() {
        return set.size();
    }
    
    public Object[] toArray() {
        return set.toArray();
    }

    public <E> E[] toArray(E[] a) {
        return set.toArray(a);
    }

    public String toString() {
        return set.toString();
    }
}