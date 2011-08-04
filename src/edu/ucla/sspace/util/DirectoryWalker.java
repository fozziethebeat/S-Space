/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.util;

import java.io.File;
import java.io.FileFilter;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * A utility class that allows for easy iteration over all the files within a
 * directory and its subdirectories (but not returning the directories
 * themselves).  This class also provides the ability to limit the returned
 * files on the basis of a suffix, or a custom file file filter.  This class
 * enables code such as:
 * 
 * <pre>
 * for (File f : new DirectoryWalker(dirName)) { 
 *     // process f
 * }
 * </pre>
 */
public class DirectoryWalker implements Iterable<File> {

    /**
     * The base directory from which files will be returned
     */
    private final File baseDir;
    
    /**
     * The filter used to remove files from the iteration stream.
     */
    private final FileFilter filter;

    /**
     * Creates a {@code DirectoryWalker} that will return all files recursively
     * accessible from the directory
     *
     * @throws IllegalArgumentException if {@code baseDir} is not a directory
     */
    public DirectoryWalker(File baseDir) {
        this(baseDir, "");
    }
    
    /**
     * Creates a {@code DirectoryWalker} that will only return files whose names
     * end in the specified suffix
     *
     * @throws IllegalArgumentException if {@code baseDir} is not a directory
     */
    public DirectoryWalker(File baseDir, final String suffix) {
        this(baseDir, new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith(suffix);
                }
            });
    }

    /**
     * Creates a {@code DirectoryWalker} that will only return files that are
     * acceptable by the filter.
     *
     * @param filter the filter to use in deciding whether a file should be
     *        returned.  This filter should always return {@code true} for
     *        directories
     *
     * @throws IllegalArgumentException if {@code baseDir} is not a directory
     */
    public DirectoryWalker(File baseDir, FileFilter filter) {
        this.baseDir = baseDir;
        this.filter = filter;
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException(baseDir + " is not a directory");
        }
        if (filter == null)
            throw new NullPointerException();
    }

    /**
     * Returns an iterator over all the files recursively accessible from this
     *  {@code FileWalker}'s base directory
     */       
    public Iterator<File> iterator() {
        return new FileIterator();
    }

    /**
     * The iterator class that actually does the file walking.
     */
    private class FileIterator implements Iterator<File> {

        private final Queue<File> files;
        
        private File next;       

        public FileIterator() {
            files = new ArrayDeque<File>();
            for (File f : baseDir.listFiles(filter)) {
                files.offer(f);
            }
            advance();            
        }

        private void advance() {
            next = null;
            while (next == null && !files.isEmpty()) {
                File f = files.poll();
                // Note that we use list() without the filter-based overload to
                // ensure that the iteration picks up directories.
                if (f.isDirectory())
                    files.addAll(Arrays.asList(f.listFiles(filter)));
                else if (filter.accept(f))
                    next = f;
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public File next() {
            if (next == null)
                throw new NoSuchElementException();
            File f = next;
            advance();
            return f;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}