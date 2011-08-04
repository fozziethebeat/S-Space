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

/**
 * A {@code Document} implementation backed by a {@code File} whose contents are
 * used for the document text.
 */
public class FileDocument implements Document {
	
    /**
     * The reader for the backing file.
     */
    private final BufferedReader reader;
    
    /**
     * Constructs a {@code Document} based on the contents of the provide file.
     *
     * @param fileName the name of a file whose contents will be used as a
     *        document
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public FileDocument(String fileName) throws IOException {
	BufferedReader r = null;
	try {
	    r = new BufferedReader(new FileReader(fileName));
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	reader = r;
    }

    /**
     * {@inheritDoc}
     */
    public BufferedReader reader() {
	return reader;
    }
    
}
