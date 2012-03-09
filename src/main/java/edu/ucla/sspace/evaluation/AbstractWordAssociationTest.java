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

import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SynchronizedIterator;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordSimilarityEvaluation} test.
 *
 * @authro David Jurgens
 */ 
public abstract class AbstractWordAssociationTest 
        implements WordAssociationTest {

    /**
     * A mapping from a word pair to the human association judgement for it
     */
    protected final Map<Pair<String>,Double> wordPairToHumanJudgement;

    /** 
     *
     * @param wordPairToHumanJudgement A mapping from a word pair to the human
     *        association judgement for it
     */
    public AbstractWordAssociationTest(
            Map<Pair<String>,Double> wordPairToHumanJudgement) {
        this.wordPairToHumanJudgement = wordPairToHumanJudgement;
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
    public WordAssociationReport evaluate(final SemanticSpace sspace) {

        // setup concurrent data structures so that the similarity questions can
        // be run in parallel.
        int numThreads = Runtime.getRuntime().availableProcessors();

        final BlockingQueue<Runnable> workQueue =
            new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new WorkerThread(workQueue);
            t.start();
        }
        final Semaphore itemsProcessed = new Semaphore(0); 

        // Set up thread safe counters for the final data points we need to
        // report.
        final double testRange = getHighestScore() - getLowestScore();
        final AtomicInteger unanswered = new AtomicInteger();
        final AtomicInteger answered = new AtomicInteger();

        // Set up the data structures for storing the human and computed scores.
        int numQuestions = wordPairToHumanJudgement.size();
        final double[] compScores = new double[numQuestions];
        final double[] humanScores = new double[numQuestions];
        
        // Iterate over each of the questions, offering each one to the work
        // queue so that question evaluations can be done in parrallel.
        int question = 0;
        for (Map.Entry<Pair<String>, Double> e :
                wordPairToHumanJudgement.entrySet()) {

            // Set up final variables for the work queue.
            final Pair<String> p = e.getKey();
            final int index = question;
            final double humanScore = e.getValue();
            question++;

            humanScores[index] = humanScore;

            // Offer a new question to the work queue.
            workQueue.offer(new Runnable() {
                public void run() {
                    Double association = computeAssociation(sspace, p.x, p.y);

                    // Skip questions that cannot be answered with the provided
                    // semantic space.  Store the score for this question as the
                    // minimum double value.
                    if (association == null) {
                        unanswered.incrementAndGet();
                        compScores[index] = Double.MIN_VALUE;
                    } else {
                        answered.incrementAndGet();

                        // Scale the associated result to within the test's
                        // range of values
                        compScores[index] =
                            (association * testRange) + getLowestScore();
                    }
                    itemsProcessed.release();
                }
            });
        }
                
        // Wait 
        try { 
            itemsProcessed.acquire(numQuestions);
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }

        // Copy over the answered questions to a smaller array so that
        // unanswered questions do not affect the overal rating for this test. 
        // Note that since all unanswered questions are given a minimum double
        // value, by just skipping those scores we will have observed the
        // correct number of answered questions.
        double[] finalHumanScores = new double[answered.get()];
        double[] finalCompScores = new double[answered.get()];
        for (int i = 0, index = 0; i < numQuestions; ++i) {
            if (compScores[i] == Double.MIN_VALUE)
                continue;
            finalHumanScores[index] = humanScores[i];
            finalCompScores[index] = compScores[i];
            index++;
        }

        // Compute the final score for this test.
        double score = computeScore(humanScores, compScores);

        return new SimpleWordAssociationReport(
            wordPairToHumanJudgement.size(), score, unanswered.get());
    }

    /**
     * Returns the correlation between the computer generated scores and the
     * human evaluated scores.  Sub-classes can override this if the correlation
     * metric is not suitable for the data set.  Possible alternatives are mean
     * square error or simply the average computer generated score.
     */
    protected double computeScore(double[] humanScores, double[] compScores) {
        return Similarity.correlation(humanScores, compScores);
    }

    /**
     * Returns the lowest score possible for human judgments.  This score is
     * interpreted as the least associated.
     */
    protected abstract double getLowestScore();

    /**
     * Returns the highest score possible for human judgments.  This score is
     * interpreted as the most associated.
     */
    protected abstract double getHighestScore();

    /**
     * Returns the association of the two words on a scale of 0 to 1.
     * Subclasses should override this method to provide specific ways of
     * determining the association of two words in the semantic space, but
     * should ensure that the return value falls with the predefined scale.
     *
     * @return the assocation or {@code null} if either {@code word1} or {@code
     *         word2} are not in the semantic space
     */
    protected abstract Double computeAssociation(SemanticSpace sspace, 
                                                 String word1, String word2);
}
