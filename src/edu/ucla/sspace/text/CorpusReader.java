/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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
import java.io.Reader;
import java.util.Iterator;


/**
 * A basic interface for setting up a {@link CorpusReader}, which reads un
 * cleaned text from corpus files and transforms them into an appropriately
 * cleaned {@link Document} instance.
 *
 * @author Keith Stevens
 */
public interface CorpusReader<D extends Document> {

    /**
     * Returns a {@link Iterator} that traverses the documents containted in 
     * the given {@code file}.
     *
     * @param file A text file holding documents in a format
     *        that is readable by a particular {@link CorpusReader}.  This text
     *        file may have it's own unique text structure or an xml format.
     *        Each {@link CorpusReader} should specify the expected text format.
     */
    Iterator<D> read(File file);

    /**
     * Retrusn a {@link Iterator} that traverses the documents contained in
     * {@code baseReader}.
     *
     * @param baseReader A {@link Reader} that will extract text from a data
     *        source, such as a URL, a File, a data stream, or any other source
     *        accesible via the {@link Reader} interface.  Each {@link
     *        CorpusReader} should specify the expected text format, be it an
     *        XML schema or some other unique format.
     */
    Iterator<D> read(Reader baseReader);
}
