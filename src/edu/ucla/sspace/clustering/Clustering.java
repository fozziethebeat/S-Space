/*
 * Copyright 2009 Keith Stevens 
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

import java.util.Properties;


/**
 * An interface for any Ofline clustering implementation.  This interface
 * supports hard and soft clustering by returning a {@link Assignment}.  A
 * {@link Matrix} is passed in where each row is to be considered the set of
 * data points to cluster.
 *
 * </p>
 *
 * Implementations should not modify the contents of a {@code Matrix} to
 * cluster.  Implementations may use a passed in properties object to support
 * additional parameters that for clustering.  By convention, implementations
 * should specify a set of default values such that the matrix rows can be
 * clustered if no properties are specified. 
 *
 * </p>
 *
 * If a clustering algorithm requires the number of clusters to be specified, an
 * implementation may throw an {@link UnsupportedOperationException} when the
 * number of clusters is not specified.
 *
 * @author Keith Stevens
 */
public interface Clustering {

    /**
     * Clusters the set of rows in the given {@code Matrix} without a specified
     * number of clusters (optional operation).  The set of cluster assignments
     * are returned for each row in the matrix.
     *
     * @param matrix the {@link Matrix} whose row data points are to be
     *        clustered
     * @param props the properties to use for any parameters each clustering
     *        algorithm may need 
     *
     * @return an array of {@link Assignment} instances that indicate zero or
     *         more clusters to which each row belongs.
     */
    Assignment[] cluster(Matrix matrix, Properties props);

    /**
     * Clusters the set of rows in the given {@code Matrix} into the specified
     * number of clusters.  The set of cluster assignments are returned for each
     * row in the matrix.
     *
     * @param matrix the {@link Matrix} whose row data points are to be
     *        clustered 
     * @param numClusters the number of clusters to generate
     * @param props the properties to use for any parameters each clustering
     *        algorithm may need 
     *
     * @return an array of {@link Assignment} instances that indicate zero or
     *         more clusters to which each row belongs.
     */
    Assignment[] cluster(Matrix matrix, int numClusters, Properties props);
}
