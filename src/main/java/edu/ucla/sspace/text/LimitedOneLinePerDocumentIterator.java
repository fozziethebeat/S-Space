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

import java.io.IOException;

import java.util.Iterator;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An iterator decorator that returns {@link Document} instances given a file
 * that contains list of files.  This iterator allows an upper limit on the
 * number of documents returned to be set.  Once this limit has been reached,
 * the iterator must be reset before it can return more documents.
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class LimitedOneLinePerDocumentIterator implements Iterator<Document> {
    
    /**
     * The base iterator to decorate.
     */
    private final Iterator<Document> iter;

    /**
     * An {@code AtomicInteger} counting the number of documents returned by
     * this iterator so far.
     */
    private final AtomicInteger docCount;

    /**
     * Set to true once this iterator has been reset for the first time.
     */
    private boolean isReset;

    /**
     * The maximum number of documents returned by this iterator before it needs
     * to be reset.
     */
    private final int docLimit;

    /**
     * If set to true, multiple resets will be allowed.
     */
    private final boolean useMultipleResets;

    /**
     * Constructs an {@code Iterator} for the documents contained in the
     * provided file.
     *
     * @param documentsFile a file that contains one document per line
     *
     * @throws IOException if any error occurs when reading {@code
     *         documentsFile}
     */
    public LimitedOneLinePerDocumentIterator(Iterator<Document> iter,
                                             int docLimit,
                                             boolean useMultipleResets)
            throws IOException {
        this.iter = iter;
        this.docLimit = docLimit;
        this.useMultipleResets = useMultipleResets;
        docCount = new AtomicInteger(); 
        isReset = false;
    }
    
    /**
     * Returns {@code true} if there are more documents in the provided file.
     */
    public synchronized boolean hasNext() { 
        return docCount.get() < docLimit && iter.hasNext();
    }

    /**
     * Returns the next document from the file.
     */
    public synchronized Document next() {
        if (!isReset || useMultipleResets)
            docCount.incrementAndGet();
        return iter.next();
    }

    /**
     * Reset the iterator so that new documents may be returned.
     */
    public synchronized void reset() {
        docCount.set(0);
        isReset = true;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException(
            "removing documents is not supported");
    }
}
