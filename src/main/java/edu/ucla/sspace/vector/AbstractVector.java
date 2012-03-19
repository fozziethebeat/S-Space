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

package edu.ucla.sspace.vector;


/**
 * An abstract base class that provides default implementations of common
 * methods in {@link Vector}.  {@link Vector} implementations need
 * only implement {@link #length()} and {@link #getValue(int)} functionality to be
 * read-only vectors.
 */
public abstract class AbstractVector<T extends Number> implements Vector<T> {

    public AbstractVector() { }

    /**
     * Throws an {@link UnsupportedOperationException} if called (vector is
     * unmodifiable).
     */
    public double add(int index, double delta) {
        throw new UnsupportedOperationException("Modification is unsupported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof Vector) {
            Vector v = (Vector)o;
            int len = v.length();
            if (len != length())
                return false;
            for (int i = 0; i < len; ++i) {
                if (!v.getValue(i).equals(getValue(i)))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int len = length();
        int hash = 0;

        for (int i = 0; i < len; ++i) {
            hash ^= i ^ getValue(i).intValue();
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        double m = 0;
        int length = length();
        for (int i = 0; i < length; ++i) {
            double d = getValue(i).doubleValue();
            m += d * d;
        }
        return Math.sqrt(m);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        throw new UnsupportedOperationException("Modification is unsupported");
    }

    /**
     * Returns a string description of the full contents of this vector
     */
    public String toString() {
        int length = length();
        StringBuilder sb = new StringBuilder(length * 3);
        sb.append('[');
        for (int i = 0; i < length; ++i) {
            sb.append(getValue(i));
            if (i + 1 < length)
                sb.append(", ");
        }
        sb.append(']');
        return sb.toString();
    }
}