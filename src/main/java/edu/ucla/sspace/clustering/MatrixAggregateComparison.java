/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
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
import edu.ucla.sspace.matrix.MatrixAggregate;


/**
 * A {@link PartitionComparison} that utilizes a {@link
 * MatrixAggregate} function.  Before calling the {@link MatrixAggregate}
 * method, {@link #compare} computes a contingency matrix for each data point
 * appearing in the two compared {@link Partition}s.
 *
 * @see MatrixAggregate
 *
 * @author Keith Stevens
 */
public abstract class MatrixAggregateComparison implements PartitionComparison {

    /**
     * The {@link MatrixAggregate} used to compare two {@link Partition}s.
     */
    private final MatrixAggregate aggregator;

    /**
     * Creates a new {@link MatrixAggregateComparison} using the given {@link
     * MatrixAggregate} function.
     */
    public MatrixAggregateComparison(MatrixAggregate aggregator) {
        this.aggregator = aggregator;
    }

    /**
     * {@inheritDoc}
     */
    public double compare(Partition p1, Partition p2) {
        // Create the contingency matrix.
        Matrix contingency = new ArrayMatrix(p1.numClusters(), p2.numClusters());
        for (int i = 0; i < p1.numPoints(); ++i) {
            int x = p1.assignments()[i];
            int y = p2.assignments()[i];
            if (x >= 0 && y >= 0)
                contingency.add(x, y, 1d);
        }

        // Aggregate the data.
        return aggregator.aggregate(contingency);
    }
}
