/*
 * Copyright 2011 Keith Stevens 
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
 * Tranforms a matrix by the row magnitudes.  After this transform, each row
 * will have a magnitude of 1.
 *
 * @author Keith Stevens
 */
public class RowMagnitudeTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new RowMagnitudeGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new RowMagnitudeGlobalTransform(inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "TF-IDF";
    }

    public class RowMagnitudeGlobalTransform implements GlobalTransform {

        /**
         * The row magnitudes.
         */
        private double[] rowMagnitudes;

        /**
         * Creates an instance of {@code RowMagnitudeGlobalTransform} from a
         * {@link Matrix}.
         */
        public RowMagnitudeGlobalTransform(Matrix matrix) {
            rowMagnitudes = new double[matrix.rows()];
            for (int r = 0; r < matrix.rows(); ++r)
                rowMagnitudes[r] = matrix.getRowVector(r).magnitude();
        }
        
        /**
         * Creates an instance of {@code RowMagnitudeGlobalTransform} from a
         * {@code File} in the format {@link Format}.
         */
        public RowMagnitudeGlobalTransform(File inputMatrixFile,
                                           Format format) {
        }

        /**
         * Computes the Term Frequency-Inverse Document Frequency for a given
         * value where {@code value} is the observed frequency of term {@code
         * row} in document {@code column}.
         *
         * @param row The index speicifying the term being observed
         * @param column The index specifying the document being observed
         * @param value The number of occurances of the term in the document.
         *
         * @return the TF-IDF of the observed value
         */
        public double transform(int row, int column, double value) {
            return value / rowMagnitudes[row];
        }
    }
}
