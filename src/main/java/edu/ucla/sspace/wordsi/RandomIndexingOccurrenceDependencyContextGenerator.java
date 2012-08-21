/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.hal.EvenWeighting;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.util.Map;
import java.util.Queue;


/**
 * @author Keith Stevens
 */
public class RandomIndexingOccurrenceDependencyContextGenerator
    extends AbstractOccurrenceDependencyContextGenerator {

    /**
     * A mapping from strings to {@code IntegerVector}s which represent an index
     * vector.
     */
    private final Map<String, TernaryVector> indexMap;

    /**
     * The {@code PermutationFunction} to use for co-occurrances.
     */
    private final PermutationFunction<TernaryVector> permFunc;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorLength;

    /**
     * Set to true when the index map should be treated as a read only
     * structure.
     */
    private boolean readOnly;

    /**
     * Creates a new {@link RandomIndexingContextGenerator}.
     *
     * @param indexMap The map responsible for mapping co-occurring terms to
     *        their index vectors.  This map should generate index vectors for
     *        words that are not currently mapped when in normal mode.
     * @param perm A {@link PermutationFunction} for {@link TernaryVector}s.
     * @param indexVectorLength The number of dimensions in each index vector.
     */
    public RandomIndexingOccurrenceDependencyContextGenerator(
            Map<String, TernaryVector> indexMap,
            PermutationFunction<TernaryVector> perm,
            int indexVectorLength,
            int windowSize) {
        super(null, new EvenWeighting(), windowSize);

        this.indexMap = indexMap;
        this.permFunc = perm;
        this.indexVectorLength = indexVectorLength;
        this.readOnly = false;
    }

    /**
     * Returns the word stored by {@code node}.
     */
    public String getFeature(DependencyTreeNode node, int index) {
        return node.word();
    }

    /**
     * Adds an index vector corresponding to each word in {@code words} to
     * {@code meaning}.
     */
    protected void addContextTerms(SparseDoubleVector meaning,
                                   Queue<String> words,
                                   int distance) {
        --distance;
        // Iterate through the words in the context.
        for (String term : words) {
            ++distance;
            if (term.equals(IteratorFactory.EMPTY_TOKEN))
                continue;

            // Skip words that are not stored in the map if we are in read only
            // mode.
            if (readOnly && !indexMap.containsKey(term))
                continue;

            // Get the index vector for the word.
            TernaryVector termVector = indexMap.get(term);
            if (termVector == null)
                continue;
            
            // Permute the index vector if a permutation function is provided.
            if (permFunc != null)
                termVector = permFunc.permute(termVector, distance);

            // Add the index vector and update the distance.
            add(meaning, termVector);
        }
    }

    /**
     * Adds a {@link TernaryVector} to a {@link IntegerVector}
     */
    private void add(SparseDoubleVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }
    
    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return indexVectorLength;
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        readOnly = true;
    }
}
