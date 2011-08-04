/*
 * Copyright 2009 Keith Stevens 
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

import java.io.File;
import java.io.IOException;

import java.util.Properties;

import java.util.logging.Logger;


/**
 * Transforms a matrix using row correlation weighting.  The input matrix is assumed
 * to be formatted as rows representing terms and columns representing
 * co-occuring terms.  Each matrix cell indicates the number of times the row's word
 * occurs the other term.  See the following paper for details
 * and analysis:
 *
 * <p style="font-family:Garamond, Georgia, serif"> Rohde, D. L. T., Gonnerman,
 * L. M., Plaut, D. C. (2005).  An Improved Model of Semantic Similarity Based
 * on Lexical Co-Occurrence. <i>Cognitive Science</i> <b>(submitted)</b>.
 * Available <a
 * href="http://www.cnbc.cmu.edu/~plaut/papers/pdf/RohdeGonnermanPlautSUB-CogSci.COALS.pdf">here</a></p>
 *
 * @author Keith Stevens
 */
public class CorrelationTransform implements Transform {

    /**
     * A property prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.matrix.CorrelationTransform";

    /**
     * Specifies if negative values should be stored after the co-occurance
     * matrix has been normalized by correlation.
     */
    public static final String SAVE_NEGATIVES_PROPERTY =
        PROPERTY_PREFIX + ".saveNegatives";

    /**
     * Specifies if values should be replaced with their square root after the
     * co-occurance matrix has been normalized by correlation.
     */
    public static final String USE_SQUARE_ROOT_PROPERTY =
        PROPERTY_PREFIX + ".useSquareRoot";

    /**
     * A logger to keep track of progress.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(CorrelationTransform.class.getName());

    /**
     * Specifies if negative values should be kept.
     */
    boolean saveNegatives;

    /**
     * Specifies if co-occurrance values should be reduced to their square root.
     */
    boolean useSquareRoot;

    /**
     * Creates a new {@link CorrelationTransform} using {@code System}
     * properties.
     */
    public CorrelationTransform() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@link CorrelationTransform} using the provided {@code
     * Properties}.
     */
    public CorrelationTransform(Properties props) {
        saveNegatives = props.getProperty(SAVE_NEGATIVES_PROPERTY) != null;
        useSquareRoot = props.getProperty(USE_SQUARE_ROOT_PROPERTY) != null;
    }

    /**
     * Transform the matrix in place using row correlation.  This is done
     * according to correlation equation provided by the cited COALS paper on
     * page 3 in table 4.  By default, negative values are dropped after the
     * correlation has been computed, if {@code SAVE_NEGATIVES_PROPERTY} was
     * set, then the negative values are maintained.  If negative values are
     * dropped then by default, after the correlation is done all values are
     * replaced with their square root.
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
             throws IOException {
        // create a temp file for the output
        File output = File.createTempFile(inputMatrixFile.getName() + 
                                          ".correlation-transform", ".dat");
        transform(inputMatrixFile, format, output);
        return output;
    }

    /**
     * Transform the matrix in place using row correlation.  This is done
     * according to correlation equation provided by the cited COALS paper on
     * page 3 in table 4.  By default, negative values are dropped after the
     * correlation has been computed, if {@code SAVE_NEGATIVES_PROPERTY} was
     * set, then the negative values are maintained.  If negative values are
     * dropped then by default, after the correlation is done all values are
     * replaced with their square root.
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        Matrix m = MatrixIO.readMatrix(inputMatrixFile, format);
        transform(m);
        MatrixIO.writeMatrix(m, outputMatrixFile, format);
    }

    /**
     * Transform the matrix in place using row correlation.  This is done
     * according to correlation equation provided by the cited COALS paper on
     * page 3 in table 4.  By default, negative values are dropped after the
     * correlation has been computed, if {@code SAVE_NEGATIVES_PROPERTY} was
     * set, then the negative values are maintained.  If negative values are
     * dropped then by default, after the correlation is done all values are
     * replaced with their square root.
     *
     * @param matrix The {@link Matrix} to modify in place.
     *
     * @return {@code matrix}
     */
    public Matrix transform(Matrix matrix) {
        double totalSum = 0;

        // Generate the total value in each row and column.
        double[] rowSums = new double[matrix.rows()];
        double[] colSums = new double[matrix.columns()];
        for (int i = 0; i < matrix.rows(); ++i) {
            for (int j = 0; j < matrix.columns(); ++j) {
                totalSum += matrix.get(i,j);
                colSums[j] += matrix.get(i,j);
                rowSums[i] += matrix.get(i,j);
            }
        }

        // Use the row and column totals to compute the correlation.
        for (int i = 0; i < matrix.rows(); ++i) {
            for (int j = 0; j< matrix.columns(); ++j) {
                double newVal =
                    (totalSum * matrix.get(i,j) - rowSums[i] * colSums[j]) /
                    Math.sqrt(rowSums[i] * (totalSum - rowSums[i]) *
                              colSums[j] * (totalSum - colSums[j]));

                // Store the computed value.
                if (saveNegatives)
                    matrix.set(i,j, newVal);
                else {
                    newVal = (newVal > 0) ? newVal : 0;
                    newVal = (useSquareRoot) ? Math.sqrt(newVal) : newVal;
                    matrix.set(i,j, newVal);
                }
            }
        }
        return matrix;
    }
}
