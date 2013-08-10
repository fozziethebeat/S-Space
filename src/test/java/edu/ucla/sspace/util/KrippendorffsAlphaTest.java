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

    public KrippendorffsAlphaTest() {
        double[][] responses = {
            {1,          2, 3, 3, 2, 1, 4, 1, 2, Double.NaN, Double.NaN, Double.NaN },
            {1,          2, 3, 3, 2, 2, 4, 1, 2, 5, Double.NaN, 3 },
            {Double.NaN, 3, 3, 3, 2, 3, 4, 2, 2, 5, 1, Double.NaN },
            {1,          2, 3, 3, 2, 4, 4, 1, 2, 5, 1, Double.NaN }
        };

        List<DoubleVector> vectors = new ArrayList<DoubleVector>();
        for (int i = 0; i < 4; ++i)
            vectors.add(Vectors.asVector(responses[i]));
        testMatrix = Matrices.asMatrix(vectors);
    }

    @Test public void testNominal() {
        double alpha = new KrippendorffsAlpha().compute(testMatrix, 
            KrippendorffsAlpha.LevelOfMeasurement.NOMINAL);
        assertEquals(0.743, alpha, 0.001);
    }
   

    /* @Test */ public void testOrdinal() {
        double alpha = new KrippendorffsAlpha().compute(testMatrix, 
            KrippendorffsAlpha.LevelOfMeasurement.ORDINAL);
        assertEquals(0.815, alpha, 0.001);
    }

    @Test public void testInterval() {
        double alpha = new KrippendorffsAlpha().compute(testMatrix, 
            KrippendorffsAlpha.LevelOfMeasurement.INTERVAL);
        assertEquals(0.849, alpha, 0.001);
    }

}
