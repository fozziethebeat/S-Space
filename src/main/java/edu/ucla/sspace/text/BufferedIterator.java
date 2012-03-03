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
import java.io.StringReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over all the tokens in a stream, which supports arbitrary
 * look-ahead into the stream by buffering the tokens.
 *
 * @author David Jurgens
 */
public class BufferedIterator implements Iterator<String> {

    /**
     * The backing iterator that will tokenize the stream
     */
    private final Iterator<String> tokenizer;

    /**
     * A buffer of tokens to return.  This is intentionally a {@code List} and
     * not a {@code Queue} so that we can take advantage of the {@link
     * List#subList(int,int)} functionality for {@link #peek(int)}.
     */
    private final List<String> buffer;

    /**
     * Tokenizes the string and buffers to the tokens to allow arbitrary
     * look-ahead.
     */
    public BufferedIterator(String str) {
	this(new BufferedReader(new StringReader(str)));
    }

    /**
     * Tokenizes the string contents on the reader and buffers to the tokens to
     * allow arbitrary look-ahead.
     */
    public BufferedIterator(BufferedReader br) {
	this(new WordIterator(br));
    }

    /**
     * Buffers the tokens in the provided iterator to allow arbitrarily
     * look-ahead.  The state of the provided iterator is updated by calls to
     * this iterator.
     */
    public BufferedIterator(Iterator<String> tokens) {
        this.tokenizer = tokens;
	buffer = new LinkedList<String>();
    }
    
    /**
     * Advances the specified number of tokens in the stream and places them in
     * the buffer.
     *
     * @param tokens the number of tokens to advance
     *
     * @return {@true} if the stream contained at least that many tokens, {@code
     *         false} if the stream contained fewer
     */
    private boolean advance(int tokens) {
	while (buffer.size() < tokens && tokenizer.hasNext())
	    buffer.add(tokenizer.next());
	return buffer.size() >= tokens;
    }

    /**
     * Returns {@code true} if the stream contains another token to return.
     */
    public boolean hasNext() {
	return buffer.size() > 0 || advance(1);
    }

    /**
     * Returns the next token in the stream.
     */
    public String next() {
	if (!hasNext()) {
	    throw new NoSuchElementException();
	}
	return buffer.remove(0);
    }

    /**
     * Returns an list of the next tokens in the stream based on the requested
     * amount.  If the stream did not contain as many tokens as was requested,
     * list will contain the remaining tokens, but will not throw an {@code
     * NoSuchElementException}.
     *
     * @param tokens the number of tokens to access in the stream
     * 
     * @return a list of the requested tokens or the remaining tokens in the
     *         stream, whichever is fewer
     */
    public List<String> peek(int tokens) {
	advance(tokens);
	return new ArrayList<String>(
	    buffer.subList(0, Math.min(tokens, buffer.size())));
    }
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
	throw new UnsupportedOperationException("remove is not supported");
    }
}