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
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.vector.Vector;


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
     * The work used by all {@code LinkClustering} instances to perform
     * multi-threaded operations.
     */
    private final WorkQueue workQueue;

    /**
     * Creates this {@code WordComparator} with as many threads as processors.
     */
    public RowComparator() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates this {@code WordComparator} with the specified number of threads.
     */
    public RowComparator(int numProcs) {
        this.workQueue = WorkQueue.getWorkQueue(numProcs);
    }

    /**
     * Compares the specified row to all other rows, returning the k-nearest
     * rows according to the similarity metric.
     *
     * @param m The {@link Matrix} containing data points to be compared
     * @param row The current row in {@code m} to be compared against all other
     *        rows
     * @param kNearestRows The number of most similar rows to retain
     * @param similarityType The similarity method to use when comparing rows
     *
     * @return a mapping from the similarity to the {@code kNearestRows} most
     *         similar rows
     */
    public SortedMultiMap<Double,Integer> getMostSimilar(
            Matrix m, int row,
            int kNearestRows, Similarity.SimType similarityType) {
        return getMostSimilar(m, row, kNearestRows,
                              Similarity.getSimilarityFunction(similarityType));
    }

    /**
     * Compares the specified row to all other rows, returning the k-nearest
     * rows according to the similarity metric.
     *
     * @param m The {@link Matrix} containing data points to be compared
     * @param row The current row in {@code m} to be compared against all other
     *        rows
     * @param kNearestRows The number of most similar rows to retain
     * @param simFunction The {@link SimilarityFunction} to use when comparing
     *        rows
     * @return a mapping from the similarity to the k most similar rows
     */
    public SortedMultiMap<Double,Integer> getMostSimilar(
            Matrix m, int row,
            int kNearestRows, SimilarityFunction simFunction) {
        
        
        Object key = workQueue.registerTaskGroup(m.rows() - 1);

        // the most-similar set will automatically retain only a fixed number of
        // elements
        final SortedMultiMap<Double,Integer> mostSimilar =
            new BoundedSortedMultiMap<Double,Integer>(kNearestRows, false);

        // loop through all the other words computing their similarity
        int rows = m.rows();
        Vector v = m.getRowVector(row);
        for (int i = 0; i < rows; ++i) {
            // skip same row
            if (i == row) 
                continue;

            workQueue.add(key,
                          new Comparison(m, v, i, simFunction, mostSimilar));
        }
        
        // Wait for all the partition densities to be calculated
        workQueue.await(key);
        
        return mostSimilar;
    }

    /**
     * A comparison task that compares the row vector and updates the mapping
     * from similarity to row.
     */
    private static class Comparison implements Runnable {
        
        private final Matrix m;
        private final Vector row;
        private final int otherRow;
        private final SimilarityFunction simFunction;
        private final MultiMap<Double,Integer> mostSimilar;

        public Comparison(Matrix m,
                          Vector row,
                          int otherRow,
                          SimilarityFunction simFunction,
                          MultiMap<Double,Integer> mostSimilar) {
            this.m = m;
            this.row = row;
            this.otherRow = otherRow;
            this.simFunction = simFunction;
            this.mostSimilar = mostSimilar;
        }

        public void run() {
            Double similarity = simFunction.sim(
                    row, m.getRowVector(otherRow));
            
            // lock on the Map, as it is not thread-safe
            synchronized(mostSimilar) {
                mostSimilar.put(similarity, otherRow);
            }
        }
    }
}
