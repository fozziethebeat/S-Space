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
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An iterator over all the tokens in a stream that uses a {@link TokenFilter} to
 * remove invalid tokens.
 */
public class FilteredIterator implements Iterator<String> {

    /**
     * The backing iterator that tokenizes the stream
     */
    private final Iterator<String> tokenizer;

    /**
     * The filter to use in accepting or rejecting tokens
     */
    private final TokenFilter filter;

    /**
     * The next token to return
     */
    private String next;

    /**
     * Creates a filtered iterator using the string as a source of tokens
     */
    public FilteredIterator(String str, TokenFilter filter) {
	this(new WordIterator(str), filter);
    }

    /**
     * Creates a filtered iterator using the reader as a source of tokens
     */
    public FilteredIterator(BufferedReader reader, TokenFilter filter) {
	this(new WordIterator(reader), filter);
    }

    /**
     * Creates a filtered iterator using provided iterator as the source of
     * tokens
     */
    public FilteredIterator(Iterator<String> tokens, TokenFilter filter) {
	tokenizer = tokens;
	this.filter = filter;
	next = null;
	advance();
    }

    /**
     * Advances to the next word in the token stream.
     */
    private void advance() {
	String s = null;
	while (tokenizer.hasNext()) {
	    String nextToken = tokenizer.next();
	    // stop if the word should be returned
	    if (filter.accept(nextToken)) {
		s = nextToken;
		break;
	    }
	}
	next = s;
    }

    /**
     * Returns {@code true} if this iterator has additional tokens that would be
     * accepted by the filter
     */
    public boolean hasNext() {
	return next != null;
    }

    /**
     * Returns the next word from the reader that has passed the filter.
     */
    public String next() {
	if (next == null) {
	    throw new NoSuchElementException();
	}
	String s = next;
	advance();
	return s;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
	throw new UnsupportedOperationException("remove is not supported");
    }

}