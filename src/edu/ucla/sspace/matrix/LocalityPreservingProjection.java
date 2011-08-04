/*
 * Copyright 2010 David Jurgens
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

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.Matrix.Type;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Iterator;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Java wrapper around the Matlab implementation of the Spectral Regression
 * form of Locality Preserving Projection (LPP), which is a linear-time subspace
 * projection.  Details on LPP may be found in the paper by He and Niyogi: <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">Xiaofei He and Partha
 *     Niyogi, "Locality Preserving Projections," in <i>Proceedings of Advances
 *     in Neural Information Processing Systems 16 (NIPS 2003)</i>. Vancouver
 *     Canada. 2003.  Available <a
 *     href="http://books.nips.cc/papers/files/nips16/NIPS2003_AA20.pdf">here</a></li>
 * </ul>
 *
 * Further information about the Spectral Regression version of the algorithm
 * may be found on Deng Cai's <a
 * href="http://www.zjucadcg.cn/dengcai/Data/data.html">webpage</a>.  This class
 * requires that the following Matlab files availabe on the aforementioned
 * webpage be made available in the root directory of the calling code: {@code
 * SR_caller.m}, {@code SR.m}, {@code lsqr2.m}, and {@code lars.m}.
 *
 * <p> This class requires the availability of Matlab or Octave.
 *
 * <p> Each of the projection methods requires the prior calculation of the
 * affinity matrix.  This matrix identifies which rows in the input matrix have
 * some degree of local proximity.  A non-zero value in the affinity matrix
 * specifies the weight of the connection between the two rows.  Weights may be
 * of any positive value.  See {@link AffinityMatrixCreator} for programatic
 * ways of creating an affinity matrix based on similarity methods.
 *
 * @see SVD
 * @see AffinityMatrixCreator
 */
public class LocalityPreservingProjection {

    private static final Logger LOGGER = 
	Logger.getLogger(LocalityPreservingProjection.class.getName());

    /**
     * The generic Matlab/Octave implementation of SR-LPP where the files and
     * language-specific I/O calls have been left open as printf formatting
     * arguments to be later specified.
     */
    private static final String SR_LPP_M =
        "%% This code requires the SR-LPP implementation by Deng Cai (dengcai AT\n" +
        "%% gmail.com) available at\n" +
        "%% http://www.zjucadcg.cn/dengcai/SR/index.html\n" +
        "\n" +
        "%% Load the data matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "data = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +   
        "\n" +
        "%% Load the affinity matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "W = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +
        "\n" +
        "%% If 0, all of the dimensions in the adj. matrix are used\n" +
        "Dim = %d\n" +
        "\n" +
        "options = [];\n" +
        "options.W = W;\n" +
        "options.ReguAlpha = 0.01;\n" +
        "options.ReguType = 'Ridge';\n" +
        "options.ReducedDim = Dim;\n" +
        "%% Call the SR code\n" +
        "[eigvector] = SR_caller(options, data);\n" +
        "[nSmp,nFea] = size(data);\n" +
        "if size(eigvector,1) == nFea + 1\n" +
        "    Projection = [data ones(nSmp,1)]*eigvector;\n" +
        "else\n" +
        "    Projection = data*eigvector;\n" +
        "end\n" +
        "%% Save the projection as a matrix\n" +
        "%s\n" +
        "fprintf(1,'Finished\\n');\n" +
        "\n";

