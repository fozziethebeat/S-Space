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

package edu.ucla.sspace.util;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * An interface for reader file-based resources regardless of the environment in
 * which the system is operating, e.g. a Hadoop environment.
 */
public interface ResourceFinder {

    /**
     * Finds the file with the specified name and returns a reader for that
     * files contents.
     *
     * @param fileName the name of a file
     *
     * @return a {@code BufferedReader} to the contents of the specified file
     *
     * @throws IOException if the resource cannot be found or if an error occurs
     *         while opening the resource
     */
    BufferedReader open(String fileName) throws IOException;
    
}