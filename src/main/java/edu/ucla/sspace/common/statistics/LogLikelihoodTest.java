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
       
        /*

        double ll = 2 *
            both    * Math.log( (both / (double)row1sum) / (col1sum / sum)) +
            justA   * Math.log( (justA / (double)row1sum) / (col2sum / sum)) +
            justB   * Math.log( (justB / (double)row2sum) / (col1sum / sum)) +
            neither * Math.log( (neither / (double)row2sum) / (col2sum / sum));

        System.out.printf("%d\t%d%n%d\t%d%n  = 2 * (%f + %f + %f + %f)%n",
                          both, justA, justB, neither, 
                          both    * Math.log( (both / (double)row1sum) / (col1sum / sum)),
                          justA   * Math.log( (justA / (double)row1sum) / (col2sum / sum)),
                          justB   * Math.log( (justB / (double)row2sum) / (col1sum / sum)),
                          neither * Math.log( (neither / (double)row2sum) / (col2sum / sum)));
                                                    


        return ll;        
        */    

        /*
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

        return 2 *
            ((both * Math.log(both - aExp))  +
             (justB * Math.log(justB - bExp))  +
             (justA * Math.log(justA - cExp))  +
             (neither * Math.log(neither - dExp)));
        */

        

}
