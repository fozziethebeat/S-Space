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

package edu.ucla.sspace.wordsi.semeval;

import edu.ucla.sspace.wordsi.ContextExtractor;
import edu.ucla.sspace.wordsi.ContextGenerator;
import edu.ucla.sspace.wordsi.Wordsi;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.SparseDoubleVector;

import edu.ucla.sspace.vector.VectorIO;

import java.io.BufferedReader;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;


/**
 * A {@link ContextExtractor} for handling SemEval or SenseEval corpora.  For
 * each document, there should be an instance identifier, which uniquely
 * identifies the context.  There should also be some marker, i.e., "|||", that
 * marks where the focus word is in the document.  Only one context vector will
 * be generated for each document.  This class depends on a {@link
 * ContextGenerator} for generating the context vectors.
 *
 * @author Keith Stevens
 */
public class SemEvalContextExtractor implements ContextExtractor {

    /**
     * The default separator used.
     */
    private static final String DEFAULT_SEPARATOR = "||||";

    /**
     * The {@link ContextGenerator} responsible for creating context vectors.
     */
    private final ContextGenerator generator;

    /**
     * The number of words before and after a focus word which compose the
     * context.
     */
    private final int windowSize;

    /**
     * The token used to separate the previous context from the focus word.
     */
    private final String separator;

    /**
     * Creates a new {@link SemEvalContextExtractor}.
     *
     * @param generator The {@link ContextGenerator} responsible for creating
     *        context vectors
     * @param windowSize the number of words before and after a focus word which
     *        compose the context.
     */
    public SemEvalContextExtractor(ContextGenerator generator,
                                   int windowSize) {
        this(generator, windowSize, DEFAULT_SEPARATOR);
    }

    /**
     * Creates a new {@link SemEvalContextExtractor}.
     *
     * @param generator The {@link ContextGenerator} responsible for creating
     *        context vectors
     * @param windowSize the number of words before and after a focus word which
     *        compose the context.
     */
    public SemEvalContextExtractor(ContextGenerator generator,
                                   int windowSize,
                                   String separator) {
        this.generator = generator;
        this.windowSize = windowSize;
        this.separator = separator;
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

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        // Skip empty documents.
        if (!it.hasNext())
            return;

        String instanceId = it.next();

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; it.hasNext(); ++i) {
            String term = it.next();
            if (term.equals(separator))
                break;
            prevWords.offer(term.intern());
        }

        // Eliminate the first set of words that we don't want to inspect.
        while (prevWords.size() > windowSize)
            prevWords.remove();

        // It's possible that the SenseEval/SemEval parser failed to find the
        // focus word.  For these cases, skip the context.
        if (!it.hasNext())
            return;

        String focusWord = it.next().intern();

        // Extract the set of words to consider after the focus word.
        while (it.hasNext() && nextWords.size() < windowSize)
            nextWords.offer(it.next().intern());

        // Create the context vector and have wordsi handle it.
        SparseDoubleVector contextVector = generator.generateContext(
                prevWords, nextWords);
        wordsi.handleContextVector(focusWord, instanceId, contextVector);
    }
}
