/*
 * Copyright 2014 David Jurgens
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

package edu.ucla.sspace.text;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;

import edu.ucla.sspace.util.CombinedIterator;


/**
 * A basic implementation of {@link Document} that wraps a collection of {@link
 * Sentence} instances.
 */
public class SimpleDocument implements Document {

    private final Iterable<Sentence> sentences;

    private final CoreMap annotations;
    
    public SimpleDocument(Iterable<Sentence> sentences) {
        this.sentences = sentences;
        this.annotations = new ArrayCoreMap();
    }

    /**
     * {@inheritDoc}
     */
    public CoreMap annotations() {
        return annotations;
    }

    /**
     * Returns an iterator over all the tokens in all the sentences in this
     * document.
     */
    public Iterator<Token> tokens() {
        Queue<Iterator<Token>> iters = new ArrayDeque<Iterator<Token>>();
        for (Sentence s : sentences)
            iters.add(s.iterator());
        return new CombinedIterator(iters);
    }

    /**
     * Returns an iterator over all the tokens in this document's text.
     */
    public Iterator<Sentence> iterator() {
        return sentences.iterator();
    }  
    
}
