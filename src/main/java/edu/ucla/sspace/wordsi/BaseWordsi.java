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

import edu.ucla.sspace.common.SemanticSpace;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import java.io.BufferedReader;


/**
 * This base class accepts or rejects key words based on a set of {@code
 * acceptedWords} and dispatches calls to a {@link ContextExtractor} so that
 * {@link Wordsi} sub-classes will be called with each generated vector.
 *
 * @author Keith Stevens
 */
public abstract class BaseWordsi implements Wordsi, SemanticSpace {

    /**
     * The set of words which should be represented by {@link Wordsi}.
     */
    private final Set<String> acceptedWords;

    /**
     * The {@link ContextExtractor} responsible for parsing documents and
     * creating context vectors.
     */
    private ContextExtractor extractor;

    /**
     * Creates a new {@link BaseWordsi}.
     *
     * @param acceptedWords The set of words which {@link Wordsi} should
     *        represent, may be {@code null} or empty.
     * @param trackSecondaryKeys If true, secondary key assignments will be
     *        tracked
     */
    public BaseWordsi(Set<String> acceptedWords,
                      ContextExtractor extractor) {
        this.acceptedWords = acceptedWords;
        this.extractor = extractor;
    }

    /**
     * {@inheritDoc}
     */
    public boolean acceptWord(String word) {
        return acceptedWords == null || 
               acceptedWords.isEmpty() ||
               acceptedWords.contains(word);
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "Wordsi";
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return extractor.getVectorLength();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) {
        extractor.processDocument(document, this);
    }
}
