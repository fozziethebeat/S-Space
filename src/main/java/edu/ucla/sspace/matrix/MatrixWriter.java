package edu.ucla.sspace.matrix;

import java.io.OutputStream;


/**
 * An interface for writing complete matrices to files.  Each implementation of
 * {@link MatrixWriter} should correspond to a particular matrix format and
 * attempt to optimize writing for Dense {@link Matrix} objects and {@link
 * SparseMatrix} objects.
 *
 * @author Keith Stevens
 */
public interface MatrixWriter {

    /**
     * Writes the dense {@link Matrix} to the file using a particular format.
     */
    void writeMatrix(Matrix m, OutputStream f);

    /**
     * Writes the {@link SparseMatrix} to the file using a particular format.
     */
    void writeMatrix(SparseMatrix m, OutputStream f);
}
