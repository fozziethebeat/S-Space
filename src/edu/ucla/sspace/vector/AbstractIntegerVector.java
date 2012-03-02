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
 * methods in {@link IntegerVector}.  {@link IntegerVector} implementations need
 * only implement {@link #length()} and {@link #get(int)} functionality to be
 * read-only vectors.
 */
public abstract class AbstractIntegerVector extends AbstractVector<Integer> 
        implements IntegerVector {

    public AbstractIntegerVector() { }

    /**
     * Throws an {@link UnsupportedOperationException} if called (vector is
     * unmodifiable).
     */
    public int add(int index, int delta) {
        throw new UnsupportedOperationException("set is not supported");
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof IntegerVector) {
            IntegerVector v = (IntegerVector)o;
            int len = v.length();
            if (len != length())
                return false;
            for (int i = 0; i < len; ++i) {
                if (v.get(i) != get(i))
                    return false;
            }
            return true;
        }
        else 
            return super.equals(o);
    }

    /**
     * {@inheritDoc}
     */
    public Integer getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int len = length();
        int hash = 0;
        for (int i = 0; i < len; ++i) {
            hash ^= i ^ get(i);
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
            double d = get(i);
            m += d * d;
        }
        return Math.sqrt(m);
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called (vector is
     * unmodifiable).
     */
    public void set(int index, int value) {
        throw new UnsupportedOperationException("set is not supported");
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray() {
        int[] arr = new int[length()];
        for (int i = 0; i < arr.length; ++i)
            arr[i] = get(i);
        return arr; 
    }

    /**
     * Returns a string description of the full contents of this vector
     */
    public String toString() {
        int length = length();
        StringBuilder sb = new StringBuilder(length * 3);
        sb.append('[');
        for (int i = 0; i < length; ++i) {
            sb.append(get(i));
            if (i + 1 < length)
                sb.append(", ");
        }
        sb.append(']');
        return sb.toString();
    }
}