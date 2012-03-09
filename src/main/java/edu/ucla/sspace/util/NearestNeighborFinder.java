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

import edu.ucla.sspace.vector.Vector;

import java.util.Set;


/**
 * An interface for utilities that finds the <i>k</i>-nearest neighbors of one
 * or more words in a given {@link SemanticSpace}
 */
public interface NearestNeighborFinder {

    /**
     * Finds the <i>k</i> most similar words in the semantic space according to
     * the cosine similarity, returning a mapping from their similarity to the
     * word itself.
     *
     * @return the most similar words, or {@code null} if the provided word was
     *         not in the semantic space.
     */
    SortedMultiMap<Double,String> getMostSimilar(
        String word, int numberOfSimilarWords);

    /**
     * Finds the <i>k</i> most similar words in the semantic space according to
     * the cosine similarity, returning a mapping from their similarity to the
     * word itself.
     *
     * @return the most similar words, or {@code null} if none of the provided
     *         word were not in the semantic space.
     */
    SortedMultiMap<Double,String> getMostSimilar(
        Set<String> terms, int numberOfSimilarWords);

    /**
     * Finds the <i>k</i> most similar words in the semantic space according to
     * the cosine similarity, returning a mapping from their similarity to the
     * word itself.
     *
     * @return the most similar words to the vector
     */
    SortedMultiMap<Double,String> getMostSimilar(
        Vector v, int numberOfSimilarWords);
}