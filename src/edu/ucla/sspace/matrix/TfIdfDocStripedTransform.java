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
 * Tranforms a matrix according to the <a
 * href="http://en.wikipedia.org/wiki/Tf%E2%80%93idf">Term frequency-Inverse
 * Document Frequency</a> weighting.  The input matrix is assumed to be
 * formatted as rows representing documents and columns representing rows.  Each
 * matrix cell indicates the number of times the columns's word occurs within
 * the rows's document.  For full details see:
 *
 * <ul><li style="font-family:Garamond, Georgia, serif">Spärck Jones, Karen
 *      (1972). "A statistical interpretation of term specificity and its
 *      application in retrieval". <i>Journal of Documentation</i> <b>28</b>
 *      (1): 11–21.</li></ul>
 *
 * @see TfIdfTransform
 *
 * @author Keith Stevens
 */
public class TfIdfDocStripedTransform extends BaseTransform {

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new TfIdfGlobalTransform(matrix);
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File inputMatrixFile,
                                           MatrixIO.Format format) {
        return new TfIdfGlobalTransform(inputMatrixFile, format);
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "TF-IDF-DOC-STRIPED";
    }

    public class TfIdfGlobalTransform implements GlobalTransform {

        /**
         * The total number of terms that occur in each document.
         */
        private double[] docTermCount;

        /**
         * The total number of documents that each term occurs in.
         */
        private double[] termDocCount;

        /**
         * The total number of documents present in the matrix.
         */
        private int totalDocCount;

        /**
         * Creates an instance of {@code TfIdfGlobalTransform} from a {@link
         * Matrix}.
         */
        public TfIdfGlobalTransform(Matrix matrix) {
            MatrixStatistics stats =
                TransformStatistics.extractStatistics(matrix, false, true);
            docTermCount = stats.rowSums;
            termDocCount = stats.columnSums;
            totalDocCount = docTermCount.length;
        }
        
        /**
         * Creates an instance of {@code TfIdfGlobalTransform} from a {@code
         * File} in the format {@link Format}.
         */
        public TfIdfGlobalTransform(File inputMatrixFile, Format format) {
            MatrixStatistics stats = TransformStatistics.extractStatistics(
                    inputMatrixFile, format, false, true);
            docTermCount = stats.rowSums;
            termDocCount = stats.columnSums;
            totalDocCount = docTermCount.length;
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
            double tf = value / docTermCount[row];
            double idf =
                Math.log(totalDocCount / (1 + termDocCount[column]));
            return tf * idf;
        }
    }
}
