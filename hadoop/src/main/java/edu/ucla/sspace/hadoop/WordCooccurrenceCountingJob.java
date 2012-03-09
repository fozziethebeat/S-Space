/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.hadoop;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.logging.Logger;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;

/**
 * A hadoop utility that spawns a job to count all the co-occurrences for a
 * directory of files within the Hadoop File System.  This class serves as the
 * underlying co-occurrence counting logic for other semantic space algorithms
 * that use the oc-occurrences to build their final representations.
 *
 *
 *
 *
 * <p> This class is not thread safe.
 *
 * @author David Jurgens
 */
public class WordCooccurrenceCountingJob {

    private final HadoopJob job;
   
    /**
     * Creates a {@code WordCooccurrenceCountingJob} using the System properties
     * for configuring the parameters.
     */
    public WordCooccurrenceCountingJob() {
        this(System.getProperties());
    }

    /**
     * Creates a {@code WordCooccurrenceCountingJob} using the provided
     * properties for configuring the parameters.
     */
    public WordCooccurrenceCountingJob(Properties props) {
        job = new HadoopJob(
            RawTextCooccurrenceMapper.class, 
            Text.class,                     // mapper output key class
            TextIntWritable.class,          // mapper output value class
            CooccurrenceReducer.class, 
            WordCooccurrenceWritable.class, // output key class
            IntWritable.class,              // output value class
            props);
    }

    /**
     * Exceutes the word co-occurrence counting job on the corpus files in the
     * input directory using the current Hadoop instance, returning an iterator
     * over all the occurrences frequences found in the corpus.
     *
     * @param inputDirs a list of directories on the Hadoop distributed file
     *        system containing all the corpus files that will be processed
     *
     * @return an iterator over the unique {@link WordCooccurrence} counts found
     *         in the corpus.  Note that if two words co-occur the same distance
     *         apart multiple times, only one {@code WordCooccurrence} is
     *         returned, where the number of co-occurrences is reflected by the
     *         the {@link WordCooccurrence#getCount() getCount()} method.
     *
     * @throws Exception if Hadoop throws an {@code Exception} during its
     *         execution or if the resulting output files cannot be read.
     */
    public Iterator<WordCooccurrence> execute(Collection<String> inputDirs)
            throws Exception {

        HadoopJobResults results = job.run(inputDirs);
        return new WordCooccurrenceIterator(
            results.getFileSystem(), results.getResults().iterator());
    }

    /**
     * An iterator over the output files from the {@link CooccurrenceReducer}
     * that returns the set of {@link WordCooccurrence} instances extracted from
     * the corpus.
     */
    private static class WordCooccurrenceIterator 
            implements Iterator<WordCooccurrence> {
        
        /**
         * The files containing results that have not yet been returned
         */
        private final Iterator<Path> files;

        /**
         * The current file being processed by this iterator.  The actual file
         * processing is delegated to a special purpose iterator.
         */
        private FileIterator curFile;

        /**
         * The next word occurrence to return or {@code null} if there are no
         * further instances to return.
         */
        private WordCooccurrence next;

        /**
         * The file system currently being used by this Hadoop instance.
         */
        private FileSystem fileSystem;

        /**
         * Creates a {@code WordCooccurrenceIterator} that returns all the
         * occurrences contained in the provided files.
         *
         * @param fileSystem the file system used to access the paths in {@code
         *        files}
         * @param files the series of input files to be read by this iterator
         *        and returned as {@link WordCooccurrence} instances
         */
        public WordCooccurrenceIterator(FileSystem fileSystem, 
                                      Iterator<Path> files) throws IOException {
            this.fileSystem = fileSystem;
            this.files = files;
            advance();
        }

        private void advance() throws IOException {
            if (curFile != null && curFile.hasNext()) {
                next = curFile.next();
            }
            else if (files.hasNext()) {
                curFile = new FileIterator(
                    new BufferedReader(
                        new InputStreamReader(fileSystem.open(files.next()))));
                next = curFile.next();
            }
            else {
                next = null;
            }
        }

        /**
         * Returns true if the iterator has more occurrences
         */
        public boolean hasNext() {
            return next != null;
        }
        
        /**
         * Returns the next instance
         */
        public WordCooccurrence next() {
            if (next == null) {
                throw new NoSuchElementException("No further word occurrences");
            }
            WordCooccurrence n = next;
            try {
                advance();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            return n;
        }

        /**
         * Throws an {@link UnsupportedOperatonException} if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An iterator that transforms the file output of the reduce step into
     * {@link WordCooccurrence} instances.
     */
    private static class FileIterator 
            implements Iterator<WordCooccurrence> {
        
        /**
         * The reader containing the contents of the reducer output.
         */
        private BufferedReader br;
        
        /**
         * The next line from the reader or {@code null} if there were no
         * further lines to be read.
         */
        private String nextLine;

        /**
         * Creates a {@code FileIterator} over the word co-occurrence
         * information contaned within the reader.  The data is expected to be
         * formatted according to the {@link CooccurrenceReducer} output.
         */
        public FileIterator(BufferedReader br) throws IOException {
            this.br = br;
            nextLine = br.readLine();
        }
        
        /**
         * Returns {@code true} if there are still word co-occurrences left to
         * return
         */ 
        public boolean hasNext() {
            return nextLine != null;
        }

        /**
         * Returns the next word co-occurrence from the file
         */
        public WordCooccurrence next() {
            if (nextLine == null) {
                throw new NoSuchElementException("No further word occurrences");
            }
            String n = nextLine;
            try {
                nextLine = br.readLine();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            String[] arr = n.split("\t");
            return new SimpleWordCooccurrence(arr[0], arr[1],
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
        }

        /**
         * Throws an {@link UnsupportedOperatonException} if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}