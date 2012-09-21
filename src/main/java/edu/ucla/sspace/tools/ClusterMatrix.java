/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.clustering.Partition;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.matrix.CellMaskedMatrix;
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Clusters a data matrix using the specified algorithm with with requested
 * number of clusters.
 *
 * Arguments are:
 * <ol>
 *   <li>matrixFile</li>
 *   <li>file Format</li>
 *   <li>clustering algorithm</li>
 *   <li>number of clusters</li>
 * </ol>
 *
 * Cluster assignments will be reported to standard out.
 *
 * @author Keith Stevens
 */
public class ClusterMatrix {

    private static final Logger LOG =
        Logger.getLogger(ClusterMatrix.class.getName());

    /**
     * Filles a matrix with a range of numbers starting from {@code start} to
     * {@code end}.
     */
    public static int[] range(int start, int end) {
        int[] r = new int[end-start];
        for (int i = 0; i < r.length; ++i)
            r[i] = i+start;
        return r;
    }

    /**
     * Samples {@code sampleSize} values between {@code start} and {@code end}.
     * If {@code withReplacement}, values can be sampled more than once,
     * otherwise all sampled values will be unique.
     */
    public static int[] sample(int start, int end,
                               int sampleSize,
                               boolean withReplacement) {
        int[] r = new int[sampleSize];
        if (!withReplacement) {
            List<Integer> l = new ArrayList<Integer>();
            for (int i = start; i < end; ++i)
                l.add(i);
            Collections.shuffle(l);
            for (int i = 0; i < sampleSize; ++i)
                r[i] = l.get(i);
        } else {
            Random rand = new Random();
            for (int i = 0; i < r.length; ++i)
                r[i] = rand.nextInt(end-start) + start;
        }
        return r;
    }

    public static void main(String[] vargs) throws Exception {
        LoggerUtil.setLevel(Level.FINE);

        ArgOptions options = new ArgOptions();
        options.addOption('f', "sampleFeatures",
                          "If true, sampling will be done over the feature " +
                          "space only",
                          false, null, "Optional");
        options.addOption('g', "sampleGraph",
                          "If true, the data matrix will be treated as a " +
                          "symmetric adjacency matrix, and both rows and " +
                          "columns will be sampled.",
                          false, null, "Optional");
        options.addOption('r', "sampleWithReplacement",
                          "If sampleRate is set, and this is set, sampling " +
                          "will be done with replacement.  Otherwise, the " +
                          "data matrix will be sampled without replacement.",
                          false, null, "Optional");
        options.addOption('s', "sampleRate",
                          "If set, the data matrix will be randomly sampled. " +
                          "The smaller sampled matrix will then be clustered " +
                          "using the listed algorithm.  The final clustering " +
                          "soultion will be reported in terms of the " +
                          "original matrix.",
                          true, "FLOAT", "Optional");
        String[] args = options.parseOptions(vargs);
        if (args.length != 5) {
            System.err.printf("Given %d arguments, requires 5\n", args.length);
            System.err.println("usage: Java ClusterMatrix [options] <matrixFile> <format> <clusterAlg> <numClusters> <outfile>\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        LOG.fine("Loading data matrix");
        File matrixFile = new File(args[0]);
        Format format = Format.valueOf(args[1]);
        Matrix matrix = MatrixIO.readMatrix(matrixFile, format);
        int numRows = matrix.rows();
        int[] rowOrdering = null;
        int[] colOrdering = null;
        if (options.hasOption('s')) {
            LOG.fine("Sampling data matrix");
            if (options.hasOption('f')) {
                int sampleSize = (int) Math.ceil(matrix.columns() *
                                 options.getDoubleOption('s'));
                rowOrdering = range(0, matrix.rows());
                colOrdering = sample(0, matrix.columns(), sampleSize, 
                                     options.hasOption('r'));
            } else {
                int sampleSize = (int) Math.ceil(matrix.rows() *
                                 options.getDoubleOption('s'));
                rowOrdering = sample(0, matrix.rows(), sampleSize, 
                                     options.hasOption('r'));
                colOrdering = (options.hasOption('g'))
                    ? rowOrdering
                    : range(0, matrix.columns());
            }

            if (matrix instanceof SparseMatrix)
                matrix = new CellMaskedSparseMatrix((SparseMatrix) matrix,
                                                    rowOrdering, colOrdering);
            else
                matrix = new CellMaskedMatrix(matrix, rowOrdering, colOrdering);
        }

        LOG.fine("Clustering data matrix");
        Clustering alg = ReflectionUtil.getObjectInstance(args[2]);
        int numClusters = Integer.parseInt(args[3]);
        Partition p = Partition.fromAssignments(
                alg.cluster(matrix, numClusters, System.getProperties()));

        LOG.fine("Outputing Cluster Assignments");
        PrintWriter w = new PrintWriter(args[4]);
        w.printf("%d %d\n", numRows, p.numClusters());
        for (Set<Integer> points : p.clusters()) {
            for (int point : points)
                w.printf("%d ", (rowOrdering != null) ? rowOrdering[point] : point);
            w.println();
        }
        w.close();
    }
}
