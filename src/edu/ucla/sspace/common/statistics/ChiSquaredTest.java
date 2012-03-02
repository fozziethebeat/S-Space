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

public class ChiSquaredTest implements SignificanceTest {

    /**
     * Returns the &Chi;<sup>2</sup> test statistic
     */
    public double score (int both, int justA, int justB, int neither) {
        int col1sum = both + justA;    // t[0] + t[2];
        int col2sum = justB + neither; // t[1] + t[3];
        int row1sum = both + justB;    // t[0] + t[1];
        int row2sum = justA + neither; // t[2] + t[3];
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        // Chi-squared is (Observed - Expected)^2 / Expected
        return 
            ((both - aExp) * (both - aExp) / aExp) +
            ((justB - bExp) * (justB - bExp) / bExp) +
            ((justA - cExp) * (justA - cExp) / cExp) +
            ((neither - dExp) * (neither - dExp) / dExp);
    }
}
