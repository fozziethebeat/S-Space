/*
 * Copyright 2009 Keith Stevens 
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

import java.io.File;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;


/**
 * An abstract base class for corpus reading iterators that need to traverse
 * through a directory structure to find files containing text.  Sub classes
 * will need to implement a method that will traverse a single file at a time
 * and return string forms of documents.
 */
public abstract class DirectoryCorpusReader implements Iterator<Document> {

    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.text.DirectoryCorpusReader";

    public static final String NO_PRE_PROCESS_PROPERTY =
        PROPERTY_PREFIX + ".nopreprocess";

    /**
     * The set of directories, represented as a queue of files, that need to be
     * evaluated.
     */
    private Stack<Queue<File>> filesToExplore;

    /**
     * The String representing the next document to return.
     */
    private String nextLine;

    /**
     * The document pre processor that will be used to remove any unwanted items
     * from documents.
     */
    private final DocumentPreprocessor processor;

    /**
     * Constructs a new {@link DirectoryCoprusReader} from an initial file
     * with the system properties. 
     */
    public DirectoryCorpusReader(String startingFile) {
        this(startingFile, System.getProperties());
    }

    /**
     * Constructs a new {@link DirectoryCoprusReader} from an initial file.  If
     * that file is a real file, it will be the only file read.  If the file
     * given is a directory, all the files and sub directories will be read by
     * this reader.
     */
    public DirectoryCorpusReader(String startingFile, Properties props) {
        filesToExplore = new Stack<Queue<File>>();

        LinkedList<File> files = new LinkedList<File>();
        filesToExplore.push(files);

        // If the given file is a directory, store the sub files as the first
        // files to explore.  Otherwise the given file name is the only file to
        // iterate over.
        File start = new File(startingFile);
        if (start.isDirectory()) {
            File[] subFiles = start.listFiles();
            Arrays.sort(subFiles);
            files.addAll(Arrays.asList(subFiles));
        } else
            files.add(start);
        Collections.sort(files);

        if (props.getProperty(NO_PRE_PROCESS_PROPERTY) == null)
            processor = new DocumentPreprocessor();
        else 
            processor = null;
    }

    /**
     * Sets up the iterator so that the first call to next returns a document.
     * THIS MUST BE CALLED IN CONSTRUCTORS FOR SUB CLASSES.
     */
    protected void init() {
        String currentDoc = findNextDoc();
        setupCurrentDoc(currentDoc);
        nextLine = advance();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean hasNext() {
        return nextLine != null;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Document next() {
        Document doc = new StringDocument(nextLine);
        nextLine = advance();
        return doc;
    }

    /**
     * Throws {@link UnsupportedOperationException} if called.
     */
    public synchronized void remove() {
        throw new UnsupportedOperationException("Remove not permitted.");
    }

    /**
     * Returns a new String representing a complete document extracted from
     * {@code currentDoc}.
     */
    protected abstract String advanceInDoc();

    /**
     * Sets up any data members needed to process the current file being
     * processed.
     */
    protected abstract void setupCurrentDoc(String currentDocName);

    /**
     * Returns a cleaned version of the document if document processing is
     * enabled
     *
     * @see DocumentProcessor#process(String)
     */
    protected String cleanDoc(String document) {
        return (processor != null) ? processor.process(document) : document;
    }

    /**
     * Returns the next String representing a complete document that is
     * accessible by this {@code DirectoryCorpusReader}.  If all files have been
     * traversed then this will return null.
     */
    protected String advance() {
        String newDoc = advanceInDoc();
        if (newDoc == null) {
            String currentDoc = findNextDoc();
            if (currentDoc == null)
                return null;
            setupCurrentDoc(currentDoc);
            newDoc = advanceInDoc();
        }
        return newDoc;
    }

    /**
     * Returns the next file name that can be processed to extract documents.
     * Files are found by recursively searching the directory structure.
     */
    private String findNextDoc() {
        if (filesToExplore.empty())
            return null;

        // Check that there are files in this current directory worth exploring.
        // If there are not any files, remove this directory from the list of
        // files to explore and return whatever can be found in the next set of
        // files.
        Queue<File> files = filesToExplore.peek();
        if (files.size() == 0) {
            filesToExplore.pop();
            return findNextDoc();
        }

        // Check the next file in the queue.  If it is a directory, recurse
        // into that directory and return whatever the contents of what is found
        // there.
        File f = files.remove();
        if (f.isDirectory()) {
            File[] subFiles = f.listFiles();
            Arrays.sort(subFiles);
            filesToExplore.push(new LinkedList<File>(Arrays.asList(subFiles)));
            return findNextDoc();
        }

        // Skip over hidden files.
        if (f.isHidden())
            return findNextDoc();

        // The file is a real file worth extracting documents from.
        return f.getPath();
    }
}