    /**
     * The generic Matlab/Octave implementation of LPP where the files and
     * language-specific I/O calls have been left open as printf formatting
     * arguments to be later specified.
     */
    private static final String LPP_M =
        "%% LPP code based on the Matlab implementation by Deng Cai (dengcai2 AT\n" +
        "%% cs.uiuc.edu) available at\n" +
        "%% http://www.cs.uiuc.edu/homes/dengcai2/Data/code/LPP.m\n" +
        "\n" +
        "%% Load the data matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "data = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +   
        "[nSmp,nFea] = size(data);\n" +
        // NOTE: the following 5 lines subtract out the mean from the data.
        // This process might not be feasible for very large data sets, so it
        // might be worth making this configurable in the future
        "%% Subtract out the mean fromm the data.  See page 7 of the LPI paper\n" +
        "printf('Subtracting out the mean\\n');\n" +
        "if issparse(data)\n" +
        "    data = full(data);\n" +
        "end\n" +
        "sampleMean = mean(data);\n" +
        "data = (data - repmat(sampleMean,nSmp,1));\n" +
        "\n" +
        "%% Load the affinity matrix from file\n" +
        "Tmp = load('%s','-ascii');\n" +
        "W = spconvert(Tmp);\n" +
        "%% Remove the raw data file to save space\n" +
        "clear Tmp;\n" +
        "\n" +
        "%% If 0, all of the dimensions in the adj. matrix are used\n" +
        "Dim = %d\n" +
        "\n" +
        "options = [];\n" +
        "\n" +
        "D = full(sum(W,2));\n" +
        "%%options.ReguAlpha = options.ReguAlpha*sum(D)/length(D);\n" +
        "D = sparse(1:nSmp,1:nSmp,D,nSmp,nSmp);\n" +
        "\n" +
        "printf('Computing D prime\\n');\n" +
        "DPrime = data'*D*data;\n" +
        "DPrime = max(DPrime,DPrime');\n" +
        "\n" +
        "printf('Computing W prime\\n');\n" +
        "WPrime = data'*W*data;\n" +
        "WPrime = max(WPrime,WPrime');\n" +
        "\n" +
        "dimMatrix = size(WPrime,2);\n" +
        "\n" +
        "if Dim > dimMatrix\n" +
        "    Dim = dimMatrix;\n" + 
        "end\n" +
        "\n" +
        "%% Before using eigs, check whether the affinity matrix is positive and definite\n" +
        "%%printf('Testing if DPrime is pos. def.\\n');\n" +
        "%%isposdef = true;\n" +
        "%%for i=1:length(DPrime)\n" +
        "%%     if ( det( DPrime(1:i, 1:i) ) <= 0 )\n" +
        "%%          isposdef = false;\n" +
        "%%          break;\n" +
        "%%      end\n" +
        "%%end\n" +
        "\n" +
        "%%if (isposdef & dimMatrix > 1000 & Dim < dimMatrix/10) | (dimMatrix > 500 & Dim < dimMatrix/20) | (dimMatrix > 250 & Dim < dimMatrix/30)\n" +
        "%%    bEigs = 1;\n" +
        "%%else\n" +
        "    bEigs = 0;\n" +
        "%%end\n" +
        "\n" +
        "\n" +
        "printf('Computing Eigenvectors\\n');\n" +
        "if bEigs\n" +
        "    %%disp('using eigs to speed up!');\n" +
        "    [eigvector, eigvalue] = eigs(WPrime,DPrime,Dim,'la');\n" +
        "    eigvalue = diag(eigvalue);\n" +
        "else\n" +
        "    [eigvector, eigvalue] = eig(WPrime,DPrime);\n" +
        "    eigvalue = diag(eigvalue);\n" +
        "\n" +
        "    [junk, index] = sort(-eigvalue);\n" +
        "    eigvalue = eigvalue(index);\n" +
        "    eigvector = eigvector(:,index);\n" +
        "\n" +
        "    if Dim < size(eigvector,2)\n" +
        "        eigvector = eigvector(:, 1:Dim);\n" +
        "        eigvalue = eigvalue(1:Dim);\n" +
        "    end\n" +
        "end\n" +
        "\n" +
        "for i = 1:size(eigvector,2)\n" +
        "    eigvector(:,i) = eigvector(:,i)./norm(eigvector(:,i));\n" +
        "end\n" +
        "\n" +
        "eigIdx = find(eigvalue < 1e-3);\n" +
        "eigvalue (eigIdx) = [];\n" +
        "eigvector(:,eigIdx) = [];\n" +
        "\n" +
        "%% Compute the projection\n" +
        "printf('Computing projection matrix\\n');\n" +
        "projection = data*eigvector;\n" +
        "\n" +
        "%% Save the projection as a matrix\n" +
        "%s\n" +
        "printf('Finished\\n');" +
        "\n";
      
    /**
     * Uninstantiable
     */
    private LocalityPreservingProjection() { }

    /**
     * Projects the rows of the input matrix into a lower dimensional subspace
     * using the Locality Preserving Projection (LPP) algorithm and the affinity
     * matrix as a guide to locality.
     *
     * @param inputMatrix a matrix file whose rows will be projected
     * @param affinityMatrix a square matrix whose entries denote locality
     *        between the rows of the inputMatrix.  Note that this matrix's
     *        dimensions must be equal to the number of rows in the input
     *        matrix.
     * @param dimensions the number of dimensions into which the inputMatrix
     *        should be projected
     *
     * @return a file containing the LPP-reduced data in {@code DENSE_TEXT}
     *         format.
     *
     * @throws IOError if any exception occurrs during processing
     */
    public static MatrixFile project(MatrixFile inputMatrix,
                                     MatrixFile affinityMatrix,
                                     int dimensions) {
        try {
            File outputFile = File.createTempFile("lcc-output-matrix", ".dat");
            execute(inputMatrix.getFile(), affinityMatrix.getFile(), dimensions,
                    outputFile);
            return new MatrixFile(outputFile, MatrixIO.Format.DENSE_TEXT);
        } catch (IOException ioe) { 
            throw new IOError(ioe);
        }
    }

