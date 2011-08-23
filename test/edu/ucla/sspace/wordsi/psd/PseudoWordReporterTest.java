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

package edu.ucla.sspace.wordsi.psd;

import edu.ucla.sspace.wordsi.AssignmentReporter;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class PseudoWordReporterTest {

    @Test public void testUpdateAssignment() {
        OutputStream stream = new ByteArrayOutputStream();
        AssignmentReporter reporter = new PseudoWordReporter(stream);
        reporter.updateAssignment("catdog", "dog", 5);
        reporter.updateAssignment("catdog", "dog", 2);
        reporter.updateAssignment("catdog", "dog", 5);
        reporter.updateAssignment("catdog", "cat", 1);
        reporter.updateAssignment("catdog", "cat", 1);
        reporter.updateAssignment("catdog", "cat", 5);
        reporter.finalizeReport();

        String computed = stream.toString();
        int[] dogCounts = new int[] {0, 0, 1, 0, 0, 2};
        int[] catCounts = new int[] {0, 2, 0, 0, 0, 1};
        for (int i = 0; i < dogCounts.length; ++i) {
            assertTrue(computed.contains("catdog dog "+i+" "+dogCounts[i]));
            assertTrue(computed.contains("catdog cat "+i+" "+catCounts[i]));
        }
    }

    @Test public void testAssignContextToKey() {
        OutputStream stream = new ByteArrayOutputStream();
        AssignmentReporter reporter = new PseudoWordReporter(stream);

        reporter.assignContextToKey("catdog", "cat", 1);
        reporter.assignContextToKey("catdog", "dog", 0);
        reporter.assignContextToKey("catdog", "dog", 2);
        reporter.assignContextToKey("catdog", "dog", 3);

        String[] expected = new String[] {"dog", "cat", "dog", "dog"};
        String[] computed = reporter.contextLabels("catdog");
        assertEquals(expected.length, computed.length);
        for (int i = 0; i < expected.length; ++i)
            assertEquals(expected[i], computed[i]);
    }
}
