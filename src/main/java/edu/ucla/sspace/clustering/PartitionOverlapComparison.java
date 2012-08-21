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

import java.util.Arrays;
import java.util.Comparator;


/**
 * An implementation of a similarity based {@link PartitionComparison} that
 * measures the number of agreements between two {@link Partition}s.  This
 * approach runs in O(n log n) time in terms of the number of points within a
 * {@link Partition}.
 *
 * @author Keith Stevens
 */
public class PartitionOverlapComparison implements PartitionComparison {

    /**
     * {@inheritDoc}
     */
    public double compare(Partition p1, Partition p2) {
        Integer[] indices = new Integer[p1.assignments().length];
        for (int i = 0; i < indices.length; ++i)
            indices[i] = i;
        Arrays.sort(indices, new PartitionComparator(p1, p2));

        int overlap = 0;
        int clusterSize = 1;
        for (int i = 1; i < indices.length; ++i) {
            int prevPoint = indices[i-1];
            int currPoint = indices[i];
            if (p1.assignments()[currPoint] == -1 && p2.assignments()[currPoint] == -1)
                continue;

            if (p1.assignments()[prevPoint] == p1.assignments()[currPoint] &&
                p2.assignments()[prevPoint] == p2.assignments()[currPoint])
                clusterSize++;
            else {
                overlap += clusterSize * (clusterSize - 1) / 2;
                clusterSize = 1;
            }
        }
        overlap += clusterSize * (clusterSize - 1) / 2;
        return overlap;
    }

    public boolean isDistance() {
        return false;
    }

    private static class PartitionComparator implements Comparator<Integer> {

        private final int[] a1;

        private final int[] a2;

        public PartitionComparator(Partition p1, Partition p2) {
            this.a1 = p1.assignments();
            this.a2 = p2.assignments();
        }

        public int compare(Integer x, Integer y) {
            return (a1[x] == a1[y]) ? a2[y] - a2[x] : a1[y] - a1[x];
        }

        public boolean equals(Object o) {
            return o instanceof PartitionComparator;
        }
    }
}
