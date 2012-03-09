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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.VectorIO;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * @author Keith Stevens
 */ 
public abstract class AbstractNormedWordPrimingTest 
        implements NormedWordPrimingTest {

    /**
     * A mapping from a word pair to the human association judgement for it
     */
    protected final Set<NormedPrimingQuestion> normedWordQuestions;

    /** 
     *
     * @param wordPairToHumanJudgement A mapping from a word pair to the human
     *        association judgement for it
     */
    public AbstractNormedWordPrimingTest(Set<NormedPrimingQuestion> primes) {
        this.normedWordQuestions = primes;
    }

    /**
     * Evaluates the performance of a given {@code SemanticSpace} on a given
     * {@code WordSimilarityEvaluation} using the provided similarity metric.
     * Returns a {@link WordSimilarityReport} detailing the performance, with
     * similarity scores scaled by the lowest and highest human based similarity
     * ratings.
     *
     * @param sspace The {@link SemanticSpace} to test against
     *
     * @return A {@link WordSimilarityReport} detailing the performance
     */
    public NormedWordPrimingReport evaluate(final SemanticSpace sspace) {
        Set<String> sspaceWords = sspace.getWords();

        // Setup the counts needed for computing the priming report.
        int totalQuestions = normedWordQuestions.size();
        int unanswered = 0;
        double totalCorrelation = 0;

        // Evaluate each priming question.  If the cue does not exist in the
        // semantic space, consider the question unanswerable.
        for (NormedPrimingQuestion question : normedWordQuestions) {
            if (sspaceWords.contains(question.getCue())) {
                Double correl = answerQuestion(sspace,sspaceWords,question);
                if (correl == null) {
                    unanswered++;
                    continue;
                }
                totalCorrelation += correl.doubleValue();
            }
            else 
                unanswered++;
        }

        double averageCorrelation =
            totalCorrelation / (totalQuestions - unanswered);
        return new SimpleNormedWordPrimingReport(
                totalQuestions, unanswered, averageCorrelation);
    }

    /**
     * Returns the correlation for a given {@link NormedPrimingQuestion} given a
     * {@link Semanticspace}.  Target words that do not exist in the {@link
     * SemanticSpace} are skipped and do not affect the correlation.
     */
    private Double answerQuestion(final SemanticSpace sspace,
                                  Set<String> sspaceWords,
                                  NormedPrimingQuestion question) {
        // Lists to contain the known and computed strenghts.
        List<Double> knownStrengthsList = new ArrayList<Double>();
        List<Double> computedStrengthsList = new ArrayList<Double>();

        // compute the strength between the cue and each possible target.  If
        // the target is not in the space, skip it.
        String cue = question.getCue();
        for (int i = 0; i < question.numberOfTargets(); i++) {
            String target = question.getTarget(i);
            double strength = question.getStrength(i);
            if (!sspaceWords.contains(target))
                continue;

            knownStrengthsList.add(strength);
            computedStrengthsList.add(computeStrength(sspace, cue, target));
        }

        if (knownStrengthsList.size() < 2)
            return null;

        // Convert the strength lists to arrays.
        double[] knownStrengths = new double[knownStrengthsList.size()];
        double[] computedStrengths = new double[knownStrengthsList.size()];
        for (int i = 0; i < knownStrengths.length; ++i) {
            knownStrengths[i] = knownStrengthsList.get(i);
            computedStrengths[i] = computedStrengthsList.get(i);
        }

        // Return the correlation between the known and computed strengths.
        return Similarity.spearmanRankCorrelationCoefficient(
                knownStrengths, computedStrengths);
    }


    /**
     * Returns the association of the two words on a scale of 0 to 1.
     * Subclasses should override this method to provide specific ways of
     * determining the association of two words in the semantic space, but
     * should ensure that the return value falls with the predefined scale.
     *
     * @return the assocation between {@code word1} and {@code word2}
     */
    protected abstract double computeStrength(SemanticSpace sspace, 
                                              String word1, String word2);

    /**
     * A simple {@link NormedWordPrimingReport} that serves as a struct.
     */
    public class SimpleNormedWordPrimingReport 
            implements NormedWordPrimingReport {

        /**
         * The number of cues in for this test.
         */
        private int numberOfCues;

        /**
         * The number of cues that could not be answered at all.
         */
        private int numberOfUnanswerableCues;

        /**
         * The average correlation between computed strengths and known
         * strengths for each answerable que.
         */
        private double averageCorrelation;

        /**
         * Creates a new {@link SimpleNormedWordPrimingReport}.
         */
        public SimpleNormedWordPrimingReport(int numberOfCues,
                                             int numberOfUnanswerableCues,
                                             double averageCorrelation) {
            this.numberOfCues = numberOfCues;
            this.numberOfUnanswerableCues = numberOfUnanswerableCues;
            this.averageCorrelation = averageCorrelation;
        }

        /**
         * {@inheritDoc}
         */
        public double averageCorrelation() {
            return averageCorrelation;
        }

        /**
         * {@inheritDoc}
         */
        public int numberOfCues() {
            return numberOfCues;
        }

        /**
         * {@inheritDoc}
         */
        public int numberOfUnanswerableCues() {
            return numberOfUnanswerableCues;
        }

        public String toString() {
            return String.format("Primed cues: %s\n" +
                                 "Unanswered cues: %s\n" +
                                 "Average Correlation: %f\n",
                                 numberOfCues, numberOfUnanswerableCues,
                                 averageCorrelation);
        }
    }
}
