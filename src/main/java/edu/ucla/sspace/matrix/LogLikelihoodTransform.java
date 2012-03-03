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
 * Transforms a matrix using the log-likelihood weight.  The input matrix is
 * assumed to have non-negative values and be formatted as rows representing
 * terms and columns representing terms.  Each matrix cell indicates the number
 * of times the row's word occurs within the some range of the column's word.
 * Although the log likelihood typically requires much more than this, an
 * estimation is used that utilizes only the occurrence frequency counts based.
 * See the following papers for details and analysis:
 *
 * </li style="font-family:Garamond, Georgia, serif"> Pado, S. and Lapata, M.
 * (2007) Dependnecy-Based COnstruction of Semantic Space Models.
 * <i>Association of Computational Linguistics</i>, <b>33</b>.
 
 * @author Keith Stevens
 */
public class LogLikelihoodTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new LogLikelihoodGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new LogLikelihoodGlobalTransform(
                inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "LogLikelihood";
    }

    public class LogLikelihoodGlobalTransform
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
         * The total sum of all values in the matrix.
         */
        private double matrixSum;

        /**
         * Creates an instance of {@code LogLikelihoodTransform} from a given
         * {@link Matrix}.
         */
        public LogLikelihoodGlobalTransform(Matrix matrix) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(matrix);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            matrixSum = stats.matrixSum;
        }

        /**
         * Creates an instance of {@code LogLikelihoodTransform}
         * from a matrix {@code File} of format {@code format}.
         */
        public LogLikelihoodGlobalTransform(
                File inputMatrixFile,
                MatrixIO.Format format) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(inputMatrixFile, format);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            matrixSum = stats.matrixSum;
        }

        /**
         * Computes the Log Likelihood information between the {@code row}
         * and {@code col} with {@code value} specifying the number of
         * occurances of {@code row} with {@code col}.   This is
         * approximated based on the occurance counts for each {@code row} and
         * {@code col}.
         *
         * @param row The index specifying the row being observed
         * @param col The index specifying the col being observed
         * @param value The number of ocurrances of row and col together
         */
        public double transform(int row, int col, double value) {
            double l = colCounts[col] - value;
            double m = rowCounts[row] - value;
            double n = matrixSum - (value + l + m);
            double likelihood = value * Math.log(value) + l * Math.log(l) +
                                m * Math.log(m) + n * Math.log(n);
            likelihood -= ((value + l) * Math.log(value+l) - 
                           (value + m) * Math.log(value+m));
            likelihood -= ((l + n) * Math.log(l + n) - 
                           (m + n) * Math.log(m + n));
            likelihood += ((value + l + m + n) * Math.log(value + l + m + n));
            return 2 * likelihood;
        }
    }
}

