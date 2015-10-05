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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.ucla.sspace.corenlp.CoreNlpProcessedCorpus;


/**
 * Utility functions for operating on {@link Document} instances, in the style
 * of {@link java.util.Collections}.
 */
public class Documents {

    /**
     * Wraps a series of documents as a {@link Corpus} instance
     */
    public static Corpus asCorpus(Document... docs) {
        return new SimpleCorpus(Arrays.asList(docs));
    }

    /**
     * Wraps a series of documents as a {@link Corpus} instance
     */
    public static Corpus asCorpus(Iterable<Document> docs) {
        return new SimpleCorpus(docs);
    }

    /**
     * Returns an iterable stream over the tokens in a {@link Document}.
     */
    public static Iterable<String> toTokens(Document d) {
        // REMINDER: wrap this in a proper iterator, rather than dumping to a
        // secondard data structure.
        List<String> tokens = new ArrayList<String>();
        for (Sentence s : d) {
            for (Token t : s)
                tokens.add(t.text());
        }
        return tokens;
    }
    
    // public static Document of(List<String> tokens) {
    //     return new SimpleDocument(tokens);
    // }

    // public static Document from(String text) {
    //     Iterator<String> tokenIter = 
    //         IteratorFactory.tokenizeOrdered(document);
    //     List<String> tokens = new ArrayList<String>();
    //     while (tokenIter.hasNext())
    //         tokens.add(tokenIter.next());
    //     return of(tokens);
    // }

    // public static List<Document> parse(String text) {
        
    // }

}
