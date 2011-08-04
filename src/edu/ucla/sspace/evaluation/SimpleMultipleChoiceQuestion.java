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

import java.util.List;

/**
 * An implementation of {@code MultipleChoiceQuestion} for containing a prompt,
 * list of options and the correct index.
 */
public class SimpleMultipleChoiceQuestion implements MultipleChoiceQuestion {

    /**
     * The question prompt
     */
    private final String prompt;
    
    /**
     * The options from which to choose
     */
    private final List<String> options;
    
    /**
     * The option index that is the correct answer
     */
    private final int correctAnswer;

    /**
     * Constructs this question with the provided prompt, options and answer.
     */
    public SimpleMultipleChoiceQuestion(String prompt,
                                        List<String> options,
                                        int correctAnswer) {
        this.prompt = prompt;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    /**
     * {@inheritDoc}
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getOptions() {
        return options;
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrectAnswer() {
        return correctAnswer;
    }

    /**
     * Returns a human-readable form of the question.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(100);
        sb.append(prompt).append(":\n");
        int i = 0;
        for (String option : options) {
            sb.append(i).append(" - ").append(option);
            if (i == correctAnswer) {
                sb.append("\t(correct)");
            }
            sb.append("\n");
            i++;
        }
        return sb.toString();
    }
}
