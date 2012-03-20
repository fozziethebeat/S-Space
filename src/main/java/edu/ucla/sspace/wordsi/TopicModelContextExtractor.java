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

import edu.ucla.sspace.text.Document;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.util.Iterator;


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
    public void processDocument(Document document, Wordsi wordsi) {
            // Split the line into the focus word and each feature value.
            // Feature are recorded with the feature index and the feature
            // value, all separated by spaces.
            String termAndVector;
            String header = document.title();

            SparseDoubleVector vector = new CompactSparseVector();

            Iterator<String> elements = document.iterator();
            String secondaryKey = elements.next();
            while (elements.hasNext()) {
                int index = Integer.parseInt(elements.next());
                if (!elements.hasNext())
                    return;
                double value = Double.parseDouble(elements.next());
                vector.set(index, value);
                if (index > vectorLength)
                    vectorLength = index;
            }

            wordsi.handleContextVector(header, secondaryKey, vector);
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return vectorLength;
    }
}
