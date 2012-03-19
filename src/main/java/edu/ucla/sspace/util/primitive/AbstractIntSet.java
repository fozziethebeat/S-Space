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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;


/**
 * An abstact base class that implements all of the {@link IntSet} 
 */
public abstract class AbstractIntSet extends AbstractSet<Integer> 
        implements IntSet {

    public boolean add(int i) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(IntCollection ints) {
        IntIterator it = ints.iterator();
        boolean changed = false;
        while (it.hasNext())
            if (add(it.nextInt()))
                changed = true;
        return changed;
    }

    public boolean containsAll(IntCollection ints) {
        IntIterator it = ints.iterator();
        while (it.hasNext())
            if (!contains(it.nextInt()))
                return false;
        return true;
    }

    public abstract IntIterator iterator();
    
    public boolean remove(int i) {
        throw new UnsupportedOperationException();
    }
    
    public boolean removeAll(IntCollection ints) {
        IntIterator it = ints.iterator();
        boolean changed = false;
        while (it.hasNext())
            if (remove(it.nextInt()))
                changed = true;
        return changed;
    }

    /**
     * Retains only the elements in this set that are contained in the specified
     * {@code IntSet}.
     */
    public boolean retainAll(IntCollection ints) {
        IntIterator it = iterator();
        boolean changed = false;
        while (it.hasNext()) {
            if (!ints.contains(it.nextInt())) {
                it.remove();
                changed = true;
            }
        }
        return changed;
    }

    public int[] toPrimitiveArray() {
        int[] arr = new int[size()];
        IntIterator it = iterator();
        int i = 0;
        while (it.hasNext()) {
            arr[i++] = it.nextInt();
        }
        return arr;
    }
}