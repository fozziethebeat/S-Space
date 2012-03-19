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

import edu.ucla.sspace.util.DirectoryWalker;

import java.io.File;
import java.io.Reader;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Stack;


/**
 * An abstract base class for corpus reading iterators that need to traverse
 * through a large nested directory structure to find files containing text.
 * Sub classes will need to implement a method that will traverse a single file
 * at a time and return strings holding the contents of a single document..
 *
 * </p>
 *
 * Note that {@link initialize(String)} uses the argument as a directory name,
 * and not the name of a text file to be processed.  This {@link CorpusReader}
 * instead uses that directory name as the root directory of the nested
 * directory structure.  This class also does not implement {@link
 * initialize(Reader)}.
 */
public abstract class DirectoryCorpusReader<D extends Document>
        implements CorpusReader<D> {

    /**
     * The document pre processor that will be used to remove any unwanted items
     * from documents.
     */
    private final DocumentPreprocessor processor;

    /**
     * Constructs a new {@link DirectoryCoprusReader} that uses no {@link
     * DocumentPreprocessor}.
     */
    public DirectoryCorpusReader() {
        this(null);
    }

    /**
     * Constructs a new {@link DirectoryCoprusReader} that uses {@link
     * processor} to pre-process any raw text extracted from a corpus file.
     */
    public DirectoryCorpusReader(DocumentPreprocessor processor) {
        this.processor = processor;
    }

    /**
     * Initializes the {@link DirectoryCorpusReader} to start processing all
     * files accessbile under the directory specified by {@code dirName}.  
     *
     * @param dirName A directory path containing a large directory structure
     *        that contains numerous text files that can be processed by a
     *        subclass of {@link DirectoryCoprusReader}.
     */
    public Iterator<D> read(File dir) {
        return corpusIterator((new DirectoryWalker(dir)).iterator());
    }

    /**
     * Unsupported.
     */
    public Iterator<D> read(Reader reader) {
        throw new UnsupportedOperationException(
                "The DirectoryCorpusReader cannot convert a reader to a " +
                "directory structur.");
    }

    /**
     * Returns an {@link Iterator} over documents contained in the {@link File}s
     * traversed by {@code fileIter}.  Sub-classes are encouraged to sub-class
     * BaseFileIterator for the return value of this method.
     */
    protected abstract Iterator<D> corpusIterator(Iterator<File> fileIter);

    /**
     * Unsupported.
     *
     * @throws UnsupportedOperationException when called
     */
    public void initialize(Reader baseReader) {
        throw new UnsupportedOperationException(
                "Cannot form a DirectoryCorpusReader from a Reader instance");
    }

    public abstract class BaseFileIterator implements Iterator<D> {

        /**
         * The list of files to explore based on some initial file.
         */
        private Iterator<File> filesToExplore;

        /**
         * The String representing the next document to return.
         */
        private D nextDoc;

        /**
         * Creates a new {@link BaseFileIterator}.
         */
        public BaseFileIterator(Iterator<File> filesToExplore) {
            this.filesToExplore = filesToExplore;
            this.nextDoc = null;
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return nextDoc != null;
        }

        /**
         * {@inheritDoc}
         */
        public D next() {
            D doc = nextDoc;
            nextDoc = advance();
            return doc;
        }

        /**
         * Throws {@link UnsupportedOperationException} if called.
         */
        public void remove() {
            throw new UnsupportedOperationException("Remove not permitted.");
        }

        /**
         * Returns a new String representing a complete document extracted from
         * {@code currentDoc}.
         */
        protected abstract D advanceInDoc();

        /**
         * Sets up any data members needed to process the current file being
         * processed.
         */
        protected abstract void setupCurrentDoc(File currentDoc);

        /**
         * Returns a cleaned version of the document if document processing is
         * enabled, otherwise the document text is returned unmodified.
         *
         * @see DocumentPreprocessor#process(String)
         */
        protected String cleanDoc(String document) {
            return (processor != null) ? processor.process(document) : document;
        }

        /**
         * Returns the next String representing a complete document that is
         * accessible by this {@code DirectoryCorpusReader}.  If all files have
         * been traversed then this will return null.
         */
        protected D advance() {
            D newDoc = advanceInDoc();
            if (newDoc == null) {
                // If there are no more files to explore, just return null to
                // indicate an empty reader.
                if (!filesToExplore.hasNext())
                    return null;

                // Otherwise setup the reader with the next document File.
                setupCurrentDoc(filesToExplore.next());

                // Now that the File is setup, recursively try to return the
                // next document.
                return advance();
            }
            return newDoc;
        }
    }
 }
