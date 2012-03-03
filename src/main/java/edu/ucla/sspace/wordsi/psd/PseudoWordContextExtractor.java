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

package edu.ucla.sspace.wordsi.psd;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.ContextGenerator;
import edu.ucla.sspace.wordsi.Wordsi;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Map;


/**
 * A pseudo word based {@link ContextExtractor}.  A mapping from real tokens to
 * pseudo words is used to automatically replace tokens in a corpus while {@link
 * Wordsi} processes contexts.  Only pseudo words are represented in the {@link
 * Wordsi} space.  When a token is encountered, if it has a pseudo word mapping,
 * that instance is replaced with the pseudo word mapping.  A context vector
 * will be generated for the context surrounded that word instance, and the
 * pseudo word replacement will serve as the primary key for the reporter and
 * the raw token will serve as the secondary key.  The pseudo word will then
 * replace the raw token in the context for all other words, and thus serve as a
 * feature in place of the real token.
 *
 * @author Keith Stevens
 */
public class PseudoWordContextExtractor implements ContextExtractor {

    /**
     * A single empty token.
     */
    private static final String EMPTY = "";

    /**
     * The mapping from real tokens to their pseudo word replacements.
     */
    private final Map<String, String> pseudoWordMap;

    /**
     * The generator responsible for creating context vectors from a sliding
     * window of text.
     */
    private final ContextGenerator generator;

    /**
     * The size of the sliding window of text, in a single direction.
     */
    private final int windowSize;

    /**
     * Creates a new {@link PseudoWordContextExtracto}.
     *
     * @param generator The {@link ContextGenerator} responsible for creating
     *        context vectors
     * @param windowSize The number of words before and after the focus word
     *        which compose a context
     * @param pseudoWordMap The mapping from real words to their pseudo word
     *        replacements
     */
    public PseudoWordContextExtractor(ContextGenerator generator,
                                      int windowSize,
                                      Map<String, String> pseudoWordMap) {
        this.pseudoWordMap = pseudoWordMap;
        this.generator = generator;
        this.windowSize = windowSize;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return generator.getVectorLength();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();
        Queue<String> nextRealWord = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < windowSize && it.hasNext(); ++i)
            addNextToken(it.next(), nextWords, nextRealWord);

        // Iterate through each of the words in the context, generating context
        // vectors for each acceptable word.
        String focusWord = null;
        String replacementWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            replacementWord = nextRealWord.remove();

            // Advance the sliding window to the right.
            if (it.hasNext())
                addNextToken(it.next(), nextWords, nextRealWord);

            // Represent the word if wordsi is willing to process it.
            if (!replacementWord.equals(EMPTY)) {
                SparseDoubleVector contextVector = generator.generateContext(
                        prevWords, nextWords);
                wordsi.handleContextVector(
                        focusWord, replacementWord, contextVector);
            }

            // Advance the sliding window to the right.
            prevWords.offer(focusWord);
            if (prevWords.size() > windowSize)
                prevWords.remove();
        }
    }

    /**
     * If {@code token} is part of a pseudo word, the replacement psedo word
     * will be stored in {@code nextWords} and {@code token} is stored in {@code
     * nextRealWords}.  Otherwise, {@code token} is stored in {@code nextWords}
     * and and empty string is stored in {@code nextRealWords}.
     */
    private void addNextToken(String token,
                              Queue<String> nextWords,
                              Queue<String> nextRealWords) {
        String replacement = pseudoWordMap.get(token);
        if (replacement == null) {
            nextWords.offer(token);
            nextRealWords.offer(EMPTY);
        } else {
            nextWords.offer(replacement);
            nextRealWords.offer(token);
        }
    }
}
