/*
 * Copyright 2012 David Jurgens
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

package edu.ucla.sspace.lsa;

import edu.ucla.sspace.basis.*;
import edu.ucla.sspace.common.*;
import edu.ucla.sspace.graph.*;
import edu.ucla.sspace.graph.io.*;
import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.matrix.factorization.*;
import edu.ucla.sspace.text.*;
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import edu.ucla.sspace.testsetup.DummyCorpus;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author David Jurgens
 */
public class LatentSemanticAnalysisTest {

    
    /**
     * Example taken from <a href="http://www.miislita.com/information-retrieval-tutorial/svd-lsi-tutorial-4-lsi-how-to-calculations.html#query>http://www.miislita.com/information-retrieval-tutorial/svd-lsi-tutorial-4-lsi-how-to-calculations.html#query</a>
     */
    @Test public void testProject() throws Exception {
        // Short circuit if we can't run the test
        if (!SingularValueDecompositionLibC.isSVDLIBCavailable())
            return;        
        

        int numDocs = 3;
        LatentSemanticAnalysis lsa =
            new LatentSemanticAnalysis(true, 2, new NoTransform(), 
                                       new SingularValueDecompositionLibC(),
                                       false, new StringBasisMapping());

        lsa.process(DummyCorpus.instance());
        lsa.build(System.getProperties());
        
        String query = "gold silver truck";
        
        DoubleVector projected = lsa.project(new SimpleDocument(new SimpleSentence(query)));
        assertEquals(2, projected.length());
        assertEquals(0.2140, Math.abs(projected.get(0)), 0.001);
        assertEquals(0.1821, Math.abs(projected.get(1)), 0.001);
        System.out.println("Projected: " + projected);
    }
}
