/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

import java.util.Properties;


/**
 * A generic class for clustering graphs stored as adjacency matrices.  This
 * base provides method for algorithms that do not cluster using a fixed number
 * of clusters but may need to in some cases.  For those algorithms, calls to
 * {@link cluster(Matrix, int, Properties)} will call {@link cluster(Matrix,
 * Properties)} and then merge the returned clusters using {@link
 * HierarchicalAgglomerativeClustering}.
 *
 * @author Keith Stevens
 */
public abstract class AbstractGraphClustering implements Clustering {

    private final Clustering hac;

    public AbstractGraphClustering() {
        hac = new HierarchicalAgglomerativeClustering();
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adjacency,
                               int k,
                               Properties props) {
        // Cluster the adjacency matrix with the full algorithm and no
        // specification for the number of clusters.
        Assignments baseAssignments = cluster(adjacency, props);

        // If the base algorithm generated the number of clusters we wanted, or
        // fewer, return that assignment set, as we can't do any better than
        // that.
        if (baseAssignments.numClusters() <= k)
            return baseAssignments;

        // Otherwise, use HAC to condense the given clusters.  We do this by
        // forming a new adjacency matrix that sums the edges connecting pairs
        // of clusters and clustering that with HAC.

        // Build the new adjacency matrix.
        Matrix newAdjacency = new ArrayMatrix(
                baseAssignments.numClusters(), baseAssignments.numClusters());
        for (int row = 0; row < adjacency.rows(); ++row) {
            int newRow = baseAssignments.get(row);
            for (int col = 0; col < adjacency.rows(); ++col) {
                int newCol = baseAssignments.get(col);
                newAdjacency.set(newRow, newCol, 
                                 newAdjacency.get(newRow, newCol) +
                                 adjacency.get(row, col));
            }
        }

        Assignments hacAssignments = hac.cluster(newAdjacency, k, props);
        Assignments finalAssignments = new Assignments(k, adjacency.rows());
        for (int x = 0; x < adjacency.rows(); x++)
            finalAssignments.set(x, hacAssignments.get(baseAssignments.get(x)));

        return finalAssignments;
    }
}
