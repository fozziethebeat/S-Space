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

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.hal.WeightingFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Queue;


/**
 * A raw word co-occurrence {@link ContextGenerator}.  Each co-occurring word is
 * mapped to a unique dimension and feature scores are based on the distance
 * between the co-occurring word and the focus word in a particular context.
 *
 * @author Keith Stevens
 */
public class WordOccrrenceContextGenerator implements ContextGenerator {

    /**
     * The {@link BasisMapping} used to represent the feature space.
     */
    private final BasisMapping<String, String> basis;

    /**
     * The type of weight to apply to a the co-occurrence word based on its
     * relative location
     */
    private final WeightingFunction weighting;

    /**
     * The number of words to consider in one direction to create the symmetric
     * window
     */
    private final int windowSize;

    /**
     * Creates a new {@link WordOccrrenceContextGenerator}.
     *
     * @param weighting The {@link WeightingFunction} used to score each word
     *                co-occrrence, based on the distance from the focus word
     * @param windowSize The size of the sliding symmetric window composing a
     *                context
     */
    public WordOccrrenceContextGenerator(BasisMapping<String, String> basis,
                                         WeightingFunction weighting,
                                         int windowSize) {
        this.basis = basis;
        this.weighting = weighting;
        this.windowSize = windowSize;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(Queue<String> prevWords,
                                              Queue<String> nextWords) {
        SparseDoubleVector meaning = new CompactSparseVector();
        addContextTerms(meaning, prevWords, -1 * prevWords.size());
        addContextTerms(meaning, nextWords, 1);
        return meaning;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basis.numDimensions();
    }

    /**
     * {@inheritDoc}
     */
    public void setReadOnly(boolean readOnly) {
        basis.setReadOnly(readOnly);
    }

    /**
     * Adds a feature for each word in the context that has a valid dimension.
     * Feature are scored based on the context word's distance from the focus
     * word.
     */
    protected void addContextTerms(SparseDoubleVector meaning,
                                   Queue<String> words,
                                   int distance) {
        // Iterate through each of the context words.
        for (String term : words) {
            if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                // Ignore any features that have no valid dimension.
                int dimension = basis.getDimension(term);
                if (dimension == -1)
                    continue;

                // Add the feature to the context vector and increase the
                // distance from the focus word.
                meaning.set(dimension, weighting.weight(distance, windowSize));
                ++distance;
            }
        }
    }
}
