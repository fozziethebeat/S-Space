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
 * A decorator of a {@code Vector} which provides atomic concurrent access to
 * another {@code Vector}.  This allows all reads to be done concurrently, while
 * limiting to writing to only one thread at a time.  This does not provide a
 * specific implementation of a {@code Vector}, allowing any {@code Vector}
 * implementation to be made atomic.
 *
 * @author David Jurgens
 * @author Keith Stevens
 */
public class AtomicSparseVector implements SparseDoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The original {@code SparseDoubleVector} that this {@code AtomicSparseVector}
     * decorates.
     */
    private final SparseDoubleVector vector;

    /**
     * Read lock for non-mutating access to the backing {@code vector}.
     */
    private final Lock readLock;

    /**
     * Write lock for mutating access to the backing {@code vector}.
     */
    private final Lock writeLock;

    /**
     * Creates a new {@code AtomicSparseVector} decorating an already existing
     * {@code Vector}.
     *
     * @param v The vector to decorate.
     */
    public AtomicSparseVector(SparseDoubleVector v) {
        vector = v;

        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        readLock = rwLock.readLock();
        writeLock = rwLock.writeLock();
    }
    
    /**
     * {@inheritDoc}
     */
    public double addAndGet(int index, double delta) {
        return add(index, delta);
    }

    /**
     * {@inheritDoc}
     */
    public double getAndAdd(int index, double delta) {
        writeLock.lock();
        double value = vector.get(index);
        vector.set(index, value + delta);
        writeLock.unlock();
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        writeLock.lock();
        double value = vector.add(index, delta);
        writeLock.unlock();
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        readLock.lock();
        double value = vector.get(index);
        readLock.unlock();
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.getNonZeroIndices();
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * Returns the {@link SparseDoubleVector} that backs this instance.
     */
    public SparseDoubleVector getVector() {
        return vector;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        readLock.lock();
        double m = vector.magnitude();
        readLock.unlock();
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        writeLock.lock();
        vector.set(index, value);
        writeLock.unlock();
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
        readLock.lock();
        double[] array = vector.toArray();
        readLock.lock();
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        readLock.lock();
        int length = vector.length();
        readLock.unlock();
        return length;
    }
}
