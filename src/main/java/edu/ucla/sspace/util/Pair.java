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


/**
 * A utility class for holding two of the same type of object.
 */
public class Pair<T> {

    /**
     * The first object in the pair
     */
    public final T x;
    
    /**
     * The second object in the pair
     */
    public final T y;

    /**
     * Creates a pair out of {@code x} and {@code y}
     */
    public Pair(T x, T y) {
	this.x = x;
	this.y = y;
    }

    public boolean equals(Object o) {
	if (o == null || !(o instanceof Pair))
	    return false;
	Pair p = (Pair)o;
	return (x == p.x || (x != null && x.equals(p.x))) &&
	    (y == p.y || (y != null && y.equals(p.y)));
    }
    
    public int hashCode() {
	return ((x == null) ? 0 : x.hashCode()) ^
	    ((y == null) ? 0 : y.hashCode());
    }

    public String toString() {
	return "{" + x + ", " + y + "}";
    }
}