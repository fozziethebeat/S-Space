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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.Assignment;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;


/**
 * A set of soft assignment.  Each data point may be assigned to multiple
 * clusters.  None of the particular clusters have any relationship with each
 * other.
 *
 * @author Keith Stevens
 */
public class SoftAssignment implements Assignment {

    /**
     * The array of assignments.
     */
    private int[] assignments;
    
    /**
     * Constructs a new {@link SoftAssignment} based on the given array of
     * assignments.
     */
    public SoftAssignment(int[] assignments) {
        this.assignments = assignments;
    }

    /**
     * Creates a new {@link SoftAssignment} where the data point is assigned to
     * the specified clusters.
     */
    public SoftAssignment(Collection<Integer> clusterIds) {
        assignments = new int[clusterIds.size()];
        Iterator<Integer> it = clusterIds.iterator();
        for (int i = 0; i < assignments.length; ++i)
            assignments[i] = it.next();
    }

    /**
     * Creates a new {@link SoftAssignment} where the data point is assigned to
     * the specified clusters.
     */
    public SoftAssignment(Integer... clusterIds) {
        assignments = new int[clusterIds.length];
        for (int i = 0; i < clusterIds.length; ++i)
            assignments[i] = clusterIds[i];
    }

    /**
     * {@inheritDoc}
     */
    public int[] assignments() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return assignments.length;
    }

    public String toString() {
        return "SoftAssignment" + Arrays.toString(assignments);
    }
}
