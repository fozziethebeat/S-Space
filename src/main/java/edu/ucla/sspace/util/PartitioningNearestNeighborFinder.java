/*
 * Copyright 2011 David Jurgens
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

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.DirectClustering;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.VectorMapSemanticSpace;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.info;


/**
 * A class for finding the <i>k</i>-nearest neighbors of one or more words.  The
 * {@code NearestNeighborFinder} operates by generating a set of <b>principle
 * vectors</b> that reflect average words in a {@link SemanticSpace} and then
 * mapping each principle vector to the set of words to which it is closest.
 * Finding the nearest neighbor then entails finding the <i>k</i>-closest
 * principle vectors and comparing only their words, rather than all the words
 * in the space.  This dramatically reduces the search space by partitioning the
 * vectors of the {@code SemanticSpace} into smaller sets, not all of which need
 * to be searched.
 *
 * <p> The number of principle vectors is typically far less than the total
 * number of vectors in the {@code SemanticSpace}, but should be more than the
 * expected number of neighbors being searched for.  This value can be optimized
 * by minimizing the value of {@code c} in the equation {@code c = k * p + (k *
 * (|Sspace| / p))}, where {@code p} is the number of principle components,
 * {@code k} is the number of nearest neighbors to be found, and {@code
 * |Sspace|} is the size of the semantic space.
 *
 * <p> Instances of this class are also serializable.  If the backing {@code
 * SemanticSpace} is also serializable, the space will be saved.  However, if
 * the space is not serializable, its contents will be converted to a static
 * version and saved as a copy.
 */
