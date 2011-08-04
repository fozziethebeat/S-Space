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

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


public class ClusterUtil {

    public static List<SparseDoubleVector> generateCentroids(
            List<SparseDoubleVector> dataPoints,
            Assignment[] assignments,
            int vectorLength) {
        ArrayList<SparseDoubleVector> centroids =
            new ArrayList<SparseDoubleVector>();
        int index = -1;
        for (SparseDoubleVector dataPoint : dataPoints) {
            index++;
            int[] itemAssignments = assignments[index].assignments();

            // Skip items that were not assigned to any
            // cluster.
            if (itemAssignments.length == 0)
                continue;

            int assignment = itemAssignments[0];

            // Ensure that the list of centroids has at least an
            // empty vector for itself.
            for (int i = centroids.size(); i <= assignment; ++i)
                centroids.add(new SparseHashDoubleVector(vectorLength));
            // Add the context to the assigned cluster.
            VectorMath.add(centroids.get(assignment), dataPoint);
        }
        return centroids;
    }
}
