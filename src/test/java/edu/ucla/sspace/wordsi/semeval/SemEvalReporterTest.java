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

package edu.ucla.sspace.wordsi.semeval;

import edu.ucla.sspace.wordsi.AssignmentReporter;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SemEvalReporterTest {

    @Test public void testUpdateAssignment() {
        OutputStream stream = new ByteArrayOutputStream();
        AssignmentReporter reporter = new SemEvalReporter(stream);
        reporter.updateAssignment("cat", "dog.n.123", 5);
        reporter.updateAssignment("dog", "cat.v.143", 55);
        reporter.finalizeReport();

        assertEquals("dog.n dog.n.123 dog.n.5\ncat.v cat.v.143 cat.v.55\n",
                     stream.toString());
    }

    @Test public void testAssignContextToKey() {
        OutputStream stream = new ByteArrayOutputStream();
        AssignmentReporter reporter = new SemEvalReporter(stream);

        reporter.assignContextToKey("cat", "cat.n.1", 1);
        reporter.assignContextToKey("cat", "cat.n", 0);

        String[] expected = new String[] {"cat.n", "cat.n.1"};
        String[] computed = reporter.contextLabels("cat");
        assertEquals(expected.length, computed.length);
        for (int i = 0; i < expected.length; ++i)
            assertEquals(expected[i], computed[i]);
    }
}
