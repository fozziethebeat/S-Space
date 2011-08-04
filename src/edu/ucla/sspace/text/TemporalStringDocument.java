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
import java.io.StringReader;

/**
 * A {@code TemporalDocument} implementation backed by a {@code String} whose
 * contents are used for the document text.
 */
public class TemporalStringDocument implements TemporalDocument {

    /**
     * A reader to the text of the document
     */
    private final BufferedReader reader;

    /**
     * The time at which this file was created
     */
    private final long timeStamp;

    /**
     * Constructs a {@code Document} using the provided string as the document
     * text
     *
     * @param docText the document text
     */
    public TemporalStringDocument(String docText, long timeStamp) {
	reader = new BufferedReader(new StringReader(docText));
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