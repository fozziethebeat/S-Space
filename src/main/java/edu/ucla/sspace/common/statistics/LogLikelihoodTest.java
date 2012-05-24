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

public class LogLikelihoodTest implements SignificanceTest {

    /**
     * Returns the log-likelihood test statistic
     */
    public double score (int both, int justA, int justB, int neither) {
       
        double rowEntropy = entropy(both, justA) + entropy(justB, neither);
        double columnEntropy = entropy(both, justB) + entropy(justA, neither);
        double matrixEntropy = entropy(both, justA, justB, neither);
        return 2 * (matrixEntropy - rowEntropy - columnEntropy);
    }             

    private static double entropy(int x, int y) {
        assert x >= 0 && y >= 0 : "negative counts";
        double sum = x + y;

        double result = 0.0;        
        result += (x == 0) ? 0 : x * (Math.log(x) / sum);
        result += (y == 0) ? 0 : y * (Math.log(y) / sum);
        return -result;
    }

    private static double entropy(int a, int b, int c, int d) {
        assert a >= 0 && b >= 0 && c >= 0 && d >= 0 : "negative counts";
        double sum = a + b + c + d;

        double result = 0.0;        
        result += (a == 0) ? 0 : a * (Math.log(a) / sum);
        result += (b == 0) ? 0 : b * (Math.log(b) / sum);
        result += (c == 0) ? 0 : c * (Math.log(c) / sum);
        result += (d == 0) ? 0 : d * (Math.log(d) / sum);
        return -result;
    }
}
