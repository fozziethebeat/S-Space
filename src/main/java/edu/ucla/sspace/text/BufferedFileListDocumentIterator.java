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
import java.io.IOError;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import edu.ucla.sspace.util.LineReader;


/**
 * An iterator implementation that returns {@link Document} instances given a
 * file that contains list of files, buffering their contents as necessary.
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class BufferedFileListDocumentIterator implements Iterator<Document> {

    /**
     * The default maximum number of elements to have queued
     */
    private static final int DEFAULT_BUFFER_SIZE = 100;

    /**
     * The files in the list that have yet to be returned as {@code Document}
     * instances
     */
    private final Queue<String> filesToProcess;

    private final BlockingQueue<Document> documentsToReturn;

    private final AtomicInteger remaining;

    private volatile RuntimeException bufferError;

    /**
     * Creates an {@code Iterator} over the files listed in the provided file
     * using the default buffer size.
     *
     * @code fileListName a file containing a list of file names with one per
     *       line
     *
     * @throws IOException if any error occurs when reading {@code fileListName}
     */
    public BufferedFileListDocumentIterator(String fileListName) 
            throws IOException {
        this(fileListName, DEFAULT_BUFFER_SIZE);
    }
    
    /**
     * Creates an {@code Iterator} over the files listed in the provided file.
     *
     * @code fileListName a file containing a list of file names with one per
     *       line
     *
     * @throws IOException if any error occurs when reading {@code fileListName}
     */
    public BufferedFileListDocumentIterator(String fileListName, int bufferSize)
            throws IOException {
	
	filesToProcess = new ArrayDeque<String>();
        documentsToReturn = new ArrayBlockingQueue<Document>(bufferSize);

	// read in all the files we have to process
        for (String line : new LineReader(new File(fileListName)))
	    filesToProcess.offer(line.trim());	    

        remaining = new AtomicInteger(filesToProcess.size());
        bufferError = null;

        Thread bufferingThread = new Thread(new Bufferer(), 
            "BufferingThread for " + fileListName);
        bufferingThread.setDaemon(true);
        bufferingThread.start();
    }

    /**
     * Returns {@code true} if there are more documents to return.
     */
    public boolean hasNext() {
	return remaining.get() > 0;
    }
    
    /**
     * Returns the next document from the list.
     */
    public Document next() {
        if (!hasNext())
            throw new NoSuchElementException("No further documents");
        if (bufferError != null)
            throw bufferError;
        try {
            return documentsToReturn.take();
        } catch (InterruptedException ie) {
            throw new IOError(ie);
        }
    }	
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
	throw new UnsupportedOperationException(
	    "removing documents is not supported");
    }

    class Bufferer implements Runnable {
        public void run() {
            while (!filesToProcess.isEmpty()) {
                try {
                    String file = filesToProcess.poll();
                    documentsToReturn.put(new FileDocument(file, true));
                    remaining.decrementAndGet();
                } catch (Exception e) {
                    bufferError = new RuntimeException(e);
                    throw new IOError(e);
                }
            }
        }
    }
}
