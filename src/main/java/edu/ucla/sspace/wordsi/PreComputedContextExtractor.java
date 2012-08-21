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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;


/**
 * A {@link ContextExtractor} that assumes that the corpus has already been
 * pre-processed and each document is a single line with the following format:
 *
 * </p>
 * header score,feature(|score, feature)*
 * </p>
 *
 * where header is some id that identifies the word being represented by this
 * context, score is a double, and feature is some string.  With this style of
 * corpus, {@link PreComputedContextExtractor} will obtain a dimension for each
 * feature and transform the line into a {@link SparseDoubleVector}, passing it
 * to {@link Wordsi} for futher processing.
 *
 * @author Keith Stevens
 */
public class PreComputedContextExtractor implements ContextExtractor {

    /**
     * The {@link BasisMapping} responsible for determining feature indices.
     */
    private final BasisMapping<String, String> basis;

    /**
     * Constructs a new {@link PreComputedContextExtractor} using a {@link
     * StringBasisMapping}.
     */
    public PreComputedContextExtractor() {
        this(new StringBasisMapping());
    }

    /**
     * Constructs a new {@link PreComputedContextExtractor} using the given
     * {@link BasisMapping}.
     */
    public PreComputedContextExtractor(BasisMapping<String, String> basis) {
        this.basis = basis;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        String line;
        
        try {
            line = document.readLine();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        // Split the header from the rest of the context.  The header must be
        // the focus word for this context.
        String[] headerRest = line.split("\\s+", 2);
        
        // Reject any words not accepted by wordsi.
        if (!wordsi.acceptWord(headerRest[0]))
            return;

        SparseDoubleVector context = new CompactSparseVector();

        // Iterate through each feature and convert it to a dimension for the
        // context vector.
        for (String item : headerRest[1].split("\\|")) {
            String[] featureScore = item.split(",", 2);
            double score = Double.parseDouble(featureScore[0]);
            int dimension = basis.getDimension(featureScore[1]);
            if (dimension >= 0)
                context.set(dimension, score);
        }
        wordsi.handleContextVector(headerRest[0], headerRest[0], context);
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return basis.numDimensions();
    }
}
