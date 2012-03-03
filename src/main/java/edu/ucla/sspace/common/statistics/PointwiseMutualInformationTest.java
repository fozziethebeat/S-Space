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

package edu.ucla.sspace.common.statistics;

import edu.ucla.sspace.common.Statistics;

public class PointwiseMutualInformationTest implements SignificanceTest {

    /**
     * Returns the PMI score of the both A and B.
     */
    public double score (int both, int justA, int justB, int neither) {
        int all = both + justA + justB + neither;
        double probA = (both + justA) / (double)all;
        double probB = (both + justB) / (double)all;
        double probAandB = both / (double)all;
        return Statistics.log2(probAandB / (probA * probB));
    }
}
