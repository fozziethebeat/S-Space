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
 * methods in {@link DoubleVector}.
 */
public abstract class AbstractDoubleVector implements DoubleVector {

    public AbstractDoubleVector() { }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o instanceof DoubleVector) {
            DoubleVector v = (DoubleVector)o;
            int len = v.length();
            if (len != length())
                return false;
            for (int i = 0; i < len; ++i) {
                if (v.get(i) != get(i))
                    return false;
            }
            return true;
        }
        else if (o instanceof Vector) {
            DoubleVector v = Vectors.asDouble((Vector)o);
            int len = v.length();
            if (len != length())
                return false;
            for (int i = 0; i < len; ++i) {
                if (v.get(i) != get(i))
                    return false;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int len = length();
        int hash = 0;
        double sum = 0;
        for (int i = 0; i < len; ++i) {
            sum += get(i);
        }
        return (int)sum;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] arr = new double[length()];
        for (int i = 0; i < arr.length; ++i)
            arr[i] = get(i);
        return arr; 
    }
}