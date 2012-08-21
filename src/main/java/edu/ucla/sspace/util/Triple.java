/*
 * Copyright 2012 David Jurgens
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
 * A utility class for holding three of the same type of object.
 */
public class Triple<T> implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The first object in the triple
     */
    public final T x;
    
    /**
     * The second object in the triple
     */
    public final T y;

    /**
     * The third object in the triple
     */
    public final T z;

    /**
     * Creates a triple out of {@code x}, {@code y}, and  {@code z}
     */
    public Triple(T x, T y, T z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Returns {@code true} if {@code o} is a {@link Triple} and its {@code x},
     * {@code y}, and {@code z} elements are equal to those of this triple.
     * Note that equality is specific to the ordering of the elements.
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Triple))
            return false;
        Triple p = (Triple)o;
        return (x == p.x || (x != null && x.equals(p.x))) 
            && (y == p.y || (y != null && y.equals(p.y)))
            && (z == p.z || (z != null && z.equals(p.z)));
    }
    
    public int hashCode() {
        return ((x == null) ? 0 : x.hashCode()) 
            ^  ((y == null) ? 0 : y.hashCode())
            ^  ((z == null) ? 0 : z.hashCode());
    }

    public String toString() {
        return "{" + x + ", " + y + ", " + z + "}";
    }
}