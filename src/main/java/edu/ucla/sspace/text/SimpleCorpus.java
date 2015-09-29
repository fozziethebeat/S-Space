/*
 * Copyright 2015 David Jurgens
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import edu.stanford.nlp.util.ArrayCoreMap;
import edu.stanford.nlp.util.CoreMap;


/**
 * A basic implementation of {@link Corpus} that wraps a collection of documents
 * in a single instance.
 */
public class SimpleCorpus implements Corpus {

    private final Iterable<Document> docs;

    private final CoreMap annotations;

    private TokenProcesser processer;

    public SimpleCorpus(Document... docs) {
        this(Arrays.asList(docs));
    }
    
    public SimpleCorpus(Iterable<Document> docs) {
        this.docs = docs;
        this.annotations = new ArrayCoreMap();
        this.processer = new PassThroughTokenProcesser();
    }

    /**
     * {@inheritDoc}
     */
    public CoreMap annotations() {
        return annotations;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> iterator() {
        return docs.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public TokenProcesser getTokenProcesser() {
        return processer;
    }

    /**
     * {@inheritDoc}
     */
    public void setTokenProcesser(TokenProcesser processer) {
        this.processer = processer;
    }
    
}
