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

package edu.ucla.sspace.text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * An iterator that <a href="http://en.wikipedia.org/wiki/Stemming">stems</a>
 * all of the tokens that it returns.
 */
public class StemmingIterator implements Iterator<String> {
    
    /**
     * The internal iterator that will tokenize the data prior to stemming.
     */
    private final Iterator<String> tokenizer;

    /**
     * The stemmer to use in stemming the tokens.
     */
    private final Stemmer stemmer;

    /**
     * Constructs an iterator to stem all the tokens in the string.
     */
    public StemmingIterator(String str, Stemmer stemmer) {
        this(new WordIterator(str), stemmer);
    }

    /**
     * Constructs an iterator to stem all the tokens in the reader.
     */
    public StemmingIterator(BufferedReader br, Stemmer stemmer) {
        this(new WordIterator(br), stemmer);
    }

    /**
     * Constructors an iterator that stems all of the provided tokens
     */
    public StemmingIterator(Iterator<String> tokens, Stemmer stemmer) {
        this.tokenizer = tokens;
        this.stemmer = stemmer;
    }

    /**
     * Returns {@code true} if there is another word to return.
     */
    public boolean hasNext() {
        return tokenizer.hasNext();
    }

    /**
     * Returns the next word from the reader.
     */
    public String next() {
        return stemmer.stem(tokenizer.next());
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }
}