    /**
     * Projects the rows of the input matrix into a lower dimensional subspace
     * using the Locality Preserving Projection (LPP) algorithm and the affinity
     * matrix as a guide to locality.
     *
     * @param m a matrix whose rows will be projected
     * @param affinityMatrix a square matrix whose entries denote locality
     *        between the rows of the inputMatrix.  Note that this matrix's
     *        dimensions must be equal to the number of rows in the input
     *        matrix.
     * @param dimensions the number of dimensions into which the inputMatrix
     *        should be projected
     *
     * @return a {@code Matrix} that contains the rows of {@code m} projected
     *         into the specified number of dimensions
     *
     * @throws IOError if any exception occurrs during processing
     */
    public static Matrix project(Matrix m, MatrixFile affinityMatrix,
                                 int dimensions) {        
        try {
            return execute(m, affinityMatrix, dimensions);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }        
    }

    /**
     * Projects the rows of the input matrix into a lower dimensional subspace
     * using the Locality Preserving Projection (LPP) algorithm and the affinity
     * matrix as a guide to locality.
     *
     * @param m a matrix whose rows will be projected
     * @param affinityMatrix a square matrix whose entries denote locality
     *        between the rows of the inputMatrix.  Note that this matrix's
     *        dimensions must be equal to the number of rows in the input
     *        matrix.
     * @param dimensions the number of dimensions into which the inputMatrix
     *        should be projected
     *
     * @return a {@code Matrix} that contains the rows of {@code m} projected
     *         into the specified number of dimensions
     *
     * @throws IOError if any exception occurrs during processing
     */
    public static Matrix project(Matrix m, Matrix affinityMatrix,
                                 int dimensions) {        
        try {
            File affMatrixFile = 
                File.createTempFile("affinity-matrix", ".dat");
            MatrixIO.writeMatrix(affinityMatrix, affMatrixFile, 
                                 MatrixIO.Format.MATLAB_SPARSE);
            return execute(m, new MatrixFile(affMatrixFile, 
                                             MatrixIO.Format.MATLAB_SPARSE), 
                           dimensions);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }        
    }

    /**
     * Executes the LPP script, thereby computing the locality preserving
     * projection of the data matrix to the specified number of dimension, using
     * the affinity matrix to determine locality, and returning the result as a
     * {@link Matrix}.
     *
     * @param dataMatrix a matrix where each row is a data points to be
     *        projected
     * @param affMatrixFile the file containing the affinity matrix that
     *        connects data points in the {@code dataMatrixFile}
     * @param dims the number of dimensions to which the matrix should be
     *        reduced
     */
    private static Matrix execute(Matrix dataMatrix, MatrixFile affMatrixFile,
                                  int dims) throws IOException {
        // Write the input matrix to a file for Matlab/Octave to use
        File mInput = File.createTempFile("lpp-input-data-matrix",".dat");
        mInput.deleteOnExit();
        MatrixIO.writeMatrix(dataMatrix, mInput, MatrixIO.Format.MATLAB_SPARSE);        

        // Create an output matrix to hold the results of the computation from
        // Matlab until they can be read back into memory
        File output = File.createTempFile("lpp-output-matrix",".dat");

        // Exceute the LPP code
        execute(mInput, affMatrixFile.getFile(), dims, output);

        // Upon finishing, read the matrix back into memory.
        return MatrixIO.readMatrix(output, MatrixIO.Format.DENSE_TEXT);
    }

    /**
     * Executes the LPP script, thereby computing the locality preserving
     * projection of the data matrix to the specified number of dimension, using
     * the affinity matrix to determine locality.  The result is written to the
     * output file.
     *
     * @param dataMatrixFile a file containing the original data points to be
     *        projected
     * @param affMatrixFile the file containing the affinity matrix that
     *        connects data points in the {@code dataMatrixFile}
     * @param dims the number of dimensions to which the matrix should be
     *        reduced
     * @param outputMatrix the file to which the output matrix should be written
     *        in DENSE_TEXT format
     */
    private static void execute(File dataMatrixFile, 
                                File affMatrixFile,
                                int dims, File outputMatrix) 
            throws IOException {
        // Decide whether to use Matlab or Octave
        if (isMatlabAvailable())
            invokeMatlab(dataMatrixFile, affMatrixFile, dims, outputMatrix);
            // Ensure that if Matlab isn't present that we can at least use Octave
        else if (isOctaveAvailable())
            invokeOctave(dataMatrixFile, affMatrixFile, dims, outputMatrix);
        else
            throw new IllegalStateException(
                "Cannot find Matlab or Octave to invoke LPP");
    }

