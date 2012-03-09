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

package edu.ucla.sspace.vector;

import java.io.Serializable;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A decorator of a {@code Vector} which provides synchronized access to
 * a {@code Vector}.
 *
 * @author Keith Stevens
 */
class SynchronizedVector implements DoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The original {@code Vector} that this {@code SynchronizedVector}
     * decorates.
     */
    private final DoubleVector vector;

    /**
     * Creates a new {@code SynchronizedVector} decorating an already existing
     * {@code Vector}.
     *
     * @param v The vector to decorate.
     */
    public SynchronizedVector(DoubleVector v) {
        vector = v;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized double add(int index, double delta) {
        return vector.add(index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double get(int index) {
        return vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Double getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        return vector.magnitude();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void set(int index, double value) {
        vector.set(index, value);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void set(int index, Number value) {
        vector.set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double[] toArray() {
        return vector.toArray();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int length() {
        return vector.length();
    }
}
