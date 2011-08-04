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

package edu.ucla.sspace.common;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;


/**
 * {@code DocumentVectorBuilder} generates {@code Vector} representations of a
 * document, based on semantic {@code Vector}s provided for a {@code
 * SemanticSpace}.  This can be consider as a projecting the document into the
 * semantic space.
 *
 * </p>
 *
 * Documents will be tokenized using the current tokenizing
 * method, and the vector in the {@code SemanticSpace} corresponding to each
 * word found in the document will be combined together.  
 *
 * </p>
 * Options for combining term {@code Vector}s include summation, average, and
 * term frequency weighting.
 *
 * @author Keith Stevens
 */
public class DocumentVectorBuilder {

    /**
     * The base prefix for all properties.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.common.DocumentVectorBuilder";

    /**
     * The property to specify if term frequencies should be used when combining
     * term vectors.
     */
    public static final String USE_TERM_FREQUENCIES_PROPERTY =
        PROPERTY_PREFIX + ".usetf";

    /**
     * The {@code SemanticSpace} which will provide a {@code Vector} for each
     * word found in a document.
     */
    private final SemanticSpace sspace;

    private final boolean useTermFreq;

    /**
     * Creates a {@code DocumentVectorBuilder} from a {@code SemanticSpace} and
     * extracts options from the system wide {@code Properties}.
     */
    public DocumentVectorBuilder(SemanticSpace baseSpace) {
        this(baseSpace, System.getProperties());
    }

    /**
     * Creates a {@code DocumentVectorBuilder} from a {@code SemanticSpace} and
     * extracts options from the given {@code Properties}.
     */
    public DocumentVectorBuilder(SemanticSpace baseSpace, Properties props) {
        sspace = baseSpace;
        useTermFreq = props.getProperty(USE_TERM_FREQUENCIES_PROPERTY) != null;
    }

    /**
     * Represent a document as the summation of term Vectors.
     *
     * @param document A {@code BufferedReader} for a document to project into a
     *                 {@code SemanticSpace}.
     * @param documentVector A {@code Vector} which has been pre-allocated to
     *                       store the document's representation.  This is
     *                       pre-allocated so that users of {@code
     *                       DocumentVectorBuilder} can decide what type of
     *                       {@code Vector} should be used to represent a
     *                       document.
     *
     * @return {@code documentVector} after it has been modified to represent
     *         the terms in {@code document}.
     */
    public DoubleVector buildVector(BufferedReader document,
                              DoubleVector documentVector) {
        // Tokenize and determine what words exist in the document, along with
        // the requested meta information, such as a term frequency.
        Map<String, Integer> termCounts = new HashMap<String, Integer>();
        Iterator<String> articleTokens = IteratorFactory.tokenize(document);
        while (articleTokens.hasNext()) {
            String term = articleTokens.next();
            Integer count = termCounts.get(term);
            termCounts.put(term, (count == null || !useTermFreq)
                                 ? 1 : count.intValue() + 1);
        }

        // Iterate through each term in the document and sum the term Vectors 
        // found in the provided SemanticSpace.
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            Vector termVector = sspace.getVector(entry.getKey());
            if (termVector == null)
                continue;
            add(documentVector, termVector, entry.getValue());
        }

        return documentVector;
    }

    public void add(DoubleVector dest, Vector src, int factor) {
        if (src instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) src). getNonZeroIndices();
            for (int i : nonZeros)
                dest.add(i, src.getValue(i).doubleValue());
        } else {
            for (int i = 0; i < src.length(); ++i)
                dest.add(i, src.getValue(i).doubleValue());
        }
    }
}
