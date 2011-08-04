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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A {@code TemporalDocument} implementation backed by a {@code File} whose
 * contents are used for the document text.
 */
public class TemporalFileDocument implements TemporalDocument {
	
    /**
     * The reader for the backing file.
     */
    private final BufferedReader reader;

    /**
     * The time at which this file was created
     */
    private final long timeStamp;

    /**
     * Constructs a {@code TemporalDocument} using the contents of the provided
     * file, using the {@link File#lastModified() lastModified} time as its
     * creation time.
     *
     * @param fileName the name of a file whose contents will be used as a
     *        document
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public TemporalFileDocument(String fileName) throws IOException {
	this(new File(fileName));
    }

    /**
     * Constructs a {@code TemporalDocument} using the contents of the provided
     * file, using the {@link File#lastModified() lastModified} time as its
     * creation time.
     *
     * @param file a file whose contents will be used as a document
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public TemporalFileDocument(File file) throws IOException {
	this(file, file.lastModified());
    }
    
    /**
     * Constructs a {@code TemporalDocument} using the contents of the provided
     * file, which was created at the specified time.
     *
     * @param fileName the name of a file whose contents will be used as a
     *        document
     * @param timeStamp the time at which this file was created
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public TemporalFileDocument(String fileName, long timeStamp) throws IOException {
	this(new File(fileName), timeStamp);
    }

    /**
     * Constructs a {@code TemporalDocument} using the contents of the provided
     * file, which was created at the specified time.
     *
     * @param file a file whose contents will be used as a document
     * @param timeStamp the time at which this file was created
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public TemporalFileDocument(File file, long timeStamp) throws IOException {
	BufferedReader r = null;
	try {
	    r = new BufferedReader(new FileReader(file));
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	reader = r;
	this.timeStamp = timeStamp;
    }

    /**
     * {@inheritDoc}
     */
    public BufferedReader reader() {
	return reader;
    }

    /**
     * {@inheritDoc}
     */
    public long timeStamp() {
	return timeStamp;
    }
    
}
