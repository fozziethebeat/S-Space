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

/**
 * A report of the performance of a {@link SemanticSpace} on a particular
 * {@link WordPrimingTest}.
 *
 * @author Keith Stevens
 */
public interface WordPrimingReport {

    /**
     * Returns the total number of word pairs.
     */
    int numberOfWordPairs();

    /**
     * Returns the priming score for related word pairs.
     */
    double relatedPriming();

    /**
     * Returns the priming score for unrelated word pairs.
     */
    double unrelatedPriming();

    /**
     * Returns the effect of priming, which is the difference bewtween the
     * priming score for related and unrelated pairs.
     */
    double effect();
}
