/*
 * Copyright 2009 David Jurgens 
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * A test of synonym questions gathered from the ESL (English as a Second
 * Language) exam by Peter Turney.  
 *
 * See Peter Turney's <a
 * href="http://www.aclweb.org/aclwiki/index.php?title=ESL_Synonym_Questions_(State_of_the_art)">webpage
 * on the test</a> for full details on the test and how to get a copy of the
 * questions.
 *
 * @see TOEFLSynonymEvaluation
 */
public class ESLSynonymEvaluation implements WordChoiceEvaluation {

    /**
     * The questions for the ESL Test
     */
    private final Collection<MultipleChoiceQuestion> questions;

    /**
     * The name of the data file for this test
     */
    private final String dataFileName;
    
    /**
     * Constructs this evaluation test using the ESL test question file refered
     * to by the provided name.
     */
    public ESLSynonymEvaluation(String eslQuestionsFileName) {
        this(new File(eslQuestionsFileName));
    }
    
    /**
     * Constructs this evaluation test using the ESL test question file.
     */
    public ESLSynonymEvaluation(File eslQuestionsFile) {
        questions = parseTestFile(eslQuestionsFile);
        dataFileName = eslQuestionsFile.getName();
    }

    /**
     * Parses the ESL test file and returns the set of questions contained
     * therein.
     */
    private static Collection<MultipleChoiceQuestion> parseTestFile(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            Collection<MultipleChoiceQuestion> questions = 
                new LinkedList<MultipleChoiceQuestion>();
            for (String line = null; (line = br.readLine()) != null; ) {

                // skip comments and blank lines
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }

                // Expect 5 words, | delimited, where the first is the prompt,
                // the correct answer is in index 1, and the other 3 words
                // follow
                String[] promptAndOptions = line.split("\\|");

                // each of the strings has extra white space padding, so trim it
                // off
                for (int i = 0; i < promptAndOptions.length; ++i) {
                    promptAndOptions[i] = promptAndOptions[i].trim();
                }

                String prompt = promptAndOptions[0];
                String[] options = new String[promptAndOptions.length - 1];
                List<String> optionsAsList = Arrays.asList(Arrays.copyOfRange(
                    promptAndOptions, 1, promptAndOptions.length - 1));
                questions.add(
                    new SimpleMultipleChoiceQuestion(prompt, optionsAsList, 0));
            }

            return questions;
        } catch (IOException ioe) {
            // rethrow, as any IOException is fatal to evaluation
            throw new IOError(ioe);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<MultipleChoiceQuestion> getQuestions() {
        return questions;
    }

    public String toString() {
        return "Word Choice [" + dataFileName + "]";
    }
}
