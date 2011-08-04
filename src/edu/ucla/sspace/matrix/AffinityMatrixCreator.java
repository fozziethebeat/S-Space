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
import edu.ucla.sspace.util.Duple;

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
 * A functional class that given a matrix or matrix file, computes the affinity
 * matrix for the rows in that matrix.  For an {@code n} x {@code m} matrix, the
 * affinity matrix is an {@code n} x {@code n} matrix specifying which of the
 * rows are connected; viewed as a graph, the affinity matrix specifies which
 * rows have edges between them and what the weights of those edges are.
 *
 * <p> This class provides multiple options for specifying how the affinity
 * matrix should be computed both in terms of the number of neighbors and the
 * weights of the edges to those neighbors.
 *
 * <p> Due to the full density of the affinity matrix, the matrix is always
 * written to disk, rather than stored in memory.  This class also provides
 * support for computing the affinity matrix off-core, i.e. without needing to
 * load the matrix entirely into memory.  If the off-core method is specified,
 * the returned matrix will also be on disk.
 *
 * <p> This class is thread safe.
 *
 * @see LocalityPreservingProjection
 */
public class AffinityMatrixCreator {

    /**
     * Methods by which the affinity matrix is constructed.
     */
    public enum EdgeType { 
        /**
         * An edge will be added between two data points, i and j, if j is in
         * the <i>k</i> nearest neighbors of i.  This relationship is not
         * symmetric.
         */
        NEAREST_NEIGHBORS,
        
        /**
         * An edge will be added between two data points, i and j, if the
         * similarity between them is above a certain threshold.  This
         * relationship is symmetric.
         */
        MIN_SIMILARITY 
    }

    /**
     * Options to weight edges in the affinity matrix.
     */
    public enum EdgeWeighting {
        /**
         * The weight is 1 if the data points are connected and 0 otherwise.
         */
        BINARY,

        /**
         * The edges are weighted by a Gaussian kernel (also known as a Heat
         * kernel), which is parameterized by a value <i>t</i>
         */
        GAUSSIAN_KERNEL,

        /**
         * The edges for two data points, i and j, are weighted by
         * (x<sup>T</sup><sub>i</sub>x<sub>j</sub> + 1)<sup>d</sup>, where
         * <i>d</i> indicates the degree of the polynomial kernel.
         */
        POLYNOMIAL_KERNEL,

        /**
         * The edge between two data points has the value of their dot product.
         */
        DOT_PRODUCT,

        /**
         * The edge between two data points has the value of their cosine
         * similarity.  Note that this case is equivalent to a dot product
         * normalized to 1.
         */
        COSINE_SIMILARITY,
    }

    private static final Logger LOGGER = 
	Logger.getLogger(AffinityMatrixCreator.class.getName());

