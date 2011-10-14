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

import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link AssignmentReporter} that creates a SenseEval or SemEval answer key.
 * This should be used in conjunction with a {@link SemEvalContextExtractor}.
 * When reporting, Primary keys are not used.  Secondary keys are expected to be
 * the instance identifier.  For each call to {@code updateAssignment}, the
 * reporter will generate a line of the form
 *   word.pos word.pos.idNumber word.pos.clusterNumber
 * which designates word.pos.idNumber is the secondary key.  The line signifies
 * that the given word instance was assigned to a cluster identified by
 * clusterNumber.
 *
 * @author Keith Stevens
 */
public class SemEvalReporter implements AssignmentReporter {

    /**
     * The assignment map used to record context labels.
     */
    private final Map<String, List<Assignment>> assignmentMap;

    /**
     * The writer used to output the SemEval answer key.
     */
    private PrintStream writer;

    /**
     * Creates a new {@link SemEvalReporter}.
     *
     * @param stream The stream to which the answer key should be written.
     */
    public SemEvalReporter(OutputStream stream) {
        writer = new PrintStream(stream);
        assignmentMap = new HashMap<String, List<Assignment>>();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void updateAssignment(String primaryKey, 
                                              String secondaryKey,
                                              int clusterId) {
        // Get the word and part of speech information.
        int splitIndex = secondaryKey.lastIndexOf(".");
        String wordPos = secondaryKey.substring(0, splitIndex);

        // Print out the answer key.
        writer.printf("%s %s %s.%d\n",
                      wordPos, secondaryKey, wordPos, clusterId);
    }

    /**
     * {@inheritDoc}
     */
    public void finalizeReport() {
        writer.close();
    }

    /**
     * {@inheritDoc}
     */
    public void assignContextToKey(String primaryKey,
                                   String secondaryKey,
                                   int contextId) {
        // Get the mapping from primarykeys to context descriptors.
        List<Assignment> primaryAssignments = assignmentMap.get(primaryKey);
        if (primaryAssignments == null) {
            synchronized (this) {
                primaryAssignments = assignmentMap.get(primaryKey);
                if (primaryAssignments == null) {
                    primaryAssignments = Collections.synchronizedList(
                            new ArrayList<Assignment>());
                    assignmentMap.put(primaryKey, primaryAssignments);
                }
            }
        }

        primaryAssignments.add(new Assignment(secondaryKey, contextId));
    }

    /**
     * {@inheritDoc}
     */
    public String[] contextLabels(String primaryKey) {
        // Get the mapping from primarykeys to context descriptors.
        List<Assignment> primaryAssignments = assignmentMap.get(primaryKey);

        // Return an empty array if one does not exist.
        if (primaryAssignments == null)
            return new String[0];

        // Copy the label assignments for each context id recorded.  Here we
        // assume that the largest_context_id == (#id's_recorded - 1).
        String[] labels = new String[primaryAssignments.size()];
        for (Assignment assignment : primaryAssignments)
            labels[assignment.id] = assignment.key;
        return labels;
    }

    /**
     * A simple struct class to prevent auto boxing and unboxing of integers.
     */
    private class Assignment {
        public String key;
        public int id;

        public Assignment(String key, int id) {
            this.key = key;
            this.id = id;
        }
    }
}
