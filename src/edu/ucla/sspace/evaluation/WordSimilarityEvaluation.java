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

package edu.ucla.sspace.evaluation;

import java.util.Collection;

import edu.ucla.sspace.common.SemanticSpace;

/**
 * An evaluation metric that compares the human-judged similarity of word pairs
 * against the similarity judgements from a {@link SemanticSpace}.
 *
 * @author David Jurgens
 */
public interface WordSimilarityEvaluation {

    /**
     * Returns a collection of human similarity judgements for word pairs.
     */
    Collection<WordSimilarity> getPairs();

    /**
     * Returns the numeric similarity judgement that is equivalent to two words
     * being completely similar (i.e. identical).
     */
    double getMostSimilarValue();
    
    /**
     * Returns the numeric similarity judgement that is equivalent to two words
     * being completely dissimilar (i.e. identical).
     */
    double getLeastSimilarValue();

}
