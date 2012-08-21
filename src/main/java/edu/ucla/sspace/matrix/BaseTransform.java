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

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vectors;

import java.io.File;
import java.io.IOException;


/**
 * An abstract {@link Transform} implemenation that most transforms can extend.
 * Any transform that can be implemented as a {@link GlobalTransform} can simply
 * define the a {@link GlobalTransform} and then subclass this abstract class to
 * adhere to the standard {@link Transform} interface.
 */
public abstract class BaseTransform 
        implements Transform, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private GlobalTransform transform;

    public BaseTransform() {
        transform = null;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector transform(DoubleVector column) {
        if (transform == null)
            throw new IllegalStateException(
                "the initial matrix has not been transformed yet");
        // Create a new instance of that vector's type, which will contain the
        // updated values
        DoubleVector transformed = Vectors.instanceOf(column);
        int length = column.length();
        for (int row = 0; row < length; ++row) {
            double newValue = transform.transform(row, column);
            transformed.set(row, newValue);
        }
        return transformed;
    }

    /**
     * {@inheritDoc}
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
             throws IOException {
        // create a temp file for the output
        File output = File.createTempFile(
                inputMatrixFile.getName() + ".matrix-transform", ".dat");
        transform(inputMatrixFile, format, output);
        return output;
    }

    /**
     * {@inheritDoc}
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        transform = getTransform(inputMatrixFile, format);
        FileTransformer transformer = MatrixIO.fileTransformer(format);
        transformer.transform(inputMatrixFile, outputMatrixFile, transform);
    }
    
    /**
     * {@inheritDoc}
     */
    public Matrix transform(Matrix matrix) {
        return transform(matrix, matrix);
    }

    /**
     * {@inheritDoc}
     */
    public Matrix transform(Matrix matrix, Matrix transformed) {
        // Reject any transforms on matrices that do not have the same
        // sizes.
        if (matrix.rows() != transformed.rows() ||
            matrix.columns() != transformed.columns())
            throw new IllegalArgumentException(
                    "Dimensions of the transformed matrix must match the " +
                    "input matrix");

        transform = getTransform(matrix);

        if (matrix instanceof SparseMatrix) {
            SparseMatrix smatrix = (SparseMatrix) matrix;

            // Transform a sparse matrix by only iterating over the non zero
            // values in each row.
            for (int row = 0; row < matrix.rows(); ++row) {
                SparseDoubleVector rowVec = smatrix.getRowVector(row);
                for (int col : rowVec.getNonZeroIndices()) {
                    double newValue = 
                            transform.transform(row, col, rowVec.get(col));
                    transformed.set(row, col, newValue);
                }
            }
        } else {
            // Transform dense matrices by inspecting each value in the matrix
            // and having it transformed.
            for (int row = 0; row < matrix.rows(); ++row) {
                for (int col = 0; col < matrix.columns(); ++col) {
                    double oldValue = matrix.get(row, col);
                    if (oldValue != 0d) {
                        double newValue = 
                            transform.transform(row, col, oldValue);
                        transformed.set(row, col, newValue);
                    }
                }
            }
        }

        return transformed;
    }

    /**
     * Returns a {@link GlobalTransform} for a {@link Matrix}.
     */
    protected abstract GlobalTransform getTransform(Matrix matrix);

    /**
     * Returns a {@link GlobalTransform} for a File of the given format.
     */
    protected abstract GlobalTransform getTransform(File inputMatrixFile,
                                                    MatrixIO.Format format);
}
