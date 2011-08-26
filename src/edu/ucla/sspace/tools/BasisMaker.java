/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.hal.LinearWeighting;
import edu.ucla.sspace.hal.WeightingFunction;

import edu.ucla.sspace.mains.GenericMain;

import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;
import edu.ucla.sspace.matrix.NoTransform;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.BoundedSortedMap;
import edu.ucla.sspace.util.ReflectionUtil;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;


/**
 * This main creates a {@link BasisMapping} based on the unique terms found in a
 * document set and serializes it to disk.
 *
 * @author Keith Stevens
 */
public class BasisMaker extends GenericMain {

    /**
     * {@inheritDoc}
     */
    protected void addExtraOptions(ArgOptions options) { 
        options.addOption('T', "matrixTransform",
                          "Specifies the matrix transform that should be applied " +
                          "to co-occurrence counts after they have been generated",
                          true, "CLASSNAME", "Optional");
        options.addOption('b', "basisSize",
                          "Specifies the total desired size of the basis " +
                          "(Default: 10000)",
                          true, "INT", "Optional");
        options.addOption('w', "windowSize",
                          "Specifies the sliding window size (Default: 5)",
                          true, "INT", "Optional");
        options.addOption('p', "printWeights",
                          "If true, each saved word and it's associated weight " +
                          "will be printed to standard out",
                          false, null, "Optional");
    }

    /**
     * {@inheritDoc}
     */
    protected SemanticSpace getSpace() {
        Transform transform = null;
        if (argOptions.hasOption('T'))
            transform = ReflectionUtil.getObjectInstance(
                    argOptions.getStringOption('T'));
        else
            transform = new NoTransform();
        int bound = argOptions.getIntOption('b', 10000);
        int windowSize = argOptions.getIntOption('w', 5);
        return new OccurrenceCounter(transform, bound, windowSize);
    }

    /**
     * Saves the {@link BasisMapping} created from the {@link
     * OccurrenceCounter}.
     */
    protected void saveSSpace(SemanticSpace sspace, File outputFile)
            throws IOException{
        BasisMapping<String, String> savedTerms = new StringBasisMapping();
        for (String term : sspace.getWords())
            savedTerms.getDimension(term);

        ObjectOutputStream ouStream = new ObjectOutputStream(
                new FileOutputStream(outputFile));
        ouStream.writeObject(savedTerms);
        ouStream.close();
    }

    /**
     * A simple term {@link SemanticSpace} implementation that counts word
     * co-occurrences, performs a transform, and then scores each recorded basis
     * dimension based on the row summed scores for each word.
     */
    public class OccurrenceCounter implements SemanticSpace {

        /**
         * The matrix used for storing weight co-occurrence statistics of those
         * words that occur both before and after.
         */
        private final AtomicGrowingSparseHashMatrix cooccurrenceMatrix;

        /**
         * The type of weight to apply to a the co-occurrence word based on its
         * relative location
         */
        private final WeightingFunction weighting;

        /**
         * The {@link BasisMapping} used to record dimensions.
         */
        private final BasisMapping<String, String> basis;

        /**
         * The final scores for each word in the {@code basis}.
         */
        private final Map<String, Double> wordScores;

        /**
         * The {@link Transform} class used to rescore each word.
         */
        private final Transform transform;

        /**
         * The sliding window size used when traversing documents.
         */
        private final int windowSize;

        /**
         * Creates a new {@link OccurrenceCounter}.
         */
        public OccurrenceCounter(Transform transform,
                                 int bound,
                                 int windowSize) {
            cooccurrenceMatrix = new AtomicGrowingSparseHashMatrix();
            basis = new StringBasisMapping();
            wordScores = new BoundedSortedMap<String, Double>(bound);
            weighting = new LinearWeighting();

            this.transform = transform;
            this.windowSize = windowSize;
        }

