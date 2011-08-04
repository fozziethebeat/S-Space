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

import java.util.ArrayList;
import java.util.Collection;

/**
 * An {@link ArrayList} subclass that will never throw an {@link
 * IndexOutOfBoundsException} and instead grow the backing array to match the
 * size of the requested operation.
 */
public class GrowableArrayList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 1L;


    public GrowableArrayList() {

    }

    public GrowableArrayList(Collection<? extends E> c) {
	super(c);
    }

    public GrowableArrayList(int capacity) {
	super(capacity);
    }

    private void checkIndex(int index) {
	// expand to at least that big
	if (index >= size()) {
	    for (int i = size(); i <= index; ++i) {
		super.add(null);
	    }
	}
    }

    public void add(int index, E element) {
	checkIndex(index);
	super.add(index, element);
    }

    public E get(int index) {
	// don't expand on a get() call
	return (index >= size()) ? null : super.get(index);
    }

    public E remove(int index) {
	return (index >= size()) ? null : super.remove(index);
    }

    protected void removeRange(int fromIndex, int toIndex) {
	if (toIndex >= size() && fromIndex < size()) {
	    super.removeRange(fromIndex, size() - 1);
	}
    }

    public E set(int index, E element) {
	checkIndex(index);
	return super.set(index, element);
    }
}