public class PartitioningNearestNeighborFinder 
    implements NearestNeighborFinder, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(PartitioningNearestNeighborFinder.class.getName());

    /**
     * The semantic space from which the principle vectors are derived
     */
    private transient SemanticSpace sspace;
    
    /**
     * A mapping from principle vector to the terms that are closest to it
     */
    private final MultiMap<DoubleVector,String> principleVectorToNearestTerms;
    
    /**
     * The work queue where concurrent jobs are run during the nearest-neighbot
     * searching process
     */
    private transient WorkQueue workQueue;

    /**
     * Creates a new {@code NearestNeighborFinder} for the {@link
     * SemanticSpace}, using log<sub>e</sub>(|words|) principle vectors to
     * efficiently search for neighbors.
     *
     * @param sspace a semantic space to search
     */
    public PartitioningNearestNeighborFinder(SemanticSpace sspace) {
        this(sspace, (int)(Math.ceil(Math.log(sspace.getWords().size()))));
    }

    /**
     * Creates a new {@code NearestNeighborFinder} for the {@link
     * SemanticSpace}, using the specified number of principle vectors to
     * efficiently search for neighbors.
     *
     * @param sspace a semantic space to search
     * @param numPrincipleVectors the number of principle vectors to use in
     *        representing the content of the space.
     */
    public PartitioningNearestNeighborFinder(SemanticSpace sspace, 
                                             int numPrincipleVectors) {
        if (sspace == null)
            throw new NullPointerException();
        if (numPrincipleVectors > sspace.getWords().size()) 
            throw new IllegalArgumentException(
                "Cannot have more principle vectors than " +
                "word vectors: " + numPrincipleVectors);    
        else if (numPrincipleVectors < 1) 
            throw new IllegalArgumentException(
                "Must have at least one principle vector");
        this.sspace = sspace;
        principleVectorToNearestTerms = new HashMultiMap<DoubleVector,String>();
        workQueue = new WorkQueue();
        computePrincipleVectors(numPrincipleVectors);
    }

    /**
     * Computes the principle vectors, which represent a set of prototypical
     * terms that span the semantic space
     *
     * @param numPrincipleVectors
     */
    private void computePrincipleVectors(int numPrincipleVectors) {

        // Group all of the sspace's vectors into a Matrix so that we can
        // cluster them
        final int numTerms = sspace.getWords().size();
        final List<DoubleVector> termVectors = 
            new ArrayList<DoubleVector>(numTerms);
        String[] termAssignments = new String[numTerms];
        int k = 0;
        for (String term : sspace.getWords()) {
            termVectors.add(Vectors.asDouble(sspace.getVector(term)));
            termAssignments[k++] = term;
        }

        Random random = new Random();

        final DoubleVector[] principles = new DoubleVector[numPrincipleVectors];
        for (int i = 0; i < principles.length; ++i)
            principles[i] = new DenseVector(sspace.getVectorLength());

        // Randomly assign the data set to different intitial clusters 
        final MultiMap<Integer,Integer> clusterAssignment = 
            new HashMultiMap<Integer,Integer>();
        for (int i = 0; i < numTerms; ++i) 
            clusterAssignment.put(random.nextInt(numPrincipleVectors), i);
        
        int numIters = 1;
        for (int iter = 0; iter < numIters; ++iter) {
            verbose(LOGGER, "Computing principle vectors (round %d/%d)", iter+1, numIters);
            // Compute the centroids
            for (Map.Entry<Integer,Set<Integer>> e
                     : clusterAssignment.asMap().entrySet()) {
                int cluster = e.getKey();
                DoubleVector principle = new DenseVector(sspace.getVectorLength());
                principles[cluster] = principle;
                for (Integer row : e.getValue())
                    VectorMath.add(principle, termVectors.get(row));
            }

            // Reassign each element to the centroid to which it is closest
            clusterAssignment.clear();
            final int numThreads = workQueue.availableThreads();
            Object key = workQueue.registerTaskGroup(numThreads);

            for (int threadId_ = 0; threadId_ < numThreads; ++threadId_) {
                final int threadId = threadId_;
                workQueue.add(key, new Runnable() {
                        public void run() {
                            // Thread local cache of all the cluster assignments
                            MultiMap<Integer,Integer> clusterAssignment_ = 
                                new HashMultiMap<Integer,Integer>();
                            // For each of the vectors that this thread is
                            // responsible for, find the principle vector to
                            // which it is closest
                            for (int i = threadId; i < numTerms; i += numThreads) {
                                DoubleVector v = termVectors.get(i);
                                double highestSim = -Double.MAX_VALUE;
                                int pVec = -1;
                                for (int j = 0; j < principles.length; ++j) {
                                    DoubleVector principle = principles[j];
                                    double sim = Similarity.cosineSimilarity(
                                        v, principle);
                                    assert sim >= -1 && sim <= 1 : "similarity "
                                        + " to principle vector " + j + " is "
                                        + "outside the expected range: " + sim;
                                    if (sim > highestSim) {
                                        highestSim = sim;
                                        pVec = j;
                                    }
                                }
                                assert pVec != -1 : "Could not find match to "
                                    + "any of the " + principles.length + 
                                    " principle vectors";
                                clusterAssignment_.put(pVec, i);
                            }
                            // Once all the vectors for this thread have been
                            // mapped, update the thread-shared mapping.  Do
                            // this here, rather than for each comparison to
                            // minimize the locking
                            synchronized (clusterAssignment) {
                                clusterAssignment.putAll(clusterAssignment_);
                            }
                        }
                    });
            }
            workQueue.await(key);
        }

        double mean = numTerms / (double)numPrincipleVectors;
        double variance = 0d;
        for (Map.Entry<Integer,Set<Integer>> e
                 : clusterAssignment.asMap().entrySet()) {
            Set<Integer> rows = e.getValue();
            Set<String> terms = new HashSet<String>();
            for (Integer i : rows)
                terms.add(termAssignments[i]);
             verbose(LOGGER, "Principle vectod %d is closest to %d terms",
                     e.getKey(), terms.size());
            double diff = mean - terms.size();
            variance += diff * diff;
            principleVectorToNearestTerms.putMany(
                principles[e.getKey()], terms);
        }

        verbose(LOGGER, "Average number terms per principle vector: %f, "+
                "(%f stddev)", mean, Math.sqrt(variance / numPrincipleVectors));
    }

    /**
     * {@inheritDoc}
     */
    public SortedMultiMap<Double,String> getMostSimilar(
            final String word, int numberOfSimilarWords) {
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
     * {@inheritDoc}
     */
    public SortedMultiMap<Double,String> getMostSimilar(
            final Vector v, int numberOfSimilarWords) {

        if (v == null) 
            return null;
        final int k = numberOfSimilarWords;

        // Find the k most similar principle vectors to the provided word's
        // vector.  
        final SortedMultiMap<Double,Map.Entry<DoubleVector,Set<String>>>
            mostSimilarPrincipleVectors = new BoundedSortedMultiMap
                <Double,Map.Entry<DoubleVector,Set<String>>>(k, false);        
        for (Map.Entry<DoubleVector,Set<String>> e : 
                 principleVectorToNearestTerms.asMap().entrySet()) { 
            DoubleVector pVec = e.getKey();
            double sim = Similarity.cosineSimilarity(v, pVec);
            mostSimilarPrincipleVectors.put(sim, e);
        }
        
        // Create a global map to store the results of the principle vectors'
        // terms comparisons
        final SortedMultiMap<Double,String> kMostSimTerms =
            new BoundedSortedMultiMap<Double,String>(k, false);
        
        // Register a task group to process similarity each of the principle
        // component's terms in parallel.
        Object taskId = workQueue.registerTaskGroup(
            mostSimilarPrincipleVectors.values().size());
        int termsCompared = 0;
        int i =0;
        for (Map.Entry<DoubleVector,Set<String>> e : 
                 mostSimilarPrincipleVectors.values()) {
            final Set<String> terms = e.getValue();
            termsCompared += terms.size();
            // Create a new Runnable to evaluate the similarity for each of the
            // terms that were most similar to this principle vector
            final int i_ = i++;
            workQueue.add(taskId, new Runnable() {
                    public void run() {
                        // Record the similarity in a local data structure so we
                        // avoid locking the global mostSimilar term map
                        SortedMultiMap<Double,String> localMostSimTerms =
                            new BoundedSortedMultiMap<Double,String>(k, false);
                        for (String term : terms) {
                            Vector tVec = sspace.getVector(term);
                            double sim = Similarity.cosineSimilarity(v, tVec);
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
        verbose(LOGGER, "Compared %d of the total %d terms to find the " +
                "%d-nearest neighbors", termsCompared, 
                sspace.getWords().size(), k);

        return kMostSimTerms;
    }

    /**
     * Deserializes this finder, restarting the work queue as necessary.
     */
    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // Read the initial state
        ois.defaultReadObject();
        // Restart the thread pool
        workQueue = new WorkQueue();
        sspace = (SemanticSpace)(ois.readObject());
    }
    
    /**
     * Writes the state of the finder to the stream, handling the case where the
     * backing {@code SemanticSpace} is not serializable.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        // If the SemanticSpace that was provided is already serializable, then
        // just write it to the stream as is
        if (sspace instanceof Serializable) {
            out.writeObject(sspace);
        }
        // Otherwise, in order to serialize this instance, wrap the data in a
        // dummy SemanticSpace that contains the equivalent term-vector mapping
        else {
            verbose(LOGGER, "%s is not serializable, so writing a copy " +
                    "of the data", sspace);
            Map<String,Vector> termToVector = 
                new HashMap<String,Vector>(sspace.getWords().size());
            for (String term : sspace.getWords()) 
                termToVector.put(term, sspace.getVector(term));
            SemanticSpace copy = new VectorMapSemanticSpace<Vector>(
                termToVector, "copy of " + sspace,
                sspace.getVectorLength());
            out.writeObject(copy);
        }
    }
}
