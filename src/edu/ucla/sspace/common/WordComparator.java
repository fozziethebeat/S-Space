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

package edu.ucla.sspace.common;

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
 * word in a {@link SemanticSpace}.  The comparisons required for generating the
 * list maybe be run in parallel by configuring an instance of this class to use
 * multiple threads. <p>
 *
 * All instances of this class are thread-safe.
 * 
 * @author David Jurgens
 */
public class WordComparator {

    /**
     * The queue from which worker threads run word-word comparisons
     */
    private final BlockingQueue<Runnable> workQueue;
    
    /**
     * Creates this {@code WordComparator} with as many threads as processors.
     */
    public WordComparator() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates this {@code WordComparator} with the specified number of threads.
     */
    public WordComparator(int numThreads) {
        workQueue = new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < numThreads; ++i) {
            new WorkerThread(workQueue, 10).start();            
        }
    }

    /**
     * Compares the provided word to all other words in the provided {@link
     * SemanticSpace} and return the specified number of words that were most
     * similar according to the specified similarity measure.
     *
     * @return the most similar words, or {@code null} if the provided word was
     *         not in the semantic space.
     */
    public SortedMultiMap<Double,String> getMostSimilar(
            final String word, final SemanticSpace sspace,
            int numberOfSimilarWords, Similarity.SimType similarityType) {

        Vector v = sspace.getVector(word);

        // if the semantic space did not have the word, then return null
        if (v == null) {
            return null;
        }
        
        final Vector vector = v;

        Set<String> words = sspace.getWords();
        
        // the most-similar set will automatically retain only a fixed number
        // of elements
        final SortedMultiMap<Double,String> mostSimilar =
            new BoundedSortedMultiMap<Double,String>(numberOfSimilarWords,
                                                     false);

        // The semaphore used to block until all the words have been compared.
        // Use word-2 since we don't process the comparison of the word to
        // itself, and we want one permit to be availabe after the last word is
        // compared.  The negative ensures that the release() must happen before
        // the main thread's acquire() will return.
        final Semaphore comparisons = new Semaphore(0 - (words.size() - 2));

        // loop through all the other words computing their
        // similarity
        int submitted = 0;
        for (String s : words) {

            final String other = s;
            
            // skip if it is ourselves
            if (word.equals(other)) 
                continue;

             workQueue.offer(new Comparison(
                         comparisons, sspace, vector,
                         other, similarityType, mostSimilar));
        }
        
        try {
            comparisons.acquire();
        } catch (InterruptedException ie) {
            // check whether we were interrupted while still waiting for the
            // comparisons to finish
             if (comparisons.availablePermits() < 1) {
                throw new IllegalStateException(
                    "interrupted while waiting for word comparisons to finish", 
                    ie);
             }
        }
        
        //System.out.println(executor.shutdownNow())
        return mostSimilar;
    }

    /**
     * A comparison task that compares the vector for the other word and updates
     * the mapping from similarity to word.
     */
    private static class Comparison implements Runnable {
        
        private final Semaphore semaphore;

        SemanticSpace sspace;
        Vector vector;
        String other;
        Similarity.SimType similarityMeasure;
        MultiMap<Double,String> mostSimilar;

        public Comparison(Semaphore semaphore,
                          SemanticSpace sspace,
                          Vector vector,
                          String other,
                          Similarity.SimType similarityMeasure,
                          MultiMap<Double,String> mostSimilar) {
            this.semaphore = semaphore;
            this.sspace = sspace;
            this.vector = vector;
            this.other = other;
            this.similarityMeasure = similarityMeasure;
            this.mostSimilar = mostSimilar;
        }

        public void run() {
            try {            
                Vector otherV = sspace.getVector(other);

                Double similarity = Similarity.getSimilarity(
                    similarityMeasure, vector, otherV);
                
                // lock on the Map, as it is not thread-safe
                synchronized(mostSimilar) {
                    mostSimilar.put(similarity, other);
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
