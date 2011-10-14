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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashSet;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class OneNormedQuestionPerLineTest 
        extends AbstractNormedWordPrimingTest { 

    /**
     * Creates a new {@link OneNormedQuestionPerLineTest} from a string
     * designated a file name.
     */
    public OneNormedQuestionPerLineTest(String primingFile) {
        this(new File(primingFile));
    }

    /**
     * Creates a new {@link OneNormedQuestionPerLineTest} from a {@link File}.
     */
    public OneNormedQuestionPerLineTest(File primingFile) {
        super(prepareQuestionSet(primingFile));
    }

    /**
     * Returns a set of {@link NormedPrimingQuestion}s that are extracted from a
     * text file.
     */
    private static Set<NormedPrimingQuestion> prepareQuestionSet(
            File primingFile) {
        Set<NormedPrimingQuestion> questions =
            new HashSet<NormedPrimingQuestion>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(primingFile));
            // Read each line as if it were a new question.  Words in each
            // question are separated by pipes ("|").  The first word of each
            // line is the cue.  For each target, there is is the target string
            // and the target association.
            for (String line = null; (line = br.readLine()) != null;) {
                String[] cueAndTargets = line.split("\\|");
                String[] targets = new String[cueAndTargets.length - 1];
                double[] strengths = new double[cueAndTargets.length - 1];

                // Targets and their associational strength are separated by
                // commas.
                for (int i = 1; i < cueAndTargets.length; ++i) {
                    String[] targetAndStrength = cueAndTargets[i].split(",");
                    targets[i-1] = targetAndStrength[0].trim();
                    strengths[i-1] = Double.parseDouble(targetAndStrength[1]);
                }

                // Add the new question.
                questions.add(new SimpleNormedPrimingQuestion(
                            cueAndTargets[0].trim(), targets, strengths));
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return questions;
    }

    /**
     * {@inheritDoc}
     */
    public double computeStrength(SemanticSpace sspace,
                                  String word1,
                                  String word2) {
        return Similarity.cosineSimilarity(sspace.getVector(word1),
                                           sspace.getVector(word2));
    }
}
