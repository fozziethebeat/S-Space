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

import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;


/** 
 * An abstract {@link DependencyContextGenerator} that creates a context vector
 * for a node in an array of {@link DependencyTreeNode}s by using any form of
 * co-occurrence information in the tree.  The {@link windowSize} words prior to
 * the focus word will be marked as having a negative index with respect to the
 * focus word and the words after the focus word will be marked with a positive
 * index.  Subclasses need only implement {@link #getFeature(DependencyTreeNode,
 * int) getFeature}.
 *
 * @author Keith Stevens
 */
public abstract class AbstractOccurrenceDependencyContextGenerator
        implements DependencyContextGenerator {

    /**
     * The {@link BasisMapping} used to represent the feature space.
     */
    private final BasisMapping<String, String> basis;

    /**
     * The maximum distance, left or right, from the focus word that will count
     * as a feature for any focus word.
     */
    private final int windowSize;

    /**
     * Constructs a new {@link AbstractOccurrenceDependencyContextGenerator}.
     */
    public AbstractOccurrenceDependencyContextGenerator(
            BasisMapping<String, String> basis,
            int windowSize) {
        this.basis = basis;
        this.windowSize = windowSize;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                              int focusIndex) {
        Queue<String> prevWords = new ArrayDeque<String>();
        for (int i = Math.max(0, focusIndex-windowSize-1); i < focusIndex; ++i)
            prevWords.add(getFeature(tree[i], i-focusIndex));
                
        Queue<String> nextWords = new ArrayDeque<String>();
        for (int i = focusIndex+1;
                 i < Math.min(focusIndex+windowSize+1, tree.length); ++i)
            nextWords.add(getFeature(tree[i], i-focusIndex));

        SparseDoubleVector focusMeaning = new CompactSparseVector();
        addContextTerms(focusMeaning, prevWords, -1 * prevWords.size());
        addContextTerms(focusMeaning, nextWords, 1);
        return focusMeaning;
    }

    /**
     * Returns a string representing the {@link DependencyTreeNode} which is
     * {@link dist} nodes away from the focus word currently being processed.
     */
    protected abstract String getFeature(DependencyTreeNode node, int dist);

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
                meaning.set(dimension, 1);
                ++distance;
            }
        }
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
}
