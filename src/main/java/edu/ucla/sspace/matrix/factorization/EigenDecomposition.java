package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;

import org.netlib.lapack.LAPACK;
import org.netlib.util.intW;


/**
 * @author Keith Stevens
 */
public class EigenDecomposition implements MatrixFactorization {

    private static final String VECTOR_AND_VALUE = "V";
    private static final String VALUE_RANGE = "I";
    private static final String UPPER_TRIANGULAR = "U";

    private Matrix dataClasses;

    private Matrix classFeatures;

    public void factorize(MatrixFile m, int numDimensions) {
        factorize((SparseMatrix) m.load(), numDimensions);
    }

    public void factorize(SparseMatrix sm, int numDimensions) {
        Matrix m = sm;
        factorize(m, numDimensions);
    }

    public void factorize(Matrix m, int numDimensions) {
        LAPACK l = LAPACK.getInstance();
        // Create the data matrix to pass to LAPACK.  This must be a single on
        // dimensional matrix with all values.
        int n = m.rows();
        double[] data = new double[n*n];
        for (int r = 0; r < n; ++r)
            for (int c = 0; c < n; ++c)
                data[r*n+c] = m.get(r,c);

        // Create output integer objects.
        intW M = new intW(0);
        intW info = new intW(0);

        // Create the output space for the eigen values and the eigen vectors.
        double[] eigenValues = new double[n];
        double[] eigenVectors = new double[n*numDimensions];

        // Create a workspace for LAPACK.
        int lwork = 8*n;
        double[] work = new double[lwork];
        int[] iwork = new int[5*n];
        int[] ifail = new int[n];

        // Create the lower and upper bounds on the eigen value/vectors we want
        // to compute.  The nth value is the largest value and the 1st is the
        // smallest value.
        int lower = n-numDimensions+1;
        int upper = n;
        l.dsyevx(
                VECTOR_AND_VALUE, VALUE_RANGE, UPPER_TRIANGULAR, // Parameters
                n, data, n, // Input data and size
                lower, upper, lower, upper, // ranges
                0, M, // error parameters and output
                eigenValues, eigenVectors, n, // Vector/Value output
                work, lwork, iwork, ifail, info); // Extra stuff
        dataClasses = new ArrayMatrix(1, n, eigenValues);
        classFeatures = new ArrayMatrix(numDimensions, n, eigenVectors);
    }

    public MatrixBuilder getBuilder() {
        return new MatlabSparseMatrixBuilder();
    }

    public Matrix dataClasses() {
        return dataClasses;
    }

    public Matrix classFeatures() { 
        return classFeatures;
    }
}
