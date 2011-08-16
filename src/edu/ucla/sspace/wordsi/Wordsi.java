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

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * An interface for all Wordsi implementations.  A complete {@link Wordsi}
 * implementation will likely contain four parts: a {@link ContextExtractor}, a
 * clustering method, and a {@link ContextAssignmentMap}, and a {@link
 * AssignmentReporter}.  The {@link Context Extractor} will genrate context
 * vectors for a set of words within a given BufferedReader and call {@code
 * handleContextVector} for each context vector that is generated.  Each context
 * vector can be index by two keys: the primary key, which is generally the
 * focus word for the context vectors and the secondary key, which is either the
 * same as the focus word or an additional value such as a SenseEval/SemEval
 * instance identifier.  The {@link ContextAssignmentMap} is reponsible for
 * recording which secondary keys and context id's are assigned to each focus
 * term, in many cases, this is not neccesary, but if the exact clustering for
 * each context is required, one should use a {@link ContextAssignmentMap}.  The
 * clustering method will assign the context vector to some cluster, either
 * immediately or by storing the context vectors and performing a batch
 * clustering.  The {@link AssignmentReporter} is reponsible for reporting which
 * context vectors were assigned to which clusters.  The three major components
 * to {@link Wordsi} are separated so that each various context extraction
 * algorithms can be combined with various clustering algorithms and reporting
 * methods.  
 *
 * </p>
 *
 * Implementations are suggested to subclass {@link BaseWordsi}, since it
 * provides some methods for accepting and rejecting terms and dispatching the
 * {@link ContextExtractor}.
 *
 *
 * @see ContextExtractor
 * @see AssignmentReporter
 *
 * @author Keith Stevens
 */
public interface Wordsi {

    /**
     * Returns true if this {@link Wordsi} implementation should generate a
     * semantic vector for {@code word}.
     */
    boolean acceptWord(String word);

    /** Performs some operation with {@code contextVector}, which can be indexed
     * by either {@code primaryKey}, {@code secondaryKey}, or both.  This
     * operation will likely assign the {@code contextVector} to some cluster
     * immediately or store the {@code contextVector} so that it may be
     * clustered with all other other context vecetors generated for {@code
     * primaryKey}.
     *
     * </p>
     *
     * The {@code secondaryKey} does not need to be used, but some experiments
     * may require it, such as the SenseEval/SemEval evaluation or pseudo-word
     * disambiguation.  For SenseEval/SemEval evaluations, a {@link
     * SenseEvalContextExtractor} should be used, which will provide the context
     * id as the {@code secondaryKey}; reporting should be done with a {@link
     * SenseEvalReporter}.  For pseudo-word disambiguation/discrimination, a
     * {@link PseudoWordContextExtractor} should be used, which will create
     * pseudo-words for some set of tokens.  This extractor will use the
     * pseudo-word for the {@code primaryKey} and the original token as the
     * {@code secondaryKey}.
     *
     * @param primaryKey The primary key for {@code contextVector}
     * @param secondarykey A secondary key for {@code contextVector}
     * @param contextVector a {@code SparseDoubleVector} that represents a
     *        single context for a word
     */
    void handleContextVector(String primaryKey, String secondaryKey,
                             SparseDoubleVector contextVector);
}

