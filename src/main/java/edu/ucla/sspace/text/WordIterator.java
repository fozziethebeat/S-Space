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

import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * An iterator over all of the tokens present in a {@link BufferedReader} that
 * are separated by any amount of white space.
 */
public class WordIterator implements Iterator<String> {

    /**
     * A fixed pattern matching all non-whitespace characters.
     */
    private static final Pattern notWhiteSpace = Pattern.compile("\\S+");

    /**
     * The stream from which to read tokens
     */
    private final BufferedReader br;
    
    /**
     * The next token to return
     */
    private String next;

    /**
     * The matcher that is tokenizing the current line
     */
    private Matcher matcher;
    
    /**
     * The current line being considered
     */
    private String curLine;

    /**
     * Constructs an iterator for all the tokens contained in the string
     */
    public WordIterator(String str) {
        this(new BufferedReader(new StringReader(str)));
    }

    /**
     * Constructs an iterator for all the tokens contained in text of the
     * provided reader.
     */
    public WordIterator(BufferedReader br) {
        this.br = br;
        curLine = null;
        advance();
    }

    /**
     * Advances to the next word in the buffer.
     */
    private void advance() {
        try {
            // loop until we find a word in the reader, or there are no more
            // words
            while (true) {
                // if we haven't looked at any lines yet, or if the index into
                // the current line is already at the end 
                if (curLine == null || !matcher.find()) {

                    String line = br.readLine();
                    
                    // if there aren't any more lines in the reader, then mark
                    // next as null to indicate that there are no more words
                    if (line == null) {
                        next = null;
                        br.close();
                        return;
                    }
                    
                    // create a new matcher to find all the tokens in this line
                    matcher = notWhiteSpace.matcher(line);
                    curLine = line;
                    
                    // skip lines with no matches
                    if (!matcher.find())
                        continue;                    
                }

                next = curLine.substring(matcher.start(), matcher.end());
                break;
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns {@code true} if there is another word to return.
     */
    public boolean hasNext() {
        return next != null;
    }

    /**
     * Returns the next word from the reader.
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
