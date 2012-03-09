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

import java.util.Collection;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


/**
 * A class that encapsulates the resulting output files of a Hadoop job and
 * provides file system access to those files using the {@link FileSystem} on
 * which they are stored.
 *
 * @see HadoopJobRunner
 */
public class HadoopJobResults {

    /**
     * The file system on which the results are stored
     */
    private final FileSystem fs;

    /**
     * Paths to each file result produced by the job
     */
    private final Collection<Path> results;

    /**
     * Creates a new job result containing all the specified paths that can be
     * accessed by the provided file system
     */
    public HadoopJobResults(FileSystem fs, Collection<Path> results) {
        this.fs = fs;
        this.results = results;
    }

    /**
     * Returns the file system that can be used to access each of the job's
     * result output files.
     */
    public FileSystem getFileSystem() {
        return fs;
    }

    /**
     * Returns all the file results produced by the job.
     */
    public Collection<Path> getResults() {
        return results;
    }    
}