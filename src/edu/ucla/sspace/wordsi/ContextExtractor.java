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

package edu.ucla.sspace.wordsi;

import java.io.BufferedReader;


/**
 * An interface for extracting context vectors from a document and passing on
 * the vector to a {@link Wordsi} implementation.  Implementations are
 * recomended to use either a {@link ContextGenerator} or a {@link BasisMapping}
 * that is serializable.  Use of a {@link ContextGenerator} or a {@link
 * BasisMapping} separates the feature space from the text traveral, allowing
 * the feature space to be reused, even if a different text traversal method
 * needs to be used.
 *
 * @author Keith Stevens
 */
public interface ContextExtractor {

    /**
     * Processes the content of {@code document} and calls {@link
     * Wordsi#handleContextVector} for each context vector that can be extracted
     * from {@code document}.
     */
    void processDocument(BufferedReader document, Wordsi wordsi);

    /**
     * Returns the maximum number of dimensions used to represent any given
     * context.
     */
    int getVectorLength();
}
