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

package edu.ucla.sspace.util;

import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.vector.*;

/**
 * Tests for the {@link KrippendorffsAlpha} class.
 */
public class KrippendorffsAlphaTest {

    private final Matrix testMatrix;
    private final Matrix wikiTestMatrix;

    public KrippendorffsAlphaTest() {

        double n = Double.NaN;

        double[][] responses = {
            {1, 2, 3, 3, 2, 1, 4, 1, 2, n, n, n },
            {1, 2, 3, 3, 2, 2, 4, 1, 2, 5, n, 3 },
            {n, 3, 3, 3, 2, 3, 4, 2, 2, 5, 1, n },
            {1, 2, 3, 3, 2, 4, 4, 1, 2, 5, 1, n }
        };

        List<DoubleVector> vectors = new ArrayList<DoubleVector>();
        for (int i = 0; i < 4; ++i)
            vectors.add(Vectors.asVector(responses[i]));
        testMatrix = Matrices.asMatrix(vectors);



        double[][] responses2 = {
            {n, n, n, n, n, 3, 4, 1, 2, 1, 1, 3, 3, n, 3},
            {1, n, 2, 1, 3, 3, 4, 3, n, n, n, n, n, n, n},
            {n, n, 2, 1, 3, 4, 4, n, 2, 1, 1, 3, 3, n, 4},
        };

        List<DoubleVector> vectors2 = new ArrayList<DoubleVector>();
        for (int i = 0; i < responses2.length; ++i)
            vectors2.add(Vectors.asVector(responses2[i]));
        wikiTestMatrix = Matrices.asMatrix(vectors2);
        
    }

    // @Test public void testNominal() {
    //     double alpha = new KrippendorffsAlpha().compute(testMatrix, 
    //         KrippendorffsAlpha.LevelOfMeasurement.NOMINAL);
    //     assertEquals(0.743, alpha, 0.001);
    // }

    //  @Test public void testNominal2() {
    //     double alpha = new KrippendorffsAlpha().compute(wikiTestMatrix, 
    //         KrippendorffsAlpha.LevelOfMeasurement.NOMINAL);
    //     assertEquals(0.691, alpha, 0.001);
    // }
   
    @Test public void testOrdinal() {
        double alpha = new KrippendorffsAlpha().compute(testMatrix, 
            KrippendorffsAlpha.LevelOfMeasurement.ORDINAL);
        assertEquals(0.815, alpha, 0.001);
     }
    
    //  @Test public void testInterval() {
    //     double alpha = new KrippendorffsAlpha().compute(testMatrix, 
    //         KrippendorffsAlpha.LevelOfMeasurement.INTERVAL);
    //     assertEquals(0.849, alpha, 0.001);
    // }

    //  @Test public void testInterval2() {
    //     double alpha = new KrippendorffsAlpha().compute(wikiTestMatrix, 
    //         KrippendorffsAlpha.LevelOfMeasurement.INTERVAL);
    //     assertEquals(0.811, alpha, 0.001);
    // }

}
