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

import java.util.Collection;
import java.util.LinkedList;


/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordChoiceEvaluation} test.
 *
 * @author David Jurgens
 */ 
public class WordChoiceEvaluationRunner {

    /**
     * Evaluates the performance of a given {@code SemanticSpace} on a given
     * {@code WordChoiceEvaluation} using the provided similarity metric.
     * Returns a {@link WordChoiceReport} detailing the performance.
     *
     * @param sspace The {@link SemanticSpace} to test against
     * @param test The {@link WordChoiceEvaluation} providing a set of multiple
     *             choice options
     * @param vectorComparisonType The similarity measture to use
     *
     * @return A {@link WordChoiceReport} detailing the performance
     */
    public static WordChoiceReport evaluate(
            SemanticSpace sspace,
            WordChoiceEvaluation test,
            Similarity.SimType vectorComparisonType) {
        Collection<MultipleChoiceQuestion> questions = test.getQuestions();
        int correct = 0;
        int unanswerable = 0;
        
        question_loop:
        // Answer each question by using the vectors from the provided Semantic
        // Space
        for (MultipleChoiceQuestion question : questions) {
            String promptWord = question.getPrompt();

            // get the vector for the prompt
            Vector promptVector = sspace.getVector(promptWord);

            // check that the s-space had the prompt word
            if (promptVector == null) {
                unanswerable++;
                continue;
            }

            int answerIndex = 0;
            double closestOption = Double.MIN_VALUE;

            // find the options whose vector has the highest similarity (or
            // equivalent comparison measure) to the prompt word.  The
            // running assumption hear is that for the value returned by the
            // comparison method, a high value implies more similar vectors.
            int optionIndex = 0;
            for (String optionWord : question.getOptions()) {
                
                // Get the vector for the option
                Vector optionVector = sspace.getVector(optionWord);
        
                // check that the s-space had the option word
                if (optionVector == null) {
                    unanswerable++;
                    continue question_loop;
                }
            
                double similarity = Similarity.getSimilarity(
                        vectorComparisonType, 
                        promptVector, optionVector);
            
                if (similarity > closestOption) {
                    answerIndex = optionIndex;
                    closestOption = similarity;
                }
                optionIndex++;
            }

            // see whether our guess matched with the correct index
            if (answerIndex == question.getCorrectAnswer()) {
                correct++;
            }
        }

        return new SimpleReport(questions.size(), correct, unanswerable);
    }

    /**
     * A simple implementation of a {@code Report} that just returns values
     * provided at the time of construction.
     */
    private static class SimpleReport implements WordChoiceReport {
    
        /**
         * The total number of questions 
         */
        private final int numQuestions;

        /**
         * The number of questions accurately answered by the {@link
         * SemanticSpace}
         */
        private final int correct;

        /**
         * The number of unaswnserable pairs.
         */
        private final int unanswerable;

        /**
         * Creates a simple report
         */
        public SimpleReport(int numQuestions, int correct, int unanswerable) {
            this.numQuestions = numQuestions;
            this.correct = correct;
            this.unanswerable = unanswerable;
        }

        /**
         * {@inheritDoc}
         */
        public int numberOfQuestions() {
            return numQuestions;
        }

        /**
         * {@inheritDoc}
         */
        public int correctAnswers() {
            return correct;
        }

        /**
         * {@inheritDoc}
         */
        public int unanswerableQuestions() {
            return unanswerable;
        }

        /**
         * {@inheritDoc}
         */
        public double score() {
            return (unanswerable == numQuestions) 
             ? 0d
             : ((100d * (double)correct) / (numQuestions - unanswerable));
        }

        /**
         * Returns a string describing the three values represented by this
         * {@link report}, with the accuracy reported as a percentage
         */
        public String toString() {
            return String.format("%.2f correct; %d/%d unanswered",
                    score(), 
                    unanswerable, numQuestions);
        }
    }
}
