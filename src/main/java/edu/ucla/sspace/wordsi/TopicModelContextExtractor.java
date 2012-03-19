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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;


/**
 * A {@link ContextExtractor} for processing documents with topic signatures for
 * contexts as computed by the Mallet framework.  Each document should be
 * preceeded with the very first token representing the focus word represented
 * by the context.
 *
 * @author Keith Stevens
 */
public class TopicModelContextExtractor implements ContextExtractor {

    /**
     * The vector length of the topic signatures.  This is computed on the fly.
     */
    private int vectorLength;

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        try {
            // Split the line into the focus word and each feature value.
            // Feature are recorded with the feature index and the feature
            // value, all separated by spaces.
            String termAndVector;
            if ((termAndVector = document.readLine()) == null)
                return;
            String[] tokens = termAndVector.split("\\s+");
            String[] termSplit = tokens[0].split("\\.");

            // Reject topic signatures that are too short.
            if (tokens.length < 10)
                return;

            // Compute the vector length and create the context vector.
            vectorLength = (tokens.length - 1) / 2;
            SparseDoubleVector vector = new CompactSparseVector(
                  (tokens.length - 1) / 2);

            // Read each feature index and value.
            for (int i = 1; i < tokens.length; i+=2) {
                int index = Integer.parseInt(tokens[i]);
                double value = Double.parseDouble(tokens[i+1]);
                vector.set(index, value);
            }

            wordsi.handleContextVector(termSplit[0], tokens[0], vector);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }
}
