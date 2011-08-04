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


public class FinkelsteinEtAl353WordSimilarityEvaluationTest {

    @Test public void testRAndG() throws IOException {
        File temp = File.createTempFile("FinkleTest", ".tst");
        temp.deleteOnExit();
        PrintWriter writer = new PrintWriter(temp);
        String[][] wordPairs = {{"a", "b"}, {"1", "2"}, {"cat", "dog"}};
        double[] similarities = {1, 2, 3};
        writer.println("sdjfskdfkdsj");
        writer.println("#word pair 2.0");
        for (int i = 0; i < similarities.length; ++i)
            writer.println(wordPairs[i][0] + " " + wordPairs[i][1] + " " +
                           similarities[i]);
        writer.flush();
        writer.close();

        FinkelsteinEtAl353WordSimilarityEvaluation eval =
            new FinkelsteinEtAl353WordSimilarityEvaluation(temp);

        assertEquals(0, eval.getLeastSimilarValue(), 0.00000001);
        assertEquals(10, eval.getMostSimilarValue(), 0.00000001);
        Collection<WordSimilarity> words = eval.getPairs();
        boolean[] correct = new boolean[wordPairs.length];

        for (WordSimilarity sim : words) {
            for (int i = 0; i < correct.length; ++i) {
                if (sim.getFirstWord().equals(wordPairs[i][0]) &&
                    sim.getSecondWord().equals(wordPairs[i][1]) &&
                    sim.getSimilarity() == similarities[i])
                    correct[i] = true;
            }
        }
        for (int i = 0; i < correct.length; ++i)
            assertTrue(correct[i]);
    }
}
