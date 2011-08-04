/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.dependency;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * An interface for classes that read the output files of dependency parse
 * sentences and convert the data into a dependency parse tree.  This interface
 * allows uniform access to tree structure from different parsing output
 * formats, such as CoNLL or MINIPAR.
 *
 * @author Keith Stevens
 */
public interface DependencyExtractor {
       
    /**
     * Reads the next dependency-parse tree from the reader, returning an array
     * of all the nodes in the tree.  The relations in the tree are reflected in
     * the nodes.
     *
     * </p>
     *
     * This function may be called multiple times on the same reader if more
     * than one tree exists.
     *
     * @param reader a reader containing one or more dependency parse trees in
     *        a recognized format
     *
     * @return an array of {@link DependencyTreeNode}s that compose a dependency
     *         tree or {@code null} if no tree exists in the reader.
     *
     * @throws IOException when errors are encountered during reading
     */
    public DependencyTreeNode[] readNextTree(BufferedReader reader) 
        throws IOException;
}
