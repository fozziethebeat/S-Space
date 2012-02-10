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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.TransformStatistics.MatrixStatistics;

import java.io.File;


/**
 * Computes the <a
 * href="http://en.wikipedia.org/wiki/Pearson%27s_chi-squared_test">Pearson
 * Chi-Squared</a> value for each joint event stored in a contingency matrix.
 *
 * @author Keith Stevens
 */
public class ChiSquaredTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new ChiSquaredGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new ChiSquaredGlobalTransform(inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "ChiSquared";
    }

    public class ChiSquaredGlobalTransform implements GlobalTransform {

        /**
         * The total sum of occurances for each row (row).
         */
        private double[] rowSums;

        /**
         * The total sum of occurances for each col (column).
         */
        private double[] columnSums;

        /**
         * The total summation of the entire matrix.
         */
        private double total;

        /**
         * Creates an instance of {@code ChiSquaredTransform}
         * from a given {@link Matrix}.
         */
        public ChiSquaredGlobalTransform(Matrix matrix) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(matrix);
            rowSums = stats.rowSums;
            columnSums = stats.columnSums;
            total = stats.matrixSum;
        }

        /**
         * Creates an instance of {@code ChiSquaredTransform}
         * from a matrix {@code File} of format {@code format}.
         */
        public ChiSquaredGlobalTransform(
                File inputMatrixFile,
                MatrixIO.Format format) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(inputMatrixFile, format);
            rowSums = stats.rowSums;
            columnSums = stats.columnSums;
            total = stats.matrixSum;
        }

        /**
         * Computes the Pearson Chi-Squared value for the joint event defined by
         * {@code row} and {@code col} with {@code value} specifying the number
         * of occurances of {@code row} with {@code col}.   
         *
         * @param row The index specifying the first event being observed
         * @param col The index specifying the second event being observed
         * @param value The number of joint occurrences
         */
        public double transform(int row, int col, double both) {
            double justA = rowSums[row] - both;
            double justB = columnSums[col] - both;
            double neither = total - justA - justB - both;
            return score(both, justA, justB, neither);
        }
    }

    /**
     * Returns the Pearson Chi-Squared test of significance using one degree of
     * freedom.  To identify events with a significance value above .95, or p
     * &ample; .05, reject any chi-squared values less than 3.841.
     */
    public static double score(double both, 
                               double justA, 
                               double justB, 
                               double neither) {
        // Think of the table as
        //      B       !B
        //   A: both    justA   : row1Sum
        //  !A: justB   neither : row2Sum
        //  ---------------------------
        //      col1Sum col2Sum : sum
        double col1sum = both + justB; 
        double col2sum = justA + neither;
        double row1sum = both + justA; 
        double row2sum = justB + neither;
        double sum = row1sum + row2sum;
        
        // Calculate the expected values for a, b, c, d
        // The expected value is coliSum * rowjSum / sum 
        double aExp = (row1sum / sum) * col1sum;
        double bExp = (row1sum / sum) * col2sum;
        double cExp = (row2sum / sum) * col1sum;
        double dExp = (row2sum / sum) * col2sum;

        // Chi-squared is (Observed - Expected)^2 / Expected
        return (Math.pow(both-aExp, 2) / aExp) +
               (Math.pow(justA-bExp, 2) / bExp) +
               (Math.pow(justB-cExp, 2) / cExp) +
               (Math.pow(neither-dExp, 2) / dExp);
    }
}
