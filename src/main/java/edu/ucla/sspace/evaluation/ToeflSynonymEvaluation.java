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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * A test of eighty synonym questions gathered from the Test of English as a
 * Foreign Langauge (TOEFL) exam gathered by Thomas Landauer.  See <ul> <li
 * style="font-family:Garamond, Georgia, serif"> Landauer, T. K., and Dumais,
 * S. T. (1997). A solution to Plato's problem: The Latent Semantic Analysis
 * theory of the acquisition, induction, and representation of knowledge.
 * <i>Psychological Review</i>, <b>104</b>, 211-240.  (Available <a
 * href="http://lsa.colorado.edu/papers/plato/plato.annote.html">here</a>) </li>
 * </ul> for details on the test.

 *
 * @see ESLSynonymEvaluation
 */
public class ToeflSynonymEvaluation implements WordChoiceEvaluation {

    /**
     * The questions for the Toefl Test
     */
    private final Collection<MultipleChoiceQuestion> questions;
    
    /**
     * Constructs this evaluation test using the TOEFL test question file
     * refered to by the provided name.
     *
     * @param questionFileName the name of the file that contains the TOEFL
     *        questions
     * @param answerFileName the name of the file that contains the answers to
     *        the TOEFL questions
     */
    public ToeflSynonymEvaluation(String questionFileName, 
                                  String answerFileName) {
        this(new File(questionFileName), new File(answerFileName));
    }
    
    /**
     * Constructs this evaluation test using the TOEFL test question file.
     *
     * @param questionFileName the file that contains the TOEFL questions
     * @param answerFileName the file that contains the answers to the TOEFL
     *        questions
     */
    public ToeflSynonymEvaluation(File questionsFile, File answerFile) {
        questions = parseTestFile(questionsFile, answerFile);
    }

    /**
     * Parses the TOEFL test file and returns the set of questions contained
     * therein.
     */
    private static Collection<MultipleChoiceQuestion> parseTestFile(
            File questionFile, File answerFile) {
        try {
            // First read in the answer key so we can later assign the question
            // their correct option
            BufferedReader br = new BufferedReader(new FileReader(answerFile));
            List<Integer> answers = new ArrayList<Integer>();
            for (String line = null; (line = br.readLine()) != null; ) { 
                // Skip blank lines in the file
                if (line.length() == 0)
                    continue;
                // The answers are formatted in 4 fields where the correct
                // option is in the 4th column.  Correct options are marker
                // using their character option (starting at 'a'), so subtrack
                // 'a' from it the integer 0-4 that we will use as our index
                answers.add(line.split("\\s+")[3].charAt(0) - 'a');
            }
            br.close();
            
            // Then parse the question file for the prompt and options
            int question = 0;
            br = new BufferedReader(new FileReader(questionFile));
            Collection<MultipleChoiceQuestion> questions = 
                new LinkedList<MultipleChoiceQuestion>();
            for (String line = null; (line = br.readLine()) != null; ) {
                // Skip blank lines in the file
                if (line.length() == 0)
                    continue;

                // Expect 5 lines, 1 for the prompt and 4 for the answers
                String prompt = line.split("\\s+")[1];
                String[] options = new String[4];
                for (int i = 0; i < options.length; ++i) 
                    options[i] = br.readLine().split("\\s+")[1];                
                List<String> optionsAsList = Arrays.asList(options);
                int correctOption = answers.get(question);
                questions.add(new SimpleMultipleChoiceQuestion(
                    prompt, optionsAsList, correctOption));
                question++;
            }
            br.close();

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
        return "TOEFL Synonym Test";
    }
}