        /**
         * {@inheritDoc}
         */
        public void processDocument(BufferedReader document)
               throws IOException {
            Queue<String> nextWords = new ArrayDeque<String>();
            Queue<String> prevWords = new ArrayDeque<String>();
                    
            Iterator<String> documentTokens = 
                IteratorFactory.tokenizeOrdered(document);
                    
            String focus = null;

            // Rather than updating the matrix every time an occurrence is seen,
            // keep a thread-local count of what needs to be modified in the
            // matrix and update after the document has been processed.    This
            // saves potential contention from concurrent writes.
            Map<Pair<Integer>,Double> matrixEntryToCount = 
                    new HashMap<Pair<Integer>,Double>();
                    
            //Load the first windowSize words into the Queue                
            for(int i = 0;    i < windowSize && documentTokens.hasNext(); i++)
                nextWords.offer(documentTokens.next());
                    
            while(!nextWords.isEmpty()) {
                // Load the top of the nextWords Queue into the focus word
                focus = nextWords.remove();

                // Add the next word to nextWords queue (if possible)
                if (documentTokens.hasNext())
                    nextWords.offer(documentTokens.next());

                // If the filter does not accept this word, skip the semantic
                // processing, continue with the next word
                if (focus.equals(IteratorFactory.EMPTY_TOKEN)) {
                    int focusIndex = basis.getDimension(focus);
                    
                    countOccurrences(nextWords, focusIndex,
                                     1, matrixEntryToCount);
                    countOccurrences(prevWords, focusIndex,
                                     -prevWords.size(), matrixEntryToCount);
                }

                // last, put this focus word in the prev words and shift off the
                // front if it is larger than the window
                prevWords.offer(focus);
                if (prevWords.size() > windowSize)
                    prevWords.remove();
            }

            // Once the document has been processed, update the co-occurrence
            // matrix accordingly.
            for (Map.Entry<Pair<Integer>,Double> e : matrixEntryToCount.entrySet()){
                Pair<Integer> p = e.getKey();
                cooccurrenceMatrix.addAndGet(p.x, p.y, e.getValue());
            }                                        
        }

        /**
         * Adds a occurnce count for each term in {@code words} according to
         * it's distance from the focus word.
         */
        private void countOccurrences(Queue<String> words,
                                      int focusIndex,
                                      int wordDistance,
                                      Map<Pair<Integer>, Double> entryCounts) {
            // Iterate through the words occurring after and add values
            for (String term : words) {
                // skip adding co-occurence values for words that are not
                // accepted by the filter
                if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                    int index = basis.getDimension(term);
                    
                    // Get the current number of times that the focus word has
                    // co-occurred with this word appearing after it.    Weight
                    // the word appropriately based on distance
                    Pair<Integer> p = new Pair<Integer>(focusIndex, index);
                    double value = weighting.weight(wordDistance, windowSize);
                    Double curCount = entryCounts.get(p);
                    entryCounts.put(p, (curCount == null) ? value : value + curCount);
                }
                wordDistance++;
            }
        }

        /**
         * {@inheritDoc}
         */
        public Set<String> getWords() {
            return Collections.unmodifiableSet(wordScores.keySet());
        }

        /**
         * {@inheritDoc}
         */
        public DoubleVector getVector(String word) {
            Double score = wordScores.get(word);
            return (score == null)
                ? new DenseVector(new double[] {0})
                : new DenseVector(new double[] {score});
        }

        /**
         * {@inheritDoc}
         */
        public int getVectorLength() {
            return 1;
        }

        /**
         * {@inheritDoc}
         */
        public void processSpace(Properties properties) {
            SparseMatrix cleanedMatrix = (SparseMatrix) transform.transform(
                    cooccurrenceMatrix);
            for (String term : basis.keySet()) {
                int index = basis.getDimension(term);
                SparseDoubleVector sdv = cleanedMatrix.getRowVector(index);

                double score = 0;
                for (int i : sdv.getNonZeroIndices())
                    score += sdv.get(i);

                wordScores.put(term, score);
            }
        }

        /**
         * {@inheritDoc}
         */
        public String getSpaceName() {
            return "BasisMaker";
        }
    }
}
