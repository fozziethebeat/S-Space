/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.IOError;
import java.io.IOException;
import java.io.File;

import java.util.Iterator;
import java.util.Map;

import java.util.logging.Logger;

import static edu.ucla.sspace.common.Statistics.log2;
import static edu.ucla.sspace.common.Statistics.log2_1p;


/**
 * Transforms a matrix using log-entropy weighting.  The input matrix is assumed
 * to be formatted as rows representing terms and columns representing
 * documents.  Each matrix cell indicates the number of times the row's word
 * occurs within the column's document.  See the following papers for details
 * and analysis:
 *
 * <ul> 
 *
 * <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., Foltz,
 *      P. W., & Laham, D. (1998).  Introduction to Latent Semantic
 *      Analysis. <i>Discourse Processes</i>, <b>25</b>, 259-284.</li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> S. Dumais, “Enhancing
 *      performance in latent semantic indexing (LSI) retrieval,” Bellcore,
 *      Morristown (now Telcordia Technologies), Tech. Rep. TM-ARH-017527,
 *      1990. </li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> P. Nakov, A. Popova, and
 *      P. Mateev, “Weight functions impact on LSA performance,” in
 *      <i>Proceedings of the EuroConference Recent Advances in Natural Language
 *      Processing, (RANLP’01)</i>, 2001, pp. 187–193. </li>
 *
 * </ul>
 *
 * @author David Jurgens
 */
public class LogEntropyTransform extends BaseTransform {

    /**
     * The logger for reporting the status of the transformation.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(LogEntropyTransform.class.getName());

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new LogEntropyGlobalTransform(inputMatrixFile, format);
    }
    
    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new LogEntropyGlobalTransform(matrix);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "log-entropy";
    }

    /**
     * The real implementation of the Log Entropy transformation as a {@link
     * GlobalTransform}
     */
    public class LogEntropyGlobalTransform implements GlobalTransform {

        /**
         * The entropy for every row.
         */
        private double[] rowEntropy;

        /**
         * Creates an instance of {@code LogEntropyGlobalTransform} from a
         * {@link Matrix}.
         */
        public LogEntropyGlobalTransform(Matrix matrix) {
            rowEntropy = new double[matrix.rows()];

            int numColumns = matrix.columns();
            if (matrix instanceof SparseMatrix) {
                // Special case for sparse matrices.
                SparseMatrix smatrix = (SparseMatrix) matrix;

                // Compute the entropy for each row.
                for (int row = 0; row < matrix.rows(); ++row) {
                    // Compute the total count for each row.
                    double rowCount = 0;
                    SparseDoubleVector rowVec = smatrix.getRowVector(row);
                    int[] nonZeros = rowVec.getNonZeroIndices();
                    for (int index : nonZeros) {
                        double value = rowVec.get(index);
                        rowCount += value;
                    }

                    // Compute the entropy of each row based on the occurances
                    // of each row.
                    for (int index : nonZeros) {
                        double value = rowVec.get(index);
                        double rowProbabilityForFeature = value / rowCount;
                        rowEntropy[row] += rowProbabilityForFeature *
                                           log2(rowProbabilityForFeature);
                    }

                    // Scale the entropy by the log of the number of columns.
                    rowEntropy[row] = 1 + (rowEntropy[row] / log2(numColumns));
                }
            } else {
                // The standard case for dense matrices.

                // Compute the entropy for each row.
                for (int row = 0; row < matrix.rows(); ++row) {
                    // Compute the total count for each row.
                    double rowCount = 0;
                    for (int column = 0; column < matrix.columns(); ++column)
                        rowCount += matrix.get(row, column);

                    // Compute the entropy sum of each row based on the
                    // occurances of each row.
                    for (int column = 0; column < matrix.columns(); ++column) {
                        double value = matrix.get(row, column);
                        double rowProbabilityForFeature = value / rowCount; 
                        rowEntropy[row] += rowProbabilityForFeature *
                                           log2(rowProbabilityForFeature);
                    }

                    // Scale the entropy by the log of the number of columns.
                    rowEntropy[row] = 1 + (rowEntropy[row] / log2(numColumns));
                }
            }
        }

        /**
         * Creates an instance of {@code LogEntropyGlobalTransform} from a
         * {@link File} of format {@link Format}.
         */
        public LogEntropyGlobalTransform(File inputMatrixFile,
                                         MatrixIO.Format format) {
            // Get the row sums.
            Map<Integer, Double> rowSums = new IntegerMap<Double>();
            Iterator<MatrixEntry> iter;
            try {
                iter = MatrixIO.getMatrixFileIterator(inputMatrixFile, format);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            int numColumns = 0;
            int numRows = 0;

            // Compute the total count for each row.
            while (iter.hasNext()) {
                MatrixEntry entry = iter.next();
                Double rowSum = rowSums.get(entry.row());
                rowSums.put(entry.row(), (rowSum == null) 
                        ? entry.value()
                        : rowSum + entry.value());

                // Compute the total number of rows and columns.
                if (entry.row() >= numRows)
                    numRows = entry.row() + 1;
                if (entry.column() >= numColumns)
                    numColumns = entry.column() + 1;
            }

            // Compute the entropy sum of each row based on the occurances
            // of each row.
            rowEntropy = new double[numRows];
            try {
                iter = MatrixIO.getMatrixFileIterator(inputMatrixFile, format);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            while (iter.hasNext()) {
                MatrixEntry entry = iter.next();
                Double rowSumDouble = rowSums.get(entry.row());
                double rowSum = (rowSumDouble == null) ? 0 : rowSumDouble;
                double probability = entry.value() / rowSum;
                rowEntropy[entry.row()] += probability * log2(probability);
            }

            // Scale the entropy by the log of the number of columns.
            for (int row = 0; row < numRows; ++row)
                rowEntropy[row] = 1 + (rowEntropy[row] / log2(numColumns));
        }

        /**
         * Calculates the entropy (information gain) where {@code value} is the
         * number of occurances of item {@code row} with feature {@code column}.
         * The item entropy is defined as:
         *
         * </p>   1 + entropy(item) / log(numberOfFeatures)
         * </p>
         * with entropy defined as:
         * </p>  sum_features(p(item, feature) * log(p(item, feature)))
         *
         * @param row The index specifying the observed item
         * @param column The index specifying the observed feature 
         * @param value The number occurances of the item and the feature
         *
         * @return log(value) * item_entropy(row)
         */
        public double transform(int row, int column, double value) {
            return log2_1p(value) * rowEntropy[row];
        }
    }
}
