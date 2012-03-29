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
 * Computes the popular <a
 * href="http://en.wikipedia.org/wiki/Rand_index">Rand Index</a>, which reports
 * the number of agreements between two {@link Partition}s.
 *
 * @author Keith Stevens
 */
public class RandIndex extends RandDistance {

    public double compare(Partition p1, Partition p2) {
        // Compute the raw number of disagreements between p1 and p2.
        double distance = super.compare(p1, p2);

        // Compute the total number of pairings possible in any partition of
        // this size.
        int numPoints = p1.numPoints();
        int totalPairs = numPoints * (numPoints - 1) / 2;
        
        // Compute the number of total agreements, i.e. the number of
        // co-clustered pairs and number of pairs no co-clustered in both
        // partitions by subtracting the number of disagreements from the total
        // number of possible pairs.  
        double numAgreements = totalPairs - distance;

        // Normalize this by the total number of pairs to make this a metric.
        return numAgreements / totalPairs;
    }

    /**
     * Returns false.
     */
    public boolean isDistance() {
        return false;
    }
}
