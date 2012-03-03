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


/**
 * An interface for performing priming tests where there is no normed set of
 * responses to compare against.  These tests simply measure the associational
 * strength between a prime,target pair and prime, unrelated target pairs.  The
 * key result is the effect of related primes, where a high effect suggests that
 * that semantic space models the particular form of priming modeled by some
 * implemented test.
 *
 * @author Keith Stevens
 */
public interface WordPrimingTest {

    /**
     * Evaluates a {@link SemanticSpace} on a particular test of word priming
     * pairs.
     */
    public WordPrimingReport evaluate(SemanticSpace sspace);
}
