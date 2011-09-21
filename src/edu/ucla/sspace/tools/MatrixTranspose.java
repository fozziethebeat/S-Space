package edu.ucla.sspace.tools;

import edu.ucla.sspace.matrix.*;

import java.io.*;

/**
 * @author Keith Stevens
 */
public class MatrixTranspose {
    public static void main(String[] args) throws Exception {
        Matrix m = MatrixIO.readMatrix(new File(args[0]), MatrixIO.Format.DENSE_TEXT);
        m = Matrices.transpose(m);
        File out = new File(args[1]);
        MatrixIO.writeMatrix(m, out, MatrixIO.Format.DENSE_TEXT);
    }

}
