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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An internal class for interacting with the <a
 * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>
 * library on the command line.  This class is provided in order to expose more
 * of the cluto interface for other clustering methods without duplicating code.
 *
 * @author David Jurgens
 */
class ClutoWrapper {

    /**
     * A logger to track the status of Cluto.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ClutoWrapper.class.getName());

    /**
     * Uninstantiable
     */
    private ClutoWrapper() { }

    /**
     * Clusters the rows of the give file into the specified number of clusters
     * using the string {@code method} to indicate to Cluto which type of
     * clustering to use, and returning the assignment.
     *
     * @param matrixFile The data file containing the data points to cluster.
     * @param outputFile The data file that will store the cluster assignments
     *                   made by cluto.
     * @param numClusters The number of clusters into which the matrix should
     *                    divided.
     * @param clmethod A string recognized by Cluto that indicates which
     *                 clustering algorithm should be used.
     * @param crtFunction A string recognized by Cluto that indiicates which
     *                    criterion method should be used.
     *
     * @return clusterAssignment An array where each element corresponds to a
     *         row and the filled in value will be the cluster number to which
     *         that row was assigned.  Cluster numbers will start at 0 and
     *         increase.  Rows that were not able to be clustered will be
     *         assigned a -1 value.
     */
    static Assignments cluster(Matrix matrix, String clmethod,
                                String crtFunction, int numClusters)
            throws IOException {
        Assignment[] assignments = new Assignment[matrix.rows()];        
        File outputFile = File.createTempFile("cluto-output", ".matrix");
        outputFile.deleteOnExit();
        cluster(assignments, matrix, clmethod, crtFunction,
                numClusters, outputFile);
        extractAssignments(outputFile, assignments);
        return new Assignments(numClusters, assignments, matrix);
    }


    /**
     * Clusters the rows of the give file into the specified number of clusters
     * using the string {@code method} to indicate to Cluto which type of
     * clustering to use, and returns the standard output from the program.
     *
     * @param clusterAssignment An <b>input parameters</b> array where each
     *        element corresponds to a row and the filled in value will be the
     *        cluster number to which that row was assigned.  Cluster numbers
     *        will start at 0 and increase.  Rows that were not able to be
     *        clustered will be assigned a -1 value.
     * @param matrixFile The data file containing the data points to cluster.
     * @param outputFile The data file that will store the cluster assignments
     *        made by cluto.
     * @param numClusters The number of clusters into which the matrix should
     *        divided.
     * @param clmethod A string recognized by Cluto that indicates which
     *        clustering algorithm should be used.
     *
     * @return A string containing the standard output created by Cluto.
     */
    static String cluster(Assignment[] clusterAssignment,
                          Matrix matrix, 
                          String clmethod,
                          String crtFun,
                          int numClusters,
                          File outputFile) 
            throws IOException {

        LOGGER.log(Level.FINE, "clustering {0} data points with {1} features",
                   new Object[] { matrix.rows(), matrix.columns() });
        File matrixFile = File.createTempFile("cluto-input",".matrix");
        // NOTE: Cluto seems to have allocation problems on sparse matrices that
        // are dense.  Therefore, try to estimate whether to use a dense matrix
        // format based on the matrix type
        MatrixIO.writeMatrix(matrix, matrixFile, 
                             ((matrix instanceof SparseMatrix) 
                              ? MatrixIO.Format.CLUTO_SPARSE
                              : MatrixIO.Format.CLUTO_DENSE));
        String output = cluster(clusterAssignment, matrixFile, clmethod, crtFun,
                                outputFile, numClusters);
        // Clean up the temporary file now, and if for some reason that failed,
        // mark the file to be deleted on exit.
        if (!matrixFile.delete())
            matrixFile.deleteOnExit();
        return output;
    }


    /**
     * Clusters the rows of the give file into the specified number of clusters
     * using the string {@code method} to indicate to Cluto which type of
     * clustering to use.
     *
     * @param clusterAssignment An <b>input parameer</b> that is an array where
     *        each element corresponds to a row and the filled in value will be
     *        the cluster number to which that row was assigned.  Cluster
     *        numbers will start at 0 and increase.  Rows that were not able to
     *        be clustered will be assigned a -1 value.
     * @param matrixFile The data file containing the data points to cluster.
     * @param outputFile The data file that will store the cluster assignments
     *        made by cluto.
     * @param numClusters The number of clusters into which the matrix should
     *        divided.
     * @param clmethod A string recognized by Cluto that indicates which
     *        clustering algorithm should be used.
     * @param crtFun The criterion function to use.
     *
     * @return A string containing the standard output created by Cluto.
     */
    public static String cluster(Assignment[] clusterAssignment, 
                                 File matrixFile,
                                 String clmethod,
                                 String crtFun,
                                 File outputFile,
                                 int numClusters) 
            throws IOException {
        // NOTE: the defaults for Agglomerative clustering are cosine similarity
        // and using mean-link (UPGMA) clustering, which is what we want.
        String commandLine = "vcluster " +
            "-clmethod=" + clmethod + " " +
            "-clustfile=" + outputFile  + " " +
            "-crfun=" + crtFun +
            " " + matrixFile +
            " " + numClusters;
        LOGGER.fine("executing: " + commandLine);
        Process cluto = Runtime.getRuntime().exec(commandLine);
        
        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(cluto.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(cluto.getErrorStream()));
        
        String clutoOutput = null;
        StringBuilder output = new StringBuilder("Cluto output:\n");
        for (String line = null; (line = stdout.readLine()) != null; ) 
            output.append(line).append("\n");
        clutoOutput = output.toString();
        if (LOGGER.isLoggable(Level.FINE))
            System.err.println(clutoOutput);
	    
        int exitStatus = 0;
        try {
            exitStatus = cluto.waitFor();
        } catch (InterruptedException ie) {
            LOGGER.log(Level.SEVERE, "Cluto", ie);
        }
        
        LOGGER.finer("Cluto exit status: " + exitStatus);

        // If Cluto was successful in generating the clustering the rows, read
        // in the results file to generate the output.
        if (exitStatus == 0 && clusterAssignment != null)
            extractAssignments(outputFile, clusterAssignment);
        else if (exitStatus != 0) {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; )
                sb.append(line).append("\n");

            // warning or error?
            LOGGER.warning("Cluto exited with error status.  " + exitStatus +
                               " stderr:\n" + sb.toString());
            throw new Error("Clustering failed");
        }

        stdout.close();
        stderr.close();

        return clutoOutput;
    }

    /**
     * Extracts the set of assignemnts from a CLUTO assignment file.
     *
     * @param outputFile the file containing the output of CLUTO's clustering
     * @param clusterAssignment an <i>input parameters</i> whose values will be
     *        set based on the contents of {@code outputFile}. 
     */
    static void extractAssignments(File outputFile,
                                   Assignment[] clusterAssignment)
            throws IOException {
        // The cluster assignmnet file is formatted as each row (data point)
        // having its cluster label specified on a separate line.  We can
        // read these in sequence to generate the output array.
        BufferedReader br = new BufferedReader(new FileReader(outputFile));
        for (int i = 0; i < clusterAssignment.length; ++i) {
            int j = Integer.parseInt(br.readLine());
            clusterAssignment[i] = (j < 0)
                ? new HardAssignment()   // no cluster assignment 
                : new HardAssignment(j); // specific cluster
        }
        br.close();
    }
}
