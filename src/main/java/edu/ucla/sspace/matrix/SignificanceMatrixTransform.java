package edu.ucla.sspace.matrix;

import edu.ucla.sspace.common.statistics.SignificanceTest;
import edu.ucla.sspace.matrix.TransformStatistics.MatrixStatistics;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import java.io.File;


/**
 * Computes a {@link Transform} a {@link matrix} based on a {@link
 * SignificanceTest}.  This {@link SignificanceMatrixTransform} will extract
 * sufficient statistics such as the row sums, colum sums, and total sums for
 * the matrix and pass in the correct values for each non-zero entry in the
 * matrix to the {@link SignificanceTest} instance passed in to the constructor.
 * This approach assumes that all {@link SignificanceTest} methods used will
 * return {@code 0} for events that never happen.  
 *
 * @author Keith Stevens
 */
public class SignificanceMatrixTransform extends BaseTransform {

    private final SignificanceTest tester;

    public SignificanceMatrixTransform(SignificanceTest tester) {
        this.tester = tester;
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new SignificanceTestGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new SignificanceTestGlobalTransform(inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return tester.toString();
    }

    public class SignificanceTestGlobalTransform
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
        private double totalCount;

        /**
         * Creates an instance of {@code SignificanceTestTransform}
         * from a given {@link Matrix}.
         */
        public SignificanceTestGlobalTransform(Matrix matrix) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(matrix);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            totalCount = stats.matrixSum;
        }

        /**
         * Creates an instance of {@code SignificanceTestTransform}
         * from a matrix {@code File} of format {@code format}.
         */
        public SignificanceTestGlobalTransform(
                File inputMatrixFile,
                MatrixIO.Format format) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(inputMatrixFile, format);
            rowCounts = stats.rowSums;
            colCounts = stats.columnSums;
            totalCount = stats.matrixSum;
        }

        /**
         * Computes the significance value between the {@code row} and {@code
         * col} with {@code value} specifying the number of occurances of {@code
         * row} with {@code col}.   
         *
         * @param row The index specifying the row being observed
         * @param col The index specifying the col being observed
         * @param value The number of ocurrances of row and col together
         *
         * @return The value returned by {@link tester} for this event.
         */
        public double transform(int row, int col, double value) {
            return tester.score((int) value,  // Both events
                                (int) rowCounts[row],  // Just event 1
                                (int) colCounts[col],  // Just event 2
                                (int) (totalCount - rowCounts[row] - colCounts[col])); // Neither event
        }
    }
}
