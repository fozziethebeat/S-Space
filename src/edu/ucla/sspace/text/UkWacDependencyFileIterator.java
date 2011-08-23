/*
 * Copyright 2010 Keith Stevens
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
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;


/**
 * An iterator implementation that returns {@link Document} containg a single
 * dependency parsed sentence given a file in the <a
 * href="http://nextens.uvt.nl/depparse-wiki/DataFormat">CoNLL Format</a>
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class UkWacDependencyFileIterator implements Iterator<Document> {

    /**
     * The reader for accessing the file containing the documents
     */
    private final BufferedReader documentsReader;
    
    /**
     * The next line in the file
     */
    private String nextLine;

    /**
     * Creates an {@code Iterator} over the file where each document returned
     * contains the sequence of dependency parsed words composing a sentence..
     *
     * @param documentsFile the file specifying a dependency parsed file in the
     * <a href="http://nextens.uvt.nl/depparse-wiki/DataFormat">CoNLL Format</a>
     *
     * @throws IOException if any error occurs when reading
     *                     {@code documentsFile}
     */
    public UkWacDependencyFileIterator(String documentsFile)
            throws IOException {
        documentsReader = new BufferedReader(new FileReader(documentsFile));
        nextLine = advance();
    }

    /**
     * Returns {@code true} if there are more documents to return.
     */
    public boolean hasNext() {
        return nextLine != null;
    }
    
    private String advance() throws IOException {
        StringBuilder sb = new StringBuilder();
        String line = null;

        // Read the <text line.  Or read the end of the file and finish the
        // iteration.
        if (documentsReader.readLine() == null)
            return null;

        while ((line = documentsReader.readLine()) != null
               && !line.equals("</text>")) {
            // Skip sentence delimitors.
            if (line.startsWith("<s>") || line.startsWith("</s>"))
                continue;

            // Append the token line.
            sb.append(line).append("\n");
       }

        return sb.toString();
    }

    /**
     * Returns the next document from the file.
     */
    public synchronized Document next() {
        Document next = new StringDocument(nextLine);
        try {
            nextLine = advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return next;
    }        
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException(
            "removing documents is not supported");
    }
}
