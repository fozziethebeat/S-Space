package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SvdlibjDriver;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;

import java.io.IOError;
import java.io.IOException;

import java.util.logging.Logger;


/**
 * A wrapper around the {@link SvdlibjDriver} that implements the {@link
 * MatrixFactorization} interface.
 *
 * @author Keith Stevens
 */
public class SingularValueDecompositionLibJ implements MatrixFactorization {

    private static final Logger LOG = 
        Logger.getLogger(SingularValueDecompositionLibJ.class.getName());

    /**
     * The class by feature type matrix.
     */
    private Matrix classFeatures;

    /**
     * Set to true when {@code classFeatures} has been accessed the first time
     * to mark that the singular values have been applied to each value in the
     * matrix.
     */
    private boolean scaledClassFeatures;

    /**
     * The data point by class matrix.
     */
    private Matrix dataClasses;

    /**
     * Set to true when {@code dataClasses} has been accessed the first time to
     * mark that the singular values have been applied to each value in the
     * matrix.
     */
    private boolean scaledDataClasses;

    /**
     * The singular values computed during factorization.
     */
    private double[] singularValues;

    /**
     * {@inheritDoc}
     */
    public void factorize(SparseMatrix matrix, int dimensions) {
        Matrix[] SVD = SvdlibjDriver.svd(matrix, dimensions);

        dataClasses = SVD[0];
        scaledDataClasses = false;

        classFeatures = SVD[2];
        scaledClassFeatures = false;

        singularValues = new double[dimensions];
        for (int k = 0; k < dimensions; ++k)
            singularValues[k] = SVD[1].get(k, k);
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(MatrixFile mFile, int dimensions) {
        try {
            Matrix[] SVD = SvdlibjDriver.svd(
                    mFile.getFile(), mFile.getFormat(), dimensions);

            dataClasses = SVD[0];
            scaledDataClasses = false;

            classFeatures = SVD[2];
            scaledClassFeatures = false;

            singularValues = new double[dimensions];
            for (int k = 0; k < dimensions; ++k)
                singularValues[k] = SVD[1].get(k, k);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Matrix dataClasses() {
        if (!scaledDataClasses) {
            scaledDataClasses = true;
            // Weight the values in the data point space by the singular
            // values.
            //
            // REMINDER: when the RowScaledMatrix class is merged in with
            // the trunk, this code should be replaced.
            for (int r = 0; r < dataClasses.rows(); ++r)
                for (int c = 0; c < dataClasses.columns(); ++c)
                    dataClasses.set(r, c, dataClasses.get(r, c) * 
                                          singularValues[c]);
        }

        return dataClasses;
    }

    /**
     * {@inheritDoc}
     */
    public Matrix classFeatures() {
        if (!scaledClassFeatures) {
            scaledClassFeatures = true;
            // Weight the values in the document space by the singular
            // values.
            //
            // REMINDER: when the RowScaledMatrix class is merged in with
            // the trunk, this code should be replaced.
            for (int r = 0; r < classFeatures.rows(); ++r)
                for (int c = 0; c < classFeatures.columns(); ++c)
                    classFeatures.set(r, c, classFeatures.get(r, c) * 
                                            singularValues[c]);
        }

        return classFeatures;
    }

    /**
     * Returns a double array of the singular values.
     */
    public double[] singularValues() {
        return singularValues;
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new SvdlibcSparseBinaryMatrixBuilder();
    }
}
