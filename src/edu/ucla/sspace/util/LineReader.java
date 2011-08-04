/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;


/** 
 * A utility class for reading the lines of a file.  This class is specifically
 * designed to support replacing the code
 *<pre>
 *try {
 *    BufferedReader br = new BufferedReader(new FileReader(file));
 *    for (String line = null; (line = br.readLine()) != null; ) {
 *        // work
 *    }
 *} catch (IOException ioe) {
 *    // handling code
 *} finally {
 *    br.close();
 *}
 *</pre>
 *
 * with the code
 *
 *<pre>
 *for (String line : new LineReader(file) {
 *    // work
 *}
 *</pre>
 *
 * This class with automatically close the stream up on finishing or upon error.
 * All {@link IOException} instances are rethrown as {@link IOError}.
 *
 * @author David Jurgens
 */
public class LineReader implements Iterable<String> {

    /**
     * The backing file
     */
    private final File f;

    /**
     * Creates a line reader for the provided file.
     */
    public LineReader(File f) {
        this.f = f;
    }
    
    /**
     * Returns an iterator over the lines in the file.
     */
    public Iterator<String> iterator() {
        return new LineIterator();
    }

    /**
     * The backing iterator class that does the actual line reading from the
     * file.
     */
    private class LineIterator implements Iterator<String> {
        
        /**
         * The reader for the file.
         */
        private final BufferedReader br;

        /**
         * The next line to return.
         */
        private String next;

        public LineIterator() {
            try {
                br = new BufferedReader(new FileReader(f));
                advance();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        private void advance() {
            try {
                next = br.readLine();
                // Close the reader if no further lines exist.
                if (next == null)
                    br.close();
            }
            catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public String next() {
            if (next == null)
                throw new NoSuchElementException();
            String n = next;
            advance();
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException(
                "Cannot remove line from file");
        }
    }
}