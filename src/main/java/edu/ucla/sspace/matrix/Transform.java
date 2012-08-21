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

import edu.ucla.sspace.vector.DoubleVector;

import java.io.File;
import java.io.IOException;


/**
 * An interface for {@link Matrix} transformations.  These tranformations should
 * support both a {@link Matrix} and a serialized {@link Matrix} stored in a
 * {@code File}, in one of the supported matrix formats.  Implementations are
 * strongly encouraged to implement the {@code toString} method, as many {@link
 * edu.ucla.sspace.common.SemanticSpace} implementations will use the name of
 * the applied transform for describing their state.
 *
 * @author David Jurgens
 */
public interface Transform {

    /**
     * Once a matrix has been transformed, use the existing state of this
     * transform to apply the same transformate to this new column vector, as if
     * it had been a part of the input matrix, but without changing this
     * transform's state.
     *
     * <p> This method is intended for use cases after when the statistics of a
     * matrix are calcuated and then used to transform that matrix, but where
     * new vector data could be transformed by the same procedure.  For example,
     * a {@link TfIdfTransform} could be applied to a term document space to
     * create the transform state necessary for calculating the {@code idf};
     * then, the same weighting could be applied to a new document.
     *
     * @throws IllegalStateException if this {@code Transform} instances does
     *         not yet have sufficient state to transform a new document.  That
     *         is, the {@code transform} method has yet to be called.
     */
    DoubleVector transform(DoubleVector column);

    /**
     * Transforms the matrix in the file using the an implemented method and
     * returns a temporary file containing the result.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the matrix
     *
     * @return a file with the transformed version of the input.  This file is
     *         marked to be deleted when the JVM exits.
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    File transform(File inputMatrixFile, 
                   MatrixIO.Format format) throws IOException;

    /**
     * Transforms the input matrix using the implemented method and writes the
     * result to the file for the output matrix.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param inputFormat the format of the input matrix, and the format in
     *        which the output matrix will be written
     * @param outputMatrixFile the file to which the transformed matrix will be
     *        written
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    void transform(File inputMatrixFile, MatrixIO.Format inputFormat, 
                   File outputMatrixFile) throws IOException;

    /**
     * Returns a transformed matrix based on the given matrix.  By default,
     * algorithms will store the transformed values in place.  Since a few
     * algorithms cannot be done in place, this is optional.  (optional
     * operation).
     *
     * @param input the matrix to be transformed
     *
     * @return a pointer to {@code input} after it has been transformed
     */
    Matrix transform(Matrix input);

    /**
     * Returns a transformed matrix based on the given matrix.
     *
     * @param input the matrix to be transformed
     * @param output values transformed from {@code input} will be stored in
     *        this matrix
     *
     * @return {@code output}, the transformed version of the input matrix
     */
    Matrix transform(Matrix input, Matrix output); 
}
