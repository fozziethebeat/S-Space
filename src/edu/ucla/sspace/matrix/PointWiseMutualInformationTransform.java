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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.TransformStatistics.MatrixStatistics;

import java.io.File;


/**
 * @author Keith Stevens
 */
public class PointWiseMutualInformationTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new PointWiseMutualInformationGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new PointWiseMutualInformationGlobalTransform(
                inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "PMI";
    }

    public class PointWiseMutualInformationGlobalTransform
            implements GlobalTransform {

        /**
         * The total sum of occurances for each row (row).
         */
        private double[] rowCounts;

        /**
         * The total sum of occurances for each col (column).
         */
        private double[] colCounts;

        /**
         * The total summation of the entire matrix.
         */
        private double matrixSum;

        /**
         * Creates an instance of {@code PointWiseMutualInformationTransform}
         * from a given {@link Matrix}.
         */
        public PointWiseMutualInformationGlobalTransform(Matrix matrix) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(matrix);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            matrixSum = stats.matrixSum;
        }

        /**
         * Creates an instance of {@code PointWiseMutualInformationTransform}
         * from a matrix {@code File} of format {@code format}.
         */
        public PointWiseMutualInformationGlobalTransform(
                File inputMatrixFile,
                MatrixIO.Format format) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(inputMatrixFile, format);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            matrixSum = stats.matrixSum;
        }

        /**
         * Computes the point wise-mutual information between the {@code row}
         * and {@code col} with {@code value} specifying the number of
         * occurances of {@code row} with {@code col}.   This is
         * approximated based on the occurance counts for each {@code row} and
         * {@code col}.
         *
         * @param row The index specifying the row being observed
         * @param col The index specifying the col being observed
         * @param value The number of ocurrances of row and col together
         *
         * @return log(value * matrixSum / (rowSum[row] * colSum[col]))
         */
        public double transform(int row, int col, double value) {
            return Math.log(value * matrixSum /
                            (rowCounts[row] * colCounts[col] ));
        }
    }
}
