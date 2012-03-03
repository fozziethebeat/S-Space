/*
 * Copyright 2009 Keith Stevens
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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map;


/**
 * An iterator over all tokens in a stream that replaces tokens if they have a
 * known replacement value.  This simplifies term substitutions, such as mapping
 * the both "banana" and "cat" to "bananacat".
 */
public class WordReplacementIterator implements Iterator<String> {

    /**
     * The backing iterator that tokenizes the stream
     */
    private final Iterator<String> baseIterator;

    /**
     * A mapping from original tokens to their subsituted value.
     */
    private final Map<String, String> replacementMap;

    /**
     * The next token to return
     */
    private String next;

    public WordReplacementIterator(Iterator<String> base,
                                   Map<String, String> map) {
        baseIterator = base;
        replacementMap = map;
        next = null;
        advance();
    }

    /**
     * Advances to the next word in the token stream.
     */
    public void advance() {
        String s = null;
        if (baseIterator.hasNext())
            next = baseIterator.next();
        else
            next = null;
    }

    /**
     * Returns {@code true} if this iterator has additional tokens to return.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Return the next token's replacement if such a replacement exists.  If no
     * replacement for the next token exists, the original token is returned.
     *
     * @return The next token stored in this iterator, replaced with a
     *          subsituted value if one exists.
     */
    public String next() {
        if (next == null)
            throw new NoSuchElementException();
        String replacement = replacementMap.get(next);
        replacement = (replacement == null) ? next : replacement;
        advance();
        return replacement;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }
}
