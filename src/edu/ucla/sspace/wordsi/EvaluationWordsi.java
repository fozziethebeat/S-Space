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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * An {@link Wordsi} implementation to be used for evaluations.  The word senses
 * are not updated during processing, instead, each generated context vector is
 * compared to the existing word senses and context vector is labeled with the
 * id for the most similar sense.  The sense labeling is passed on directly to
 * the {@link AssignmentReporter} for each context vector generated.
 *
 * </p>
 *
 * Word sense must be provided by a {@link SemanticSpace}.  For any polysemous
 * words, the first sense must be keyed by the raw word and all other sense must
 * be keyed by the raw word plus "-senseNumber" where senseNumber is an integer
 * starting at 1, for the second sense, and goes up to N-1, for the last sense.
 *
 * @author Keith Stevens
 */
public class EvaluationWordsi extends BaseWordsi {

    /**
     * The underlying {@link SemanticSpace} that provides word senses.
     */
    private final SemanticSpace wordSpace;

    /**
     * The {@link AssignmentReporter} used to report sense labels for generated
     * contexts.
     */
    private final AssignmentReporter reporter;

    /**
     * Creates a new {@link EvaluationWordsi}.
     *
     * @param acceptedWords The set of accepted words.  Only these words will
     *        have context vectors generated.
     * @param extractor The {@link ContextExtractor} responsible for generating
     *        context vectors.
     * @param sspace The {@link SemanticSpace} responsible for provided existing
     *        word senses.
     * @param reporter The {@link AssignmentReporter} reponsible for reporting
     *        sense labelings.
     */
    public EvaluationWordsi(Set<String> acceptedWords,
                            ContextExtractor extractor,
                            SemanticSpace sspace,
                            AssignmentReporter reporter) {
        super(acceptedWords, extractor);
        this.wordSpace = sspace;
        this.reporter = reporter;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return new HashSet<String>();
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        return wordSpace.getVector(term);
    }

    /**
     * {@inheritDoc}
     */
    public void handleContextVector(String focusKey,
                                    String secondaryKey,
                                    SparseDoubleVector context) {
        // Find the most similar existing word sense.
        int senseNumber = 0;
        int bestSense = 0;
        double bestSimilarity = -1;

        while (true) {
            // Create the word sense key based on the sense number.
            Vector wordSense = getVector((senseNumber == 0)
                    ? focusKey
                    : focusKey + "-" + senseNumber);

            // If no word sense exists then we have examined all known word senses.
            if (wordSense == null)
                break;

            // Compute the similarity of this context vector to the current word
            // sense.
            double similarity = Similarity.cosineSimilarity(wordSense, context);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestSense = senseNumber;
            }

            senseNumber++;
        }

        // If a reporter is provided, report the sense labeling.
        if (reporter != null)
            reporter.updateAssignment(focusKey, secondaryKey, bestSense);
    }


    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties props) {
        if (reporter != null)
            reporter.finalizeReport();
    }
}

