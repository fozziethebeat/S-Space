/*
 * Copyright 2011 David Jurgens
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

import java.util.Collection;
import java.util.Iterator;


/**
 * An implementation of a {@link Assignment} where a data point may be assigned
 * to multiple clusters
 */
public class SoftAssignment implements Assignment {

    /**
     * The array holding the single assignment.
     */
    private final int[] assignments;

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
}
