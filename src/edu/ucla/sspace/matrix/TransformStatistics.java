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

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.IOError;
import java.io.IOException;
import java.io.File;

import java.util.Iterator;
import java.util.Map;


/**
 * A static utility class used for gather statistics that are frequently used in
 * matrix {@link Transform} implementations.  Given a {@link Matrix} or a {@link
 * Matrix} file, this class will gather row summations, column summations, and
 * the total summation of the matrix.  Optionally, when gathering either the row
 * or column summations, the number of non zero values in the row or column can
 * be counted instead of a full summation, which is needed for the {@link
 * TfIdfTransform}.  
 *
 * @author Keith Stevens
 */
public class TransformStatistics {

    /**
     * Extracts the full row, column, and matrix summations based on entries in
     * the given {@link Matrix}.
     *
     * @param matrix a {@link Matrix to sum over}
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(Matrix matrix) {
        return extractStatistics(matrix, false, false);
    }

    /**
     * Extracts the row, column, and matrix summations based on entries in
     * the given {@link Matrix}.  If {@code countRowOccurrances} is true, the
     * number of non zeros in each row will be counted for the row summation.
     * If {@code countColumnOccurrances} is true, the same will be done for the
     * columns.  In either case, the matrix summation will remain the same.
     *
     * @param matrix a {@link Matrix} to sum over
     * @param countRowOccurrances true if the row summation should only count
     *        the number of non zero values in a row
     * @param countColumnOccurrances true if the column summation should only
     *        count the number of non zero values in a column 
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(
            Matrix matrix,
            boolean countRowOccurrances,
            boolean countColumnOccurrances) {
        // Initialize the statistics.
        double[] rowSums = new double[matrix.rows()];
        double[] columnSums = new double[matrix.columns()];
        double matrixSum = 0;

        if (matrix instanceof SparseMatrix) {
            // Special case for sparse matrices so that only non zero values
            // are traversed.
            SparseMatrix smatrix = (SparseMatrix) matrix;

            // Compute the col and row sums. 
            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                int[] nonZeros = rowVec.getNonZeroIndices();
                for (int index : nonZeros) {
                    double value = rowVec.get(index);
                    rowSums[row] += (countRowOccurrances) ? 1 : value;
                    columnSums[index] += (countColumnOccurrances) ? 1 : value;
                    matrixSum += value;
                }
            }
        } else {
            // Compute the col and row sums by iterating over all
            // values in the matrix.
            for (int row = 0; row < matrix.rows(); ++row) {
                for (int col = 0; col < matrix.columns(); ++col) {
                    double value = matrix.get(row, col);
                    rowSums[row] += (countRowOccurrances) ? 1 : value;
                    columnSums[col] += (countColumnOccurrances) ? 1 : value;
                    matrixSum += value;
                }
            }
        }
        return new MatrixStatistics(rowSums, columnSums, matrixSum);
    }

    /**
     * Extracts the full row, column, and matrix summations based on entries in
     * the given {@link Matrix} file.
     *
     * @param inputMatrixFfile a {@link Matrix} file  to sum over
     * @param format the matrix {@link Format} of {@code inputMatrixFile}
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(
            File inputMatrixFile, Format format) {
        return extractStatistics(inputMatrixFile, format, false, false);
    }

    /**
     * Extracts the row, column, and matrix summations based on entries in
     * the given {@link Matrix}.  If {@code countRowOccurrances} is true, the
     * number of non zeros in each row will be counted for the row summation.
     * If {@code countColumnOccurrances} is true, the same will be done for the
     * columns.  In either case, the matrix summation will remain the same.
     *
     * @param inputMatrixFfile a {@link Matrix} file  to sum over
     * @param format the matrix {@link Format} of {@code inputMatrixFile}
     * @param countRowOccurrances true if the row summation should only count
     *        the number of non zero values in a row
     * @param countColumnOccurrances true if the column summation should only
     *        count the number of non zero values in a column 
     * @return a {@link MatrixStatistics} instance containing the summations
     */
    public static MatrixStatistics extractStatistics(
            File inputMatrixFile,
            Format format,
            boolean countRowOccurrances,
            boolean countColumnOccurrances) {
        // Initialize the statistics.
        int numColumns = 0;
        int numRows = 0;
        double matrixSum = 0;
        Map<Integer, Double> rowCountMap = new IntegerMap<Double>();
        Map<Integer, Double> colCountMap = new IntegerMap<Double>();

        // Get an iterator for the matrix file.
        Iterator<MatrixEntry> iter;
        try {
            iter = MatrixIO.getMatrixFileIterator(inputMatrixFile, format);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        while (iter.hasNext()) {
            MatrixEntry entry = iter.next();

            // Get the total number of columns and rows.
            if (entry.column() >= numColumns)
                numColumns = entry.column() + 1;
            if (entry.row() >= numRows)
                numRows = entry.row() + 1;

            // Skip non zero entries.
            if (entry.value() == 0d)
                continue;

            // Gather the row sums.
            Double occurance = rowCountMap.get(entry.row());
            double rowDelta = (countRowOccurrances) ? 1 : entry.value();
            rowCountMap.put(entry.row(), (occurance == null)
                    ? rowDelta
                    : occurance + rowDelta);

            // Gather the column sums.
            occurance = colCountMap.get(entry.column());
            double columnDelta = (countColumnOccurrances) ? 1 : entry.value();
            colCountMap.put(entry.column(), (occurance == null)
                    ? columnDelta
                    : occurance + columnDelta);

            matrixSum += entry.value();
        }

        // Convert the maps to arrays.
        double[] rowSums = extractValues(rowCountMap, numRows);
        double[] columnSums = extractValues(colCountMap, numColumns);
        return new MatrixStatistics(rowSums, columnSums, matrixSum);
    }

    /**
     * Extracts the values from the given map into an array form.  This is
     * neccesary since {@code toArray} on a {@link IntegerMap} does not work
     * with primitives and {@code Map} does not provide this functionality.
     * Each key in the map corresponds to an index in the array being
     * created and the value is the value in stored at the specified index.
     */
    private static <T extends Number> double[] extractValues(
            Map<Integer, T> map, int size)  {
        double[] values = new double[size];
        for (Map.Entry<Integer, T> entry : map.entrySet()) {
            if (entry.getKey() > values.length)
                throw new IllegalArgumentException(
                        "Array size is too small for values in the " +
                        "given map");
            values[entry.getKey()] = entry.getValue().doubleValue();
        }
        return values;
    }
    
    /**
     * A struct recording the row, column, and matrix summations as doubles.
     */
    public static class MatrixStatistics {
        public double[] rowSums;
        public double[] columnSums;
        public double matrixSum;

        /**
         * Creates a new {@link MatrixStatistics} instance using the given
         * double values.
         */
        public MatrixStatistics(double[] rowSums,
                                double[] columnSums,
                                double matrixSum) {
            this.rowSums = rowSums;
            this.columnSums = columnSums;
            this.matrixSum = matrixSum;
        }
    }
}
