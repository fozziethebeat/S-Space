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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.Vector;

import java.lang.reflect.Method;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A utility class for finding the {@code k} most-similar words to a provided
 * row in a {@link Matrix}.  The comparisons required for generating the list
 * maybe be run in parallel by configuring an instance of this class to use
 * multiple threads. <p>
 *
 * All instances of this class are thread-safe.
 * 
 * @author David Jurgens
 */
public class RowComparator {

    /**
     * The queue from which worker threads run word-word comparisons
     */
    private final BlockingQueue<Runnable> workQueue;
    
    /**
     * Creates this {@code WordComparator} with as many threads as processors.
     */
    public RowComparator() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates this {@code WordComparator} with the specified number of threads.
     */
    public RowComparator(int numThreads) {
        workQueue = new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < numThreads; ++i) {
            new WorkerThread(workQueue).start();            
        }
    }

    /**
     * Compares the specified row to all other rows, returning the k-nearest
     * rows according to the similarity metric.
     *
     * @return a mapping from the similarity to the k most similar rows
     */
    public SortedMultiMap<Double,Integer> getMostSimilar(
            Matrix m, int row,
            int kNearestRows, Similarity.SimType similarityType) {
        
        // the most-similar set will automatically retain only a fixed number
        // of elements
        final SortedMultiMap<Double,Integer> mostSimilar =
            new BoundedSortedMultiMap<Double,Integer>(kNearestRows, false);

        // The semaphore used to block until all the rows have been compared.
        // Use rows-2 since we don't process the comparison of the word to
        // itself, and we want one permit to be available after the last row is
        // compared.  The negative ensures that the release() must happen before
        // the main thread's acquire() will return.
        final Semaphore comparisons = new Semaphore(0);

        // loop through all the other words computing their
        // similarity
        int rows = m.rows();
        Vector v = m.getRowVector(row);
        for (int i = 0; i < rows; ++i) {
            // skip same row
            if (i == row) 
                continue;

            workQueue.offer(new Comparison(
                            comparisons, m, v, i,
                            similarityType, mostSimilar));
        }
        
        try {
            comparisons.acquire(rows - 1);
        } catch (InterruptedException ie) {
            // check whether we were interrupted while still waiting for the
            // comparisons to finish
            if (comparisons.availablePermits() < 1) {
                throw new IllegalStateException(
                    "interrupted while waiting for word comparisons to finish", 
                    ie);
            }
        }
        
        return mostSimilar;
    }

    /**
     * A comparison task that compares the row vector and updates the mapping
     * from similarity to row.
     */
    private static class Comparison implements Runnable {
        
        private final Semaphore semaphore;
        private final Matrix m;
        private final Vector row;
        private final int otherRow;
        private final Similarity.SimType similarityMeasure;
        private final MultiMap<Double,Integer> mostSimilar;

        public Comparison(Semaphore semaphore,
                          Matrix m,
                          Vector row,
                          int otherRow,
                          Similarity.SimType similarityMeasure,
                          MultiMap<Double,Integer> mostSimilar) {
            this.semaphore = semaphore;
            this.m = m;
            this.row = row;
            this.otherRow = otherRow;
            this.similarityMeasure = similarityMeasure;
            this.mostSimilar = mostSimilar;
        }

        public void run() {
            try {            

                Double similarity = Similarity.getSimilarity(
                    similarityMeasure, row, m.getRowVector(otherRow));
                
                // lock on the Map, as it is not thread-safe
                synchronized(mostSimilar) {
                    mostSimilar.put(similarity, otherRow);
                }
            } catch (Exception e) {
                // Rethrow any reflection-related exception, as this situation
                // should not normally occur since the Method being invoked
                // comes directly from the Similarity class.
                throw new Error(e);
            } finally {
                // notify that the word has been processed regardless of whether
                // an error occurred
                semaphore.release();
            }
        }
    }
}
