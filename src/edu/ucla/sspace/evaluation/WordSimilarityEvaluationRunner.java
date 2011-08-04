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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.vector.Vector;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordSimilarityEvaluation} test.
 *
 * @authro David Jurgens
 */ 
public class WordSimilarityEvaluationRunner {

    /**
     * Evaluates the performance of a given {@code SemanticSpace} on a given
     * {@code WordSimilarityEvaluation} using the provided similarity metric.
     * Returns a {@link WordSimilarityReport} detailing the performance, with
     * similarity scores scaled by the lowest and highest human based similarity
     * ratings.
     *
     * @param sspace The {@link SemanticSpace} to test against
     * @param test The {@link WordSimilarityEvaluation} providing human
     *             similarity scores
     * @param vectorComparisonType The similarity measture to use
     *
     * @return A {@link WordSimilarityReport} detailing the performance
     */
    public static WordSimilarityReport evaluate(
            SemanticSpace sspace,
            WordSimilarityEvaluation test,
            Similarity.SimType vectorComparisonType) {
        Collection<WordSimilarity> wordPairs = test.getPairs();
        int unanswerable = 0;

        // Use lists here to keep track of the judgements for each word pair
        // that the SemanticSpace has vectors for.  This allows us to skip
        // trying to correlate human judgements for pairs that the S-Space
        // cannot handle.
        List<Double> humanJudgements = new ArrayList<Double>(wordPairs.size());
        List<Double> sspaceJudgements = new ArrayList<Double>(wordPairs.size());
        
        double testRange = test.getMostSimilarValue() - 
            test.getLeastSimilarValue();

        // Compute the word pair similarity using the given Semantic Space for
        // each word.
        for (WordSimilarity pair : wordPairs) {
            // get the vector for each word
            Vector firstVector = sspace.getVector(pair.getFirstWord());
            Vector secondVector = sspace.getVector(pair.getSecondWord());

            // check that the s-space had both words
            if (firstVector == null || secondVector == null) {
                unanswerable++;
                continue;
            }

            // use the similarity result and scale it based on the original
            // answers
            double similarity = 
                    Similarity.getSimilarity(vectorComparisonType, 
                                             firstVector, secondVector);
            double scaled = (similarity * testRange) +
                             test.getLeastSimilarValue();

            humanJudgements.add(pair.getSimilarity());
            sspaceJudgements.add(scaled);
        }
        
        // Calculate the correlation between the human judgements and the
        // semantic space judgements.
        double[] humanArr = new double[humanJudgements.size()];
        double[] sspaceArr = new double[humanJudgements.size()];

        for(int i = 0; i < humanArr.length; ++i) {
            humanArr[i] = humanJudgements.get(i);
            sspaceArr[i] = sspaceJudgements.get(i);
        }

        double correlation = Similarity.correlation(humanArr, sspaceArr);

        return new SimpleReport(wordPairs.size(), correlation, unanswerable);
    }

    /**
     * A simple implementation of a {@code Report} that just returns values
     * provided at the time of construction.
     */
    private static class SimpleReport implements WordSimilarityReport {
        
        /**
         * The total number of word pairs
         */
        private final int numWordPairs;

        /**
         * The correlation between the {@link SemanticSpace} judgements and the
         * human similarity judgements
         */
        private final double correlation;

        /**
         * The number of unaswnserable pairs.
         */
        private final int unanswerable;

        /**
         * Creates a simple report
         */
        public SimpleReport(int numWordPairs,
                            double correlation, 
                            int unanswerable) {
            this.numWordPairs = numWordPairs;
            this.correlation = correlation;
            this.unanswerable = unanswerable;
        }

        /**
         * {@inheritDoc}
         */
        public int numberOfWordPairs() {
            return numWordPairs;
        }

        /**
         * {@inheritDoc}
         */
        public double correlation() {
            return correlation;
        }

        /**
         * {@inheritDoc}
         */
        public int unanswerableQuestions() {
            return unanswerable;
        }

        /**
         * Returns a string describing the three values represented by this
         * {@link report}
         */
        public String toString() {
            return String.format("%.4f correlation; %d/%d unanswered",
                     correlation, unanswerable, numWordPairs);
        }
    }
}
