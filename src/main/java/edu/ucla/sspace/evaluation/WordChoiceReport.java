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


/**
 * A report of the performance of a {@link SemanticSpace} on a particular
 * {@link WordChoiceEvaluation} test.
 *
 * @author David Jurgens
 */
public interface WordChoiceReport {

    /**
     * Returns the total number of questions on the test.
     */
    int numberOfQuestions();

    /**
     * Returns the number of questions that were answered correctly.
     */
    int correctAnswers();

    /**
     * Returns the number of questions for which the {@link SemanticSpace}
     * could not give an answer due to missing word vectors in either the
     * prompt or the options.
     */
    int unanswerableQuestions();

    /**
     * Returns the score, ranged between 0 and 100, achieved on a particlar
     * evaluation.
     */
    double score();
}

