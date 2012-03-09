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

import edu.ucla.sspace.similarity.SimilarityFunction;


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
public interface AffinityMatrixCreator {

    /**
     * Sets any numeric parameters, such as thresholds, for this {@link
     * AffinityMatrixCreator}.  Each {@link AffinityMatrixCreator} should
     * specify how many parameters it expects and in what order.
     */
    void setParams(double... params);

    /**
     * Sets the {@link SimilarityFunction}s to be used for selecting edges and
     * then weighting edges.  {@code edgeSim} will be computed over every
     * possible edge.  The returned scores will be used to filter out valid
     * edges in the graph.  {@code kernelSim} will be used to compute the final
     * edge weight between two valid edges in the graph, this is also called a
     * kernel function.
     *
     * @param edgeSim The {@link SimilarityFunction} used to filter edges.  This
     *        metric is assumed to be symmetric.
     * @param kernelSim The {@link SimilarityFunction} used to weight valid
     *        edges.  This metric may or may not be symmetric.
     */
    void setFunctions(SimilarityFunction edgeSim, SimilarityFunction kernelSim);

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, returning the result as a file on disk.
     * Because the input matrix is provided already in memory, all similarity
     * comparisons are performed in memory, which offers a significant speed-up
     * compared to the off-core overloads of this method that operate on a
     * {@link MatrixFile} as input.
     *
     * @param input The {@link Matrix} that contains data points that are to
     *         be formed into an affinity matrix.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    MatrixFile calculate(Matrix input);

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, returning the result as a file on disk.
     * The similarity comparisons are all performed off-core, which ensures that
     * the input matrix does not need to be loaded in memory in its entirety in
     * order to compute the affinity matrix.
     *
     * @param input The {@link MatrixFile} that contains data points that are to
     *         be formed into an affinity matrix.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    MatrixFile calculate(MatrixFile input);

    /**
     * Computes the affinity matrix for the input matrix according to the
     * specified similarity metrics, optionally treating the columns as data
     * points, and returning the result as a file on disk.  The similarity
     * comparisons are all performed off-core, which ensures that the input
     * matrix does not need to be loaded in memory in its entirety in order to
     * compute the affinity matrix.
     *
     * </p>
     *
     * This method is primiarily intended to support computing the affinity
     * matrix for the {@code SVDLIBC_SPARSE_BINARY} format which is stored in
     * column-major order.
     *
     * @param input The {@link MatrixFile} that contains data points that are to
     *         be formed into an affinity matrix.
     * @param useColumns {@code true} if the affinity matrix should be
     *        calculated for the <i>columns</i> of the input matrix, not the
     *        rows.
     *
     * @return the affinity matrix as a file on disk.  The result may be read
     *         back into memory with {@code MatrixFile.read()}.
     */
    MatrixFile calculate(MatrixFile input, boolean useColumns);
}
