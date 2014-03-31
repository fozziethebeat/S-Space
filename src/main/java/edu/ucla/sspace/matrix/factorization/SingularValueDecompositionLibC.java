/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.ArrayMatrix;
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
public class SingularValueDecompositionLibC extends AbstractSvd 
        implements SingularValueDecomposition, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = 
        Logger.getLogger(SingularValueDecompositionLibC.class.getName());

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
     *
     * @throws IllegalStateException if the <a
     *         href="http://tedlab.mit.edu/~dr/SVDLIBC/">SVDLIBC</a> command
     *         line executable is not able to be found.
     */
    public void factorize(MatrixFile mFile, int dimensions) {
        if (!isSVDLIBCavailable()) {
            throw new IllegalStateException(
                "Use of this class requires the SVDLIBC command line program, " +
                "which is either not installed on this system or is not " +
                "available to be executed from the command line.  Check that " +
                "your PATH settings are correct or see " + 
                "http://tedlab.mit.edu/~dr/SVDLIBC/ to download and install " +
                "the program.");
        }
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
                " -w dt " + // output is dense binary
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
                U = MatrixIO.readMatrix(
                        Ut, Format.SVDLIBC_DENSE_TEXT, 
                        Type.DENSE_IN_MEMORY, true);
                scaledDataClasses = false;
                
                V = MatrixIO.readMatrix(
                        Vt, Format.SVDLIBC_DENSE_TEXT,
                        Type.DENSE_IN_MEMORY);
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
        double[] m = new double[dimensions];

        // Check that the computed number of dimensions equals the expected
        // number of dimensions.
        int readDimensions = Integer.parseInt(br.readLine());
        if (readDimensions != dimensions)
            throw new RuntimeException(
                    "SVDLIBC generated the incorrect number of " +
                    "dimensions: " + readDimensions + " versus " + dimensions);

        // Read each singular value.
        int i = 0;
        for (String line = null; (line = br.readLine()) != null; )
            m[i++] = Double.parseDouble(line);
        return m;
    }

    /**
     * Returns {@code true} if the SVDLIBC library is available
     */
    public static boolean isSVDLIBCavailable() {
        try {
            Process svdlibc = Runtime.getRuntime().exec("svd");
            BufferedReader br = new BufferedReader(
                new InputStreamReader(svdlibc.getInputStream()));
            // Read the output to avoid some platform specific bugs where the
            // waitFor() call does not return.  Thanks to Fabian for noticing
            // this
            for (String line = null; (line = br.readLine()) != null; )
                ;
            br.close();

            svdlibc.waitFor();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
