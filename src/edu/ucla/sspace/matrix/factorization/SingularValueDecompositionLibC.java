package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.DiagonalMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.SvdlibcSparseBinaryMatrixBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A wrapper around the LibC implentation of Singular Value Decomposition.
 *
 * @author Keith Stevens
 */
public class SingularValueDecompositionLibC implements MatrixFactorization {

    private static final Logger LOG = 
        Logger.getLogger(SingularValueDecompositionLibC.class.getName());

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
        try {
            File temp = File.createTempFile("svdlibc.svd.matrix", "dat");
            MatrixIO.writeMatrix(matrix, temp, Format.SVDLIBC_SPARSE_TEXT);
            MatrixFile mFile = new MatrixFile(temp, Format.SVDLIBC_SPARSE_TEXT);
            factorize(mFile, dimensions);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(MatrixFile mFile, int dimensions) {
        try {
            String formatString = "";
            switch (mFile.getFormat()) {
            case SVDLIBC_DENSE_BINARY:
                formatString = " -r db ";
                break;
            case SVDLIBC_DENSE_TEXT:
                formatString = " -r dt ";
                break;
            case SVDLIBC_SPARSE_BINARY:
                formatString = " -r sb ";
                break;
            case SVDLIBC_SPARSE_TEXT:
                // Do nothing since it's the default format.
                break;
            default:
                throw new UnsupportedOperationException(
                    "Format type is not accepted");
            }

            File outputMatrixFile = File.createTempFile("svdlibc", ".dat");
            outputMatrixFile.deleteOnExit();
            String outputMatrixPrefix = outputMatrixFile.getAbsolutePath();

            LOG.fine("creating SVDLIBC factor matrices at: " + 
                              outputMatrixPrefix);
            String commandLine = "svd -o " + outputMatrixPrefix + formatString +
                " -w db " + // output is dense binary
                " -d " + dimensions + " " + mFile.getFile().getAbsolutePath();
            LOG.fine(commandLine);
            Process svdlibc = Runtime.getRuntime().exec(commandLine);

            BufferedReader stdout = new BufferedReader(
                new InputStreamReader(svdlibc.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                new InputStreamReader(svdlibc.getErrorStream()));

            StringBuilder output = new StringBuilder("SVDLIBC output:\n");
            for (String line = null; (line = stderr.readLine()) != null; ) {
                output.append(line).append("\n");
            }
            LOG.fine(output.toString());
            
            int exitStatus = svdlibc.waitFor();
            LOG.fine("svdlibc exit status: " + exitStatus);

            // If SVDLIBC was successful in generating the files, return them.
            if (exitStatus == 0) {
                File Ut = new File(outputMatrixPrefix + "-Ut");
                File S  = new File(outputMatrixPrefix + "-S");
                File Vt = new File(outputMatrixPrefix + "-Vt");
                    
                // load U in memory, since that is what most algorithms will
                // be using (i.e. it is the word space).  SVDLIBC returns
                // this as U transpose, so correct it by indicating that the
                // read operation should transpose the matrix as it is built
                dataClasses = MatrixIO.readMatrix(
                        Ut, Format.SVDLIBC_DENSE_BINARY, 
                        Type.DENSE_IN_MEMORY, true);
                scaledDataClasses = false;

                // V could be large, so just keep it on disk.  
                classFeatures = MatrixIO.readMatrix(
                        Vt, Format.SVDLIBC_DENSE_BINARY,
                        Type.DENSE_ON_DISK);
                scaledClassFeatures = false;

                // Sigma only has n values for an n^2 matrix, so make it sparse.
                // Note that even if we specify the output to be in dense
                // binary, the signular vectors are still reported as text
                singularValues =  readSVDLIBCsingularVector(S, dimensions);
            } else {
                StringBuilder sb = new StringBuilder();
                for (String line = null; (line = stderr.readLine()) != null; )
                    sb.append(line).append("\n");
                // warning or error?
                LOG.warning("svdlibc exited with error status.  " + 
                               "stderr:\n" + sb.toString());
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "SVDLIBC", ioe);
        } catch (InterruptedException ie) {
            LOG.log(Level.SEVERE, "SVDLIBC", ie);
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

    /**
     * Generates a diagonal {@link Matrix} from the special-case file format
     * that SVDLIBC uses to output the &Sigma; matrix.
     */
    private static double[] readSVDLIBCsingularVector(File sigmaMatrixFile,
                                                      int dimensions)
            throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(sigmaMatrixFile));
        int dimension = -1;
        int valsSeen = 0;
        double[] m = new double[dimensions];
        for (String line = null; (line = br.readLine()) != null; ) {
            String[] vals = line.split("\\s+");
            for (int i = 0; i < vals.length; ++i) {
                // Check that the number of dimensions returned by svdlibc
                // matches the requested number.
                if (dimension == -1) {
                    dimension = Integer.parseInt(vals[i]);
                    if (dimension != dimensions)
                        throw new RuntimeException(
                                "SVDLIBC generated the incorrect number of " +
                                "dimensions: " + dimension + " versus " + 
                                dimensions);
                } else
                    m[++valsSeen] = Double.parseDouble(vals[i]);
            }
        }
        return m;
    }
}
