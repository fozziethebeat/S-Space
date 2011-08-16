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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.util.Map;
import java.util.Queue;


/**
 * A random indexing based {@link ContextGenerator}.  This generator creates a
 * context vector by summing the index vectors associated with co-occurring
 * words.  When in normal mode, an index vector is generated for every observed
 * word.  When in read only mode, no new index vector are generated.  If a
 * {@link PermutationFunction} is provided, the index vectors will be permuted
 * based on the distance between the co-occurring word and the focus word in the
 * context.
 *
 * @see RandomIndexing
 *
 * @author Keith Stevens
 */
public class RandomIndexingContextGenerator implements ContextGenerator {

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
     * When true, the generator will not create dimensions for unmapped featues.
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
    public RandomIndexingContextGenerator(Map<String, TernaryVector> indexMap,
                                          PermutationFunction<TernaryVector> perm,
                                          int indexVectorLength) {
        this.indexMap = indexMap;
        this.permFunc = perm;
        this.indexVectorLength = indexVectorLength;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(Queue<String> prevWords,
                                              Queue<String> nextWords) {
        SparseDoubleVector meaning = new CompactSparseVector(indexVectorLength);
        addContextTerms(meaning, prevWords, -1 * prevWords.size());
        addContextTerms(meaning, nextWords, 1);
        return meaning;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return indexVectorLength;
    }

    /**
     * {@inheritDoc}.
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Adds the index vector for each co-occurring word in the context.    Index
     * vectors are permuted if {@code permFunc} is not {@code null}.    When in read
     * only mode, only existing index vector are used.
     */
    protected void addContextTerms(SparseDoubleVector meaning,
                                   Queue<String> words,
                                   int distance) {
        // Iterate through the words in the context.
        for (String term : words) {
            if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {

                // If in read only mode, ignore any terms that are not already in the
                // index map.
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
                ++distance;
            }
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
}
