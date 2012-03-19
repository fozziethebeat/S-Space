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
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Arrays;
import java.util.Iterator;


/**
 * An iterator implementation that returns {@link Document} instances labled
 * with the source URL from which its text was obtained, as specified in the
 * ukWaC.  See the <a
 * href="http://wacky.sslmit.unibo.it/doku.php?id=corpora">WaCky</a> group's
 * website for more information on the ukWaC.
 */
public class UkWaCDocumentIterator implements Iterator<Document> {

    /**
     * The reader for accessing the file containing the documents
     */
    private final BufferedReader lineReader;
    
    /**
     * The next document in the file
     */
    private Document nextDoc;

    /**
     * Creates an {@code Iterator} over the file where each document returned
     * is labeled by the source from which it was extracted
     *
     * @param documentsFile the UkWaC file 
     *
     * @throws IOException if any error occurs when reading
     *                     {@code documentsFile}
     */
    public UkWaCDocumentIterator(File documentsFile) throws IOException {
        lineReader = new BufferedReader(new FileReader(documentsFile));
        nextDoc = null;
        advance();
    }

    /**
     * Creates an {@code Iterator} over the file where each document returned
     * is labeled by the source from which it was extracted
     *
     * @param documentsFile the name of the UkWaC file 
     *
     * @throws IOException if any error occurs when reading
     *                     {@code documentsFile}
     */
    public UkWaCDocumentIterator(String documentsFile) throws IOException {
        this(new File(documentsFile));
    }
    
    /**
     * Returns {@code true} if there are more documents to return.
     */
    public boolean hasNext() {
        return nextDoc != null;
    }
    
    private void advance() throws IOException {
        nextDoc = null;
        String header = lineReader.readLine();
        if (header == null)
            lineReader.close();
        else {
            String doc = lineReader.readLine();
            assert doc != null;
            nextDoc = new TokenizedDocument(
                    Arrays.asList(doc.split("\\s+")), header);
        }
    }
    
    /**
     * Returns the next document from the file.
     */
    public Document next() {
        Document next = nextDoc;
        try {
            advance();
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
