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


/**
 * Computes a distance version of the popular <a
 * href="http://en.wikipedia.org/wiki/Rand_index">Rand Index</a>.  Instead of
 * computing the number of agreements between two {@link Partition}s, this
 * computes the number of disagreements between two {@link Partition}s.
 *
 * @author Keith Stevens
 */
public class RandDistance extends PartitionOverlapComparison {

    /**
     * {@inheritDoc}
     */
    public double compare(Partition p1, Partition p2) {
        // Compute the number of co-clustered elements in p1 and p2.  This runs
        // in O(n log n) time where n in the number of elements in p1.
        double overlap = super.compare(p1, p2);

        // Compute the number of co-clustered elements in each partition.  This
        // runs in O(numClusters) for both p1 and p2.
        int p1Pairs = p1.numPairs();
        int p2Pairs = p2.numPairs();

        // The sum of co-clustered elements in p1 and p2 account for the
        // pairings where each partition disagree AND they each account for the
        // overlapping co-clustered pairs.  So sum the two sums and subtract the
        // overlap count twice to compute the final distance.
        return p1Pairs + p2Pairs - 2*overlap;
    }

    /**
     * Returns true.
     */
    public boolean isDistance() {
        return true;
    }
}