    /**
     * Invokes Matlab to run the LPP script
     */
    private static void invokeMatlab(File dataMatrixFile, File affMatrixFile, 
                                     int dimensions, File outputFile) 
            throws IOException {

        String commandLine = "matlab -nodisplay -nosplash -nojvm";
        LOGGER.fine(commandLine);
        Process matlab = Runtime.getRuntime().exec(commandLine);
	    
        // Create the Matlab-specified output code for the saving the matrix
        String outputStr =
            "save " + outputFile.getAbsolutePath() + " projection -ASCII\n";
        
        // Fill in the Matlab-specific I/O 
        String matlabProgram = String.format(SR_LPP_M, 
					     dataMatrixFile.getAbsolutePath(), 
					     affMatrixFile.getAbsolutePath(),
					     dimensions, outputStr);

        // Pipe the program to Matlab for execution
        PrintWriter stdin = new PrintWriter(matlab.getOutputStream());
        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(matlab.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(matlab.getErrorStream()));

        stdin.println(matlabProgram);
        stdin.close();

        // Capture the output.  Matlab will not automatically finish executing
        // after the script ends, so look for the "Finished" text printed at the
        // end to know when to stop the process manually.
        StringBuilder output = new StringBuilder("Matlab LPP output:\n");
        for (String line = null; (line = stdout.readLine()) != null; ) {
            output.append(line).append("\n");
            if (line.equals("Finished")) {
                matlab.destroy();
            }
        }
        LOGGER.fine(output.toString());
	
        int exitStatus = -1;
        try {
            exitStatus = matlab.waitFor();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOGGER.fine("Octave LPP exit status: " + exitStatus);
        
        // If Matlab was not successful throw an error to indicate the output
        // file may be in an inconsistent state
        if (exitStatus != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
            throw new IllegalStateException(
                "Matlab LPP did not finish normally: " + sb);
        }
    }

    /**
     * Invokes Octave to run the LPP script
     */
    private static void invokeOctave(File dataMatrixFile, File affMatrixFile, 
                                     int dimensions, File outputFile) 
            throws IOException {

        // Create the octave file for executing
        File octaveFile = File.createTempFile("octave-LPP",".m");
        // Create the Matlab-specified output code for the saving the matrix
        String outputStr = 
            "save(\"-ascii\", \"" + outputFile.getAbsolutePath()
            + "\", \"projection\");\n";
        
        String octaveProgram = null;
        try {
            // Fill in the Matlab-specific I/O 
            octaveProgram = String.format(SR_LPP_M,
                                          dataMatrixFile.getAbsolutePath(), 
                                          affMatrixFile.getAbsolutePath(),
                                          dimensions, outputStr);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        PrintWriter pw = new PrintWriter(octaveFile);
        pw.println(octaveProgram);
        pw.close();
        
        // build a command line where octave executes the previously constructed
        // file
        String commandLine = "octave " + octaveFile.getAbsolutePath();
        LOGGER.fine(commandLine);
        Process octave = Runtime.getRuntime().exec(commandLine);

        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(octave.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(octave.getErrorStream()));

        // Capture the output for logging
        StringBuilder output = new StringBuilder("Octave LPP output:\n");
        for (String line = null; (line = stdout.readLine()) != null; ) {
            output.append(line).append("\n");
        }
        LOGGER.fine(output.toString());
	    
        int exitStatus = -1;
        try {
            exitStatus = octave.waitFor();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOGGER.fine("Octave LPP exit status: " + exitStatus);

        // If Octave wasn't successful, throw an exception with the output
        if (exitStatus != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
            throw new IllegalStateException(
                "Octave LPP did not finish normally: " + sb);
        }
    }
     
    /**
     * Returns {@code true} if Octave is available
     */
    private static boolean isOctaveAvailable() {
	try {
	    Process octave = Runtime.getRuntime().exec("octave -v");
            octave.waitFor();
	} catch (Exception e) {
	    return false;
	}
	return true;	
    }

    /**
     * Returns {@code true} if Matlab is available
     */
    private static boolean isMatlabAvailable() {
	try {
	    Process matlab = Runtime.getRuntime().exec("matlab -h");
            matlab.waitFor();
	} catch (Exception ioe) {
	    return false;
	}
	return true;
    }    
}