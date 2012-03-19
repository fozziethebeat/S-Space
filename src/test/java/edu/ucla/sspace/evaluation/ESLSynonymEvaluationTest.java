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

package edu.ucla.sspace.evaluation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class ESLSynonymEvaluationTest {

    @Test public void testRAndG() throws IOException {
        File temp = File.createTempFile("ESLEvalTest", ".tst");
        temp.deleteOnExit();
        PrintWriter writer = new PrintWriter(temp);
        String[][] choices= {{"a", "b", "c", "d", "e"},
                             {"1", "2", "3", "4", "5"},
                             {"f", "g", "h", "i", "j"}};

        for (int i = 0; i < choices.length; ++i) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 5; ++j)
                sb.append(choices[i][j]).append(" | ");
            writer.println(sb.toString());
        }
        writer.flush();
        writer.close();

        ESLSynonymEvaluation eval = new ESLSynonymEvaluation(temp);

        Collection<MultipleChoiceQuestion> words = eval.getQuestions();
        int i = 0;
        for (MultipleChoiceQuestion sim : words) {
            assertEquals(choices[i][0], sim.getPrompt());
            assertEquals(0, sim.getCorrectAnswer());
            ++i;
        }
    }
}