    private AffinityMatrixCreator() {}

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, returning the result as a file on disk.
     * Because the input matrix is provided already in memory, all similarity
     * comparisons are performed in memory, which offers a significant speed-up
     * compared to the off-core overloads of this method that operate on a
     * {@link MatrixFile} as input.
     *
     * @param dataSimilarityMetric the metric by which two data points should be
     *        compared when constructing the affinity matrix.
     * @param edgeType the process to use when deciding whether two data points
     *        are connected by an edge in the affinity matrix.
     * @param edgeTypeParam an optional parameter to the {@link EdgeType}
     *        selection process.  If the selected {@code EdgeType} does not take
     *        a parameter, this value is unused.
     * @param weighting the weighting scheme to use for edges in the affinity
     *        matrix
     * @param edgeWeightParam an optional parameter to the {@link EdgeWeighting}
     *        when deciding on the weighting for an edge.  If the selected
     *        {@code EdgeWeight} does not take a parameter, this value is
     *        unused.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    public static MatrixFile calculate(Matrix input,
                                       SimType dataSimilarityMetric,
                                       EdgeType edgeType,
                                       double edgeTypeParam,
                                       EdgeWeighting weighting,
                                       double edgeWeightParam) {
        try {
            File affMatrixFile = 
                File.createTempFile("affinty-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);
            
            int rows = input.rows();
            LOGGER.fine("Calculating the affinity matrix");
            switch (edgeType) {
            case NEAREST_NEIGHBORS: {
                RowComparator rc = new RowComparator();
                for (int i = 0; i < rows; ++i) {
                    LOGGER.fine("computing affinity for row " + i);
                    MultiMap<Double,Integer> neighborMap = 
                        rc.getMostSimilar(input, i, (int)edgeTypeParam, 
                                          dataSimilarityMetric);
                    Vector row = input.getRowVector(i);
                    for (int n : neighborMap.values()) {
                        double edgeWeight = 
                            getWeight(row, input.getRowVector(n),
                                      weighting, edgeWeightParam);
                        affMatrixWriter.println((i + 1) +  " " 
                                                + (n + 1) + " " +edgeWeight);
                    }
                }
                break;
            }
            case MIN_SIMILARITY: {
                for (int i = 0; i < rows; ++i) {
                    LOGGER.fine("computing affinity for row " + i);
                    Vector row1 = input.getRowVector(i);
                    // NOTE: we can compute the upper triangular and report the
                    // symmetric values.
                    for (int j = i+1; j < rows; ++j) {
                        Vector row2 = input.getRowVector(j);

                        double dataSimilarity = Similarity.getSimilarity(
                            dataSimilarityMetric, row1, row2);

                        if (dataSimilarity > edgeTypeParam) {
                            double edgeWeight = 
                                getWeight(row1, row2, 
                                          weighting, edgeWeightParam);
                            // Print out the symmetric edges
                            affMatrixWriter.println(
                                (i + 1) + " " + (j + 1) + " " + edgeWeight);
                            affMatrixWriter.println(
                                (j + 1) + " " + (i + 1) + " " + edgeWeight);
                        }
                    }
                }
                break;
            }
            default:
                assert false : 
                "Cannot construct matrix due to unknown edge type: " + edgeType;
            }

            // Finish writing the affinity matrix so it can be sent to matlab
            affMatrixWriter.close();    
            return new MatrixFile(affMatrixFile, MatrixIO.Format.MATLAB_SPARSE);

        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, returning the result as a file on disk.
     * The similarity comparisons are all performed off-core, which ensures that
     * the input matrix does not need to be loaded in memory in its entirety in
     * order to compute the affinity matrix.
     *
     * @param dataSimilarityMetric the metric by which two data points should be
     *        compared when constructing the affinity matrix.
     * @param edgeType the process to use when deciding whether two data points
     *        are connected by an edge in the affinity matrix.
     * @param edgeTypeParam an optional parameter to the {@link EdgeType}
     *        selection process.  If the selected {@code EdgeType} does not take
     *        a parameter, this value is unused.
     * @param weighting the weighting scheme to use for edges in the affinity
     *        matrix
     * @param edgeWeightParam an optional parameter to the {@link EdgeWeighting}
     *        when deciding on the weighting for an edge.  If the selected
     *        {@code EdgeWeight} does not take a parameter, this value is
     *        unused.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    public static MatrixFile calculate(MatrixFile input, 
                                       SimType dataSimilarityMetric,
                                       EdgeType edgeType,
                                       double edgeTypeParam,
                                       EdgeWeighting weighting,
                                       double edgeWeightParam) {
        return calculate(input, dataSimilarityMetric, edgeType, edgeTypeParam,
                         weighting, edgeWeightParam, false);
    }

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, optionally treating the columns as data
     * points, and returning the result as a file on disk.  The similarity
     * comparisons are all performed off-core, which ensures that the input
     * matrix does not need to be loaded in memory in its entirety in order to
     * compute the affinity matrix.
     *
     * <p>This method is primiarily intended to support computing the affinity
     * matrix for the {@code SVDLIBC_SPARSE_BINARY} format which is stored in
     * column-major order.
     *
     * @param dataSimilarityMetric the metric by which two data points should be
     *        compared when constructing the affinity matrix.
     * @param edgeType the process to use when deciding whether two data points
     *        are connected by an edge in the affinity matrix.
     * @param edgeTypeParam an optional parameter to the {@link EdgeType}
     *        selection process.  If the selected {@code EdgeType} does not take
     *        a parameter, this value is unused.
     * @param weighting the weighting scheme to use for edges in the affinity
     *        matrix
     * @param edgeWeightParam an optional parameter to the {@link EdgeWeighting}
     *        when deciding on the weighting for an edge.  If the selected
     *        {@code EdgeWeight} does not take a parameter, this value is
     *        unused.
     * @param useColumns {@code true} if the affinity matrix should be
     *        calculated for the <i>columns</i> of the input matrix, not the
     *        rows.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    public static MatrixFile calculate(MatrixFile input, 
                                       SimType dataSimilarityMetric,
                                       EdgeType edgeType,
                                       double edgeTypeParam,
                                       EdgeWeighting weighting,
                                       double edgeWeightParam,
                                       boolean useColumns) {
        File matrixFile = input.getFile();
        MatrixIO.Format format = input.getFormat();

        // IMPLEMENTATION NOTE: since the user has requested the matrix be dealt
        // with as a file, we need to keep the matrix on disk.  However, the
        // input matrix format may not be conducive to efficiently comparing
        // rows with each other (e.g. MATLAB_SPARSE is inefficient), so convert
        // the matrix to a better format.
        try {
            LOGGER.fine("Converting input matrix to new format for faster " +
                        "calculation of the affinity matrix");
            // Keep the matrix on disk, but convert it to a transposed SVDLIBC
            // sparse binary, which allows for easier efficient row-by-row
            // comparisons (which are really columns).  Note that if the data is
            // already in this format, the conversion is a no-op.
            //
            // NOTE: the !useColumns is used for the transpose because if we
            // want to use the rows, we need the data transposed to begin with
            // since the SVDLIBC sparse binary will give us column information
            // to start with
            File converted = 
                MatrixIO.convertFormat(matrixFile, format, 
                                       MatrixIO.Format.SVDLIBC_SPARSE_BINARY,
                                       !useColumns);
            LOGGER.fine("Calculating the affinity matrix");
            // Read off the matrix dimensions
            DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(converted)));
            // CRITICAL NOTE: because we are interpreting the columns as rows,
            // the dimensions are read in *reverse order* from how they are
            // stored in the file.
            int cols = dis.readInt();
            int rows = dis.readInt();
            dis.close();
            
            // Once we know the matrix dimensions, create an iterator over the
            // data, and repeatedly loop through the columns (which are really
            // rows in the original matrix) to create the affinity matrix.
            File affMatrixFile = File.createTempFile("affinity-matrix",".dat");
            PrintWriter affMatrixWriter = new PrintWriter(affMatrixFile);

            // Keep track of the first row and have a reference to the next row.
            // The nextRow reference avoid us having to advance into data
            // unnecessarily to retrieval the vector for processing to start
            SparseDoubleVector curRow = null;
            SparseDoubleVector nextRow = null;

            SvdlibcSparseBinaryFileRowIterator matrixIter = 
                new SvdlibcSparseBinaryFileRowIterator(converted);
            
            for (int row = 0; row < rows; ++row) {
                LOGGER.fine("computing affinity for row " + row);

                // This map is only used if k-nearest neighbors option is being
                // used.  The map is to the row and its weighted affinity
                // value.  We need to store the potential value at the time of
                // the similarity calculation because that is the only time the
                // two row vectors are in memory
                int k = (edgeType.equals(EdgeType.NEAREST_NEIGHBORS))
                    ? (int)edgeTypeParam : 1;
                MultiMap<Double,Duple<Integer,Double>> neighbors = 
                    new BoundedSortedMultiMap<Double,Duple<Integer,Double>>(
                        k, false);

                // Loop through each of the rows, gathering the statistics
                // necessary to compute the affinity matrix.
                for (int other = 0; other < rows; ++other) {
                    //System.out.printf("cur: %d, other %d%n", row, other);
                    // Special case for the very first row
                    if (row == 0 && curRow == null) {
                        curRow = matrixIter.next();
                        continue;
                    }
                    
                    SparseDoubleVector otherRow = matrixIter.next();
                    // Special case for the similarity threshold, which is
                    // symmetric.  In this case, we can skip over processing any
                    // rows that occur before the current row
                    if (edgeType.equals(EdgeType.MIN_SIMILARITY)
                            && other < row) 
                        continue;

                    // Save the row that will be used next so we have it to do
                    // comparisons with for earlier rows in the file
                    if (other == row + 1)
                        nextRow = otherRow;

                    // Determine if the current row and the other row should be
                    // linked in the affinity matrix.  For code simplicity, both
                    // the k-nearest neighbors and the similarity threshold code
                    // are supported within the I/O, with the caller specifying
                    // which to use.
                    double dataSimilarity = Similarity.getSimilarity(
                        dataSimilarityMetric, curRow, otherRow);
                    
                    switch (edgeType) {
                    case NEAREST_NEIGHBORS: {
                        double edgeWeight = 
                            getWeight(curRow, otherRow, 
                                      weighting, edgeWeightParam);
                        neighbors.put(dataSimilarity,
                            new Duple<Integer,Double>(other, edgeWeight));
                        break;
                    }
                    // Use the similarity threshold to decide if the rows are
                    // linked
                    case MIN_SIMILARITY: {
                        if (dataSimilarity > edgeTypeParam) {
                            double edgeWeight = 
                                getWeight(curRow, otherRow, 
                                          weighting, edgeWeightParam);
                            // Print out the symmetric edges
                            affMatrixWriter.println(
                                (row + 1) + " " + (other + 1)
                                + " " + edgeWeight);
                            affMatrixWriter.println(
                                (other + 1) + " " + (row + 1)
                                + " " + edgeWeight);
                        }
                        break;
                    }
                    default:
                        assert false : "unhandled edge type: " + edgeType;
                    }
                }
                curRow = nextRow;
                if (edgeType.equals(EdgeType.NEAREST_NEIGHBORS)) {
                    // If using k-nearest neighbors, once the row has been
                    // processed, report all the k-nearest as being adjacent
                    for (Duple<Integer,Double> t : neighbors.values()) {
                        // Note that the two rows may not have a symmetric
                        // connection so only one value needs to be written
                        affMatrixWriter.println((row + 1) + " " + (t.x + 1) + " " + t.y);
                    }
                }
                matrixIter.reset();
            }
            // Finish writing the matrix
            affMatrixWriter.close();
            return new MatrixFile(affMatrixFile, MatrixIO.Format.MATLAB_SPARSE);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns the Gaussing kernel weighting of the two vectors using the
     * parameter to weight the distance between the two vectors.
     */
    private static double gaussianKernel(Vector v1, Vector v2, 
                                         double gaussianKernelParam) {
        double euclideanDist = Similarity.euclideanDistance(v1, v2);
        return Math.pow(Math.E, -(euclideanDist / gaussianKernelParam));
    }

    /**
     * Returns the Gaussing kernel weighting of the two vectors using the
     * parameter to specify the degree of the polynomial.
     *
     * @param degree the degree of the polynomial
     */
    private static double polynomialKernel(Vector v1, Vector v2, 
                                           double degree) {
        double dotProduct = VectorMath.dotProduct(v1, v2);
        return Math.pow(dotProduct + 1, degree);
    }

    /**
     * Returns the weight of the connection from {@code x} to {@code y}.
     *
     * @param x the first vector that is connected
     * @param y the vector that is connected to {@code x}
     * @param w the method to use in deciding the weight value
     * @param param an optional parameter to use in weighting
     *
     * @return the edge weight
     */
    private static double getWeight(Vector x, Vector y, 
                                    EdgeWeighting w, double param) {
        switch (w) {
        case BINARY:
            return 1;

        case GAUSSIAN_KERNEL:
            return gaussianKernel(x, y, param);

        case POLYNOMIAL_KERNEL:
            return polynomialKernel(x, y, param);

        case DOT_PRODUCT:
            return VectorMath.dotProduct(x, y);

        case COSINE_SIMILARITY:
            return Similarity.cosineSimilarity(x, y);
        default:
            assert false : "unhandled edge weighting type: " + w;
        }
        throw new IllegalArgumentException(
            "unhandled edge weighting type: " + w);
    }

}
