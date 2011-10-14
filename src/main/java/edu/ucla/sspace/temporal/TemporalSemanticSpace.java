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

package edu.ucla.sspace.temporal;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.SortedSet;

/**
 * A common interface for interacting with semantic space models of meaning that 
 * include time information in their word representation.
 *
 * @see SemanticSpace
 * 
 * @author David Jurgens
 */
public interface TemporalSemanticSpace extends SemanticSpace {

    /**
     * Processes the contents of the provided reader as a document, using the
     * current time as the timestamp.
     *
     * @param document a reader that allows access to the text of the document
     *
     * @throws IOException if any error occurs while reading the document
     */
    void processDocument(BufferedReader document) throws IOException;

    /**
     * Processes the contents of the provided buffer as a document, using the
     * provided timestamp as the date when the document was written.
     *
     * @param document a reader that allows access to the text of the document
     * @param timestamp the time at which the document was written
     *
     * @throws IOException if any error occurs while reading the document
     */
    void processDocument(BufferedReader document, long timestamp) 
	    throws IOException;
    
    /**
     * Returns the time for the earliest semantics contained within this space.
     */
    Long startTime();

    /**
     * Returns the time for the latest semantics contained within this space.
     */
    Long endTime();

    /**
     * Returns the time steps in which the provided word occurs (optional
     * operation).
     *
     * @param word
     */
    SortedSet<Long> getTimeSteps(String word);
    
    /**
     * Returns the provided word's semantic vector based on all temporal
     * occurrences occurring on or after the provided timestamp (optional
     * operation).
     *
     * @param word a word in the semantic space
     *
     * @param startTime a UNIX timestamp that denotes the time after which all
     *        occurrences of the provided word should be counted.
     * 
     * @return the semantic vector for the word after the provided time or
     *         {@code null} if the word was not in the space.
     */
    Vector getVectorAfter(String word, long startTime);

    /**
     * Returns the provided word's semantic vector based on all temporal
     * occurrences before the provided timestamp (optional operation).
     *
     * @param word a word in the semantic space
     *
     * @param endTime a UNIX timestamp that denotes the time before which all
     *        occurrences of the provided would should be counted.
     * 
     * @return the semantic vector for the word after the provided time or
     *         {@code null} if the word was not in the space.
     */
    Vector getVectorBefore(String word, long endTime);

    /**
     * Returns the provided word's semantic vector based on all temporal
     * occurrences that happened on or after the start timestamp but before the
     * ending timestamp (optional operation).
     *
     * @param word a word in the semantic space
     *
     * @param startTime a UNIX timestamp that denotes the time before which
     *        no occurrences of the word should be counted.
     *
     * @param endTime a UNIX timestamp that denotes the time after which no
     *        occurrences of the word should be counted.
     * 
     * @return the semantic vector for the word after the provided time or
     *         {@code null} if the word was not in the space.
     *
     * @throws IllegalArgumentException if {@code startTime} &gt; {@code
     *         endTime}
     */
    Vector getVectorBetween(String word, long startTime, long endTime);

    /**
     * Returns the provided word's semantic vector based on all temporal
     * occurrences.
     *
     * @param word {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    Vector getVector(String word);
}
