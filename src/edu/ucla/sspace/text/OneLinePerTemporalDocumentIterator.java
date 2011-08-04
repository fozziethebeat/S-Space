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
import java.io.FileReader;
import java.io.IOException;

import java.util.Iterator;

/**
 * An iterator implementation that returns {@link TemporalDocument} instances
 * given a file where each line of text is treated as a separate document.
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class OneLinePerTemporalDocumentIterator 
        implements Iterator<TemporalDocument> {
    
    /**
     * The reader for accessing the file containing the documents
     */
    private final BufferedReader documentsReader;
    
    /**
     * The next line in the file
     */
    private String nextLine;
    
    /**
     * Constructs an {@code Iterator} for the documents contained in the
     * provided file.
     *
     * @param documentsFile a file that contains one document per line
     *
     * @throws IOException if any error occurs when reading {@code
     *         documentsFile}
     */
    public OneLinePerTemporalDocumentIterator(String documentsFile) 
	    throws IOException {
	
	documentsReader = new BufferedReader(new FileReader(documentsFile));
	nextLine = documentsReader.readLine();
    }
    
    /**
     * Returns {@code true} if there are more documents in the provided file.
     */
    public synchronized boolean hasNext() { 
	return nextLine != null;
    }    

    /**
     * Returns the next document from the file.
     */
    public synchronized TemporalDocument next() {
	
	int firstSpace = nextLine.indexOf(" ");
	String timeStr = nextLine.substring(0, firstSpace);
	String doc = nextLine.substring(firstSpace);
	TemporalDocument next = 
	    new TemporalStringDocument(doc, Long.parseLong(timeStr));
	try {
	    nextLine = documentsReader.readLine();
	} catch (Throwable t) {
	    t.printStackTrace();
	    nextLine = null;
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
