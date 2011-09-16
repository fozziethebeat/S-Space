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
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.Vector;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;


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
     * The {@link WorkQueue} from which worker threads run word-word comparisons
     */
    private final WorkQueue workQueue;
    
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
        workQueue = WorkQueue.getWorkQueue(numThreads);
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
            int numberOfSimilarWords, final Similarity.SimType similarityType) {

        Vector v = sspace.getVector(word);

        // if the semantic space did not have the word, then return null
        if (v == null) {
            return null;
        }
        
        final Vector vector = v;
        return getMostSimilar(v, sspace, numberOfSimilarWords, similarityType);
    }

    public SortedMultiMap<Double,String> getMostSimilar(
            final Vector vector, final SemanticSpace sspace,
            int numberOfSimilarWords, final Similarity.SimType similarityType) {
        Set<String> words = sspace.getWords();
        
        // the most-similar set will automatically retain only a fixed number
        // of elements
        final SortedMultiMap<Double,String> mostSimilar =
            new BoundedSortedMultiMap<Double,String>(numberOfSimilarWords,
                                                     false);

        Object key = workQueue.registerTaskGroup(words.size());

        // loop through all the other words computing their similarity
        for (final String other : words) {
            workQueue.add(key, new Runnable() {
                public void run() {
                    Vector otherV = sspace.getVector(other);
                    // Skip the comparison if the vectors are actually the same.
                    if (otherV == vector)
                        return;

                    Double similarity = Similarity.getSimilarity(
                        similarityType, vector, otherV);
                    
                    // lock on the Map, as it is not thread-safe
                    synchronized(mostSimilar) {
                        mostSimilar.put(similarity, other);
                    }
                }
            });
        }
        
        workQueue.await(key);
        return mostSimilar;
    }
}
