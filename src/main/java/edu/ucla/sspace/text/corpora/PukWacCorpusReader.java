/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.text.corpora;

import edu.ucla.sspace.text.CorpusReader;
import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.StringDocument;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.Reader;

import java.util.Iterator;


/**
 * Reads full documents from a parsed UkWac or Wackypedia corpus.  The corpus is
 * expected to be in full XML, with text tags delmiting documents and s tags
 * delmiting sentences.  All sentences for a document will be returned in the
 * same document.  Any other additional information in the corpus is discarded.
 *
 * @author Keith Stevens
 */
public class PukWacCorpusReader implements CorpusReader<Document> {

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> read(File file) {
        try {
            return read(new FileReader(file));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Document> read(Reader baseReader) {
        return new UkWacIterator(new BufferedReader(baseReader));
    }

    public class UkWacIterator implements Iterator<Document> {

        /**
         * The {@link BufferedReader} for reading lines in the corpus.
         */
        protected BufferedReader reader;

        /**
         * The text of the next document to return.
         */
        private String next;

        public UkWacIterator(BufferedReader reader) {
            this.reader = reader;
            this.next = advance();
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return next != null;
        }

        /**
         * {@inheritDoc}
         */
        public Document next() {
            Document doc = new StringDocument(next);
            next = advance();
            return doc;
        }

        /**
         * Unsupported.
         *
         * @throws UnsupportedOperationException When called.
         */
        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove documents from a CorpusReader");
        }

        /**
         * Advances the {@link CorpusReader} one {@link Document} a head in the
         * text.  Returns {@code null} when no more documents are found in the
         * file.
         */
        protected String advance() {
            StringBuilder sb = new StringBuilder();
            try {
                for (String line = null; (line = reader.readLine()) != null; ) {
                    // Skip the tags that denote when the document starts, and
                    // the sentence delimiters.  Also skip any empty lines.
                    if (line.startsWith("<text") ||
                        line.startsWith("<s>") ||
                        line.startsWith("</s>") ||
                        line.length() == 0)
                        continue;
                    // Break the loop when we read the deliminter for the
                    // document.
                    if (line.startsWith("</text>"))
                    break;

                    // Split up the line into it's tokens.  Append the first
                    // token, which is the real word of interest, to the builder
                    // and ignore the rest of the line.  The lines will contain
                    // any needed punctuation, so we can just include spaces
                    // between tokens to simplify tokenization.
                    String[] tokens = line.split("\\s+");
                    sb.append(tokens[0]).append(" ");
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }

            // Return null if nothing was read, indicating that the above loop
            // reached the end of it's input.
            if (sb.length() == 0)
                return null;

            return sb.toString();
        }
    }
}
