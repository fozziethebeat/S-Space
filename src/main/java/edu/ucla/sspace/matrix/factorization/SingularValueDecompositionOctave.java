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

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A wrapper around the Octave implementation of the SVD.
 *
 * @author Keith Stevens
 */
public class SingularValueDecompositionOctave extends AbstractSvd 
        implements SingularValueDecomposition, java.io.Serializable {

    private static final long serialVersionUID = 1L;


    private static final Logger LOG = 
        Logger.getLogger(SingularValueDecompositionOctave.class.getName());

    public void factorize(SparseMatrix matrix, int dimensions) {
        try {
            File mFile = File.createTempFile("octave-input", ".dat");
            MatrixIO.writeMatrix(matrix, mFile, Format.MATLAB_SPARSE);
            factorize(new MatrixFile(mFile, Format.MATLAB_SPARSE), dimensions);
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Converting to matlab file", ioe);
        }
    }

    public void factorize(MatrixFile mfile, int dimensions) {
        File matrix;

        try {
            if (mfile.getFormat() == Format.MATLAB_SPARSE)
                matrix = mfile.getFile();
            else
                matrix = MatrixIO.convertFormat(mfile.getFile(), 
                                                mfile.getFormat(),
                                                Format.MATLAB_SPARSE);

            // create the octave file for executing
            File octaveFile = File.createTempFile("octave-svds",".m");
            File uOutput = File.createTempFile("octave-svds-U",".dat");
            File sOutput = File.createTempFile("octave-svds-S",".dat");
            File vOutput = File.createTempFile("octave-svds-V",".dat");
            octaveFile.deleteOnExit();
            uOutput.deleteOnExit();
            sOutput.deleteOnExit();
            vOutput.deleteOnExit();

            // Print the customized Octave program to a file.
            PrintWriter pw = new PrintWriter(octaveFile);
            pw.printf(
                "Z = load('%s','-ascii');\n" +
                "A = spconvert(Z);\n" + 
                "clear Z;\n" + 
                "[U, S, V] = svds(A, %d);\n" +
                "save(\"-ascii\", \"%s\", \"U\");\n" +
                "save(\"-ascii\", \"%s\", \"S\");\n" +
                "save(\"-ascii\", \"%s\", \"V\");\n" +
                "fprintf('Octave Finished\\n');\n",
                matrix.getAbsolutePath(), dimensions, uOutput.getAbsolutePath(),
                sOutput.getAbsolutePath(), vOutput.getAbsolutePath());
            pw.close();
            
            // build a command line where octave executes the previously
            // constructed file
            String commandLine = "octave " + octaveFile.getAbsolutePath();
            LOG.fine(commandLine);
            Process octave = Runtime.getRuntime().exec(commandLine);

            BufferedReader br = new BufferedReader(
                new InputStreamReader(octave.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                new InputStreamReader(octave.getErrorStream()));

            // capture the output
            StringBuilder output = new StringBuilder("Octave svds output:\n");
            for (String line = null; (line = br.readLine()) != null; ) {
                output.append(line).append("\n");
            }
            LOG.fine(output.toString());
            
            int exitStatus = octave.waitFor();
            LOG.fine("Octave svds exit status: " + exitStatus);

            // If Octave was successful in generating the files, return them.
            if (exitStatus == 0) {

                // load U in memory, since that is what most algorithms will be
                // using (i.e. it is the word space)
                U = MatrixIO.readMatrix(uOutput, Format.DENSE_TEXT, 
                                        Type.DENSE_IN_MEMORY);
                scaledClassFeatures = false;

                // Sigma only has n values for an n^2 matrix, so make it sparse
                Matrix S = MatrixIO.readMatrix(sOutput, Format.DENSE_TEXT, 
                                               Type.SPARSE_ON_DISK);
                singularValues = new double[dimensions];
                for (int s = 0; s < dimensions; ++s)
                    singularValues[s] = S.get(s, s);

                // Octave does not transpose V, so transpose it
                V = MatrixIO.readMatrix(vOutput, Format.DENSE_TEXT, 
                                        Type.DENSE_ON_DISK, true);
                scaledDataClasses = false;
            }
            else {
                StringBuilder sb = new StringBuilder();
                for (String line = null; (line = stderr.readLine()) != null; ) {
                    sb.append(line).append("\n");
                }
                // warning or error?
                LOG.warning("Octave exited with error status.  " + 
                                   "stderr:\n" + sb.toString());
            }
        } catch (IOException ioe) {
            LOG.log(Level.SEVERE, "Octave svds", ioe);
        } catch (InterruptedException ie) {
            LOG.log(Level.SEVERE, "Octave svds", ie);
        }
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new MatlabSparseMatrixBuilder();
    }
}
