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

import java.io.OutputStream;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A {@link AssignmentReporter} that creates a PseudoWord answer key .
 * This should be used in conjunction with a {@link PseudoWordContextExtractor}.
 * When reporting, primary keys are expected to be the pseudo word while
 * secondary keys are expected to be the actual word used in a given instance.
 * The reporter will record the number of times each secondary key was assigned
 * to a particular cluster for each primary key.  When the report is finalized,
 * it will generate a lines of the form:
 * </li> primaryKey secondaryKey clusterNumber assignmentCount
 * This can later be used to determine what words best describe each cluster.
 *
 * @author Keith Stevens
 */
public class PseudoWordReporter implements AssignmentReporter {

    /**
     * The writer used to output the PseudoWord answer key.
     */
    private PrintStream writer;

    /**
     * A mapping from primary keys to secondary keys to the set of data point
     * ids that were assigned to each secondary key.  We use a {@link BitSet}
     * here since there should only be a few secondary keys and each secondary
     * key may have many context id's associated with it.
     */
    private final Map<String, Map<String, BitSet>> contextAssignments;
 
    /**
     * A mapping from primary keys to secondary keys to the number of times that
     * each secondary key was assigned to a particular cluster.
     */
    private Map<String, Map<String, List<Integer>>> clusterCounts;

    /**
     * Creates a new {@link PseudoWordReporter}.
     *
     * @param stream The stream to which the answer key should be written.
     */
    public PseudoWordReporter(OutputStream stream) {
        writer = new PrintStream(stream);
        clusterCounts = new HashMap<String, Map<String, List<Integer>>>();
        contextAssignments = new HashMap<String, Map<String, BitSet>>();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void updateAssignment(String primaryKey, 
                                              String secondaryKey,
                                              int clusterId) {
        if (primaryKey.equals(secondaryKey))
            return;

        // Get the mapping from seconday keys to cluster assignment counts.
        Map<String, List<Integer>> secondaryCounts = clusterCounts.get(
                primaryKey);
        if (secondaryCounts == null) {
            secondaryCounts = new HashMap<String, List<Integer>>();
            clusterCounts.put(primaryKey, secondaryCounts);
        }

        // Get the list of cluster assignment counts.
        List<Integer> counts = secondaryCounts.get(secondaryKey);
        if (counts == null) {
            counts = new ArrayList<Integer>(clusterId);
            secondaryCounts.put(secondaryKey, counts);
        }

        // Add zero values for any clusters that are not currently recorded.
        while (clusterId >= counts.size())
            counts.add(0);

        // Make the assignment for the given cluster id.
        counts.set(clusterId, counts.get(clusterId) + 1);
    }

    /**
     * {@inheritDoc}
     */
    public void finalizeReport() {
        // For each sense labeling made, print out a triplet consisting of the
        // primary key, secondary key, the cluster id, and the number of times
        // that the secondary key was assigned to the cluster.
        for (Map.Entry<String, Map<String, List<Integer>>> e :
                 clusterCounts.entrySet()) {
            String firstKey = e.getKey();
            for (Map.Entry<String, List<Integer>> f : e.getValue().entrySet()) {
                String secondKey = f.getKey();
                List<Integer> counts = f.getValue();
                for (int i = 0; i < counts.size(); ++i)
                    if (counts.get(i) > 0)
                        writer.printf("%s %s %d %d\n",
                                      firstKey, secondKey, i, counts.get(i));
            }
        }
        writer.close();
    }

    /**
     * Records an assignment of {@code contextId} to {@code secondaryKey} and
     * {@code primaryKey}.
     */
    public void assignContextToKey(String primaryKey,
                                   String secondaryKey,
                                   int contextId) {
        // Get the mapping from secondary keys to context ids.
        Map<String, BitSet> termContexts = contextAssignments.get(primaryKey);
        if (termContexts == null) {
            synchronized (this) {
                termContexts = contextAssignments.get(primaryKey);
                if (termContexts == null) {
                    termContexts = new HashMap<String, BitSet>();
                    contextAssignments.put(primaryKey, termContexts);
                }
            }
        }

        // Get the set of context id's made to the secondary key.
        BitSet contextIds = termContexts.get(secondaryKey);
        if (contextIds == null) {
            synchronized (this) { 
                contextIds = termContexts.get(secondaryKey);
                if (contextIds == null) {
                    contextIds = new BitSet();
                    termContexts.put(secondaryKey, contextIds);
                }
            }
        }

        // Update the set of context ids assigned to the secondary key.
        synchronized (contextIds) {
            contextIds.set(contextId);
        }
    }

    /**
     * Return an array mapping context ids to secondary keys. Returns an empty
     * array if there was no tracking done.
     */
    public String[] contextLabels(String primaryKey) {
        Map<String, BitSet> termContexts = contextAssignments.get(primaryKey);
        if (termContexts == null)
            return new String[0];

        // Compute the total number of assignments made.
        int totalAssignments = 0;
        for (Map.Entry<String, BitSet> entry : termContexts.entrySet())
            totalAssignments = Math.max(
                    totalAssignments, entry.getValue().length());
        
        // Fill in each assignment with the secondary key attached to each
        // context id.
        String[] contextLabels = new String[totalAssignments];
        for (Map.Entry<String, BitSet> entry : termContexts.entrySet()) {
            BitSet contextIds = entry.getValue();
            for (int contextId = contextIds.nextSetBit(0); contextId >= 0;
                     contextId = contextIds.nextSetBit(contextId+1))
                contextLabels[contextId] = entry.getKey();
        }
        return contextLabels;
    }
}
