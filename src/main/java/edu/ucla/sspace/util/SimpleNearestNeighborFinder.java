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

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.similarity.CosineSimilarity;
import edu.ucla.sspace.similarity.SimilarityFunction;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.WorkQueue;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.info;


/**
 * A native {@link NearestNeighborFinder} implemetnation for finding the {@code
 * k} most-similar words to a provided word in a {@link SemanticSpace} using a
 * brute-force search.  The comparisons required for generating the list maybe
 * be run in parallel by configuring an instance of this class to use multiple
 * threads.
 *
 * <p> All instances of this class are thread-safe.
 * 
 * @author David Jurgens
 */
public class SimpleNearestNeighborFinder implements NearestNeighborFinder {

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(SimpleNearestNeighborFinder.class.getName());

    /**
     * The work queue where concurrent jobs are run during the nearest-neighbot
     * searching process
     */
    private final WorkQueue workQueue;

    /**
     * The semantic space from which the principle vectors are derived
     */
    private final SemanticSpace sspace;    
    
    /**
     * The similarity function used to compare words
     */
    private final SimilarityFunction simFunc;

    /**
     * Creates this {@code SimpleNearestNeighborFinder} with as many threads as
     * processors, and using the cosine similarity for word comparisons.
     */
    public SimpleNearestNeighborFinder(SemanticSpace sspace) {
        this(sspace, new CosineSimilarity(), 
             Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates this {@code SimpleNearestNeighborFinder} with the specified
     * number of threads, using the cosine similarity for word comparisons.
     */
    public SimpleNearestNeighborFinder(SemanticSpace sspace, int numThreads) {
        this(sspace, new CosineSimilarity(), numThreads);
    }

    /**
     * Creates this {@code SimpleNearestNeighborFinder} with as many threads as
     * processings and using the provided similarity function to compare words.
     */
    public SimpleNearestNeighborFinder(SemanticSpace sspace, SimilarityFunction similarity) {
        this(sspace, similarity, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates this {@code SimpleNearestNeighborFinder} with the specified
     * number of threads and using the provided similarity function to compare
     * words.
     */
    public SimpleNearestNeighborFinder(SemanticSpace sspace, 
                                       SimilarityFunction similarity,
                                       int numThreads) {
        this.sspace = sspace;
        this.simFunc = similarity;
        this.workQueue = WorkQueue.getWorkQueue(numThreads);
    }

    /**
     * {@inheritDoc}
     */
    public SortedMultiMap<Double,String> getMostSimilar(
            String word, int numberOfSimilarWords) {

        Vector v = sspace.getVector(word);
        // if the semantic space did not have the word, then return null
        if (v == null)
            return null;
        // Find the most similar words vectors to this word's vector, which will
        // end up including the word itself.  Therefore, increase the count by
        // one and remove the word's vector after it finished.
        SortedMultiMap<Double,String> mostSim = 
            getMostSimilar(v, numberOfSimilarWords + 1);
        Iterator<Map.Entry<Double,String>> iter = mostSim.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Double,String> e = iter.next();
            if (word.equals(e.getValue())) {
                iter.remove();
                break;
            }
        }
        return mostSim;
    }

    /**
     * {@inheritDoc}
     */
    public SortedMultiMap<Double,String> getMostSimilar(
             Set<String> terms, int numberOfSimilarWords) {
        if (terms.isEmpty())
            return null;
        // Compute the mean vector for all the terms
        DoubleVector mean = new DenseVector(sspace.getVectorLength());
        int found = 0;
        for (String term : terms) {
            Vector v = sspace.getVector(term);
            if (v == null)
                info(LOGGER, "No vector for term " + term);
            else {
                VectorMath.add(mean, v);
                found++;
            }
        }
        // If none of the provided vectors were in the space, then return null
        if (found == 0)
            return null;

        // Find the most similar words vectors to the mean vector.  There is
        // some chance that the returned list could includes the terms
        // themselves.  Therefore, increase the count by the number of terms
        // (which is the worst case) and remove the terms after it finished.
        SortedMultiMap<Double,String> mostSim = 
            getMostSimilar(mean, numberOfSimilarWords + terms.size());
        Iterator<Map.Entry<Double,String>> iter = mostSim.entrySet().iterator();
        Set<Map.Entry<Double,String>> toRemove = 
            new HashSet<Map.Entry<Double,String>>();
        while (iter.hasNext()) {
            Map.Entry<Double,String> e = iter.next();
            if (terms.contains(e.getValue()))
                toRemove.add(e);
        }
        for (Map.Entry<Double,String> e : toRemove)
            mostSim.remove(e.getKey(), e.getValue());

        // If we still have more words that were asked for, then prune out the
        // least similar
        while (mostSim.size() > numberOfSimilarWords) 
            mostSim.remove(mostSim.firstKey());

        return mostSim;
    }


    /**
     * Finds the <i>k</i> most similar words in the semantic space according to
     * the cosine similarity, returning a mapping from their similarity to the
     * word itself.
     *
     * @return the most similar words to the vector
     */
    public SortedMultiMap<Double,String> getMostSimilar(
            final Vector v, int numberOfSimilarWords) {

        if (v == null) 
            return null;
        final int k = numberOfSimilarWords;

        // Partition the sspace's words into sets, according to how many
        // processors are available to process them.
        int numThreads = workQueue.availableThreads();
        int numWords = sspace.getWords().size();
        List<List<String>> wordSets = 
            new ArrayList<List<String>>(numThreads);
        for (int i = 0; i < numThreads; ++i)
            wordSets.add(new ArrayList<String>(numWords / numThreads));
        Iterator<String> iter = sspace.getWords().iterator();
        for (int i = 0; iter.hasNext(); ++i)
            wordSets.get(i % numThreads).add(iter.next());
        
                
        // Create a global map to store the results of the principle vectors'
        // terms comparisons
        final SortedMultiMap<Double,String> kMostSimTerms =
            new BoundedSortedMultiMap<Double,String>(k, false);
        
        // Register a task group to process similarity each of the principle
        // component's terms in parallel.
        Object taskId = workQueue.registerTaskGroup(wordSets.size());
        for (List<String> words : wordSets) {
            final List<String> terms = words;

            // Create a new Runnable to evaluate the similarity for each of the
            // terms that were most similar to this principle vector
            workQueue.add(taskId, new Runnable() {
                    public void run() {
                        // Record the similarity in a local data structure so we
                        // avoid locking the global mostSimilar term map
                        SortedMultiMap<Double,String> localMostSimTerms =
                            new BoundedSortedMultiMap<Double,String>(k, false);
                        for (String term : terms) {
                            Vector tVec = sspace.getVector(term);
                            double sim = simFunc.sim(v, tVec);
                            localMostSimTerms.put(sim, term);
                        }
                        // Lock the global map and then add the local results
                        synchronized (kMostSimTerms) {
                            for (Map.Entry<Double,String> e : 
                                     localMostSimTerms.entrySet()) {
                                kMostSimTerms.put(e.getKey(), e.getValue());
                            }
                        }
                    }
                });
        }       
        workQueue.await(taskId);

        return kMostSimTerms;
    }
}
