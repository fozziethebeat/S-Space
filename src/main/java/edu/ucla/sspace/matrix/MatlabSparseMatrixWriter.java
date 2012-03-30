package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.OutputStream;
import java.io.PrintStream;


/**
 * @author Keith Stevens
 */
public class MatlabSparseMatrixWriter implements MatrixWriter {

    /**
     * Writes the dense {@link Matrix} to the file using a particular format.
     */
    public void writeMatrix(Matrix m, OutputStream s) {
        // If the matrix is actually sparse, let the sparse method handle it.
        if (m instanceof SparseMatrix) {
            writeMatrix((SparseMatrix) m, s);
            return;
        }

        PrintStream p = new PrintStream(s);

        // Print the row, col, value entrie for each element in the matrix.
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                p.printf("%d %d %f\n", r+1, c+1, m.get(r,c));

        p.flush();
        p.close();
    }

    /**
     * Writes the {@link SparseMatrix} to the file using a particular format.
     */
    public void writeMatrix(SparseMatrix m, OutputStream s) {
        PrintStream p = new PrintStream(s);

        // Check to see if the last element in the matrix is non zero.  If it
        // has a zero value, print out a single dummy value to bound the total
        // size of the matrix.
        if (m.get(m.rows()-1, m.columns()-1) == 0d)
            p.printf("%d %d %f\n", m.rows(), m.columns(), 0.0);

        // Print the row, col, value entrie for each element in the matrix.
        for (int r = 0; r < m.rows(); ++r) {
            SparseDoubleVector v = m.getRowVector(r);
            for (int c : v.getNonZeroIndices())
                p.printf("%d %d %f\n", r+1, c+1, m.get(r,c));
        }

        p.flush();
        p.close();
    }
}
