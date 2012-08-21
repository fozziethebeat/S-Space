/*
 * Copyright 2011 Keith Stevens 
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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * The return value for all {@link Clustering} implementations.  This class
 * records the number of clusters created, the assignments for each value, and
 * helper methods for constructing the centroids of a cluster.
 *
 * @author Keith Stevens
 */
public class Assignments implements Iterable<int[]> {

    /**
     * The {@link Assignment}s made for each data point.
     */
    private int[][] assignments;

    /**
     * The number of clusters found from a particular algorithm.
     */
    private int numClusters;

    /**
     * The {@link Matrix} of data points that these {@link Assignments} link to.
     */
    private Matrix matrix;

    private int[] counts;

    /**
     * Creates a new {@link Assignments} instance that can hold up to {@code
     * numAssignments} {@link Assignment}s.  This assumes that the data matrix
     * will not be accessible.  Calls to {@link #getCentroids} will fail when
     * using this constructor.
     */
    public Assignments(int numClusters, int numAssignments) {
        this(numClusters, numAssignments, null);
    }

    /**
     * Creates a new {@link Assignments} instance that can hold up to {@code
     * numAssignments} {@link Assignment}s.
     */
    public Assignments(int numClusters, int numAssignments, Matrix matrix) {
        this.numClusters = numClusters;
        this.matrix = matrix;
        assignments = new int[numAssignments][1];
    }

    /**
     * Creates a new {@link Assignments} instance that takes ownership of the
     * {@code initialAssignments} array.  This assumes that the data matrix will
     * not be accessible.  Calls to {@link #getCentroids} will fail when using
     * this constructor.
     */
    public Assignments(int numClusters,
                       int[][] initialAssignments) {
        this(numClusters, initialAssignments, null);
    }

    public Assignments(int numClusters,
                       int[] initialAssignments, 
                       Matrix matrix) {
        this.numClusters = numClusters;
        this.matrix = matrix;
        assignments = new int[initialAssignments.length][1];
        for (int i = 0; i < initialAssignments.length; ++i)
            set(i, initialAssignments[i]);
    }

    /**
     * Creates a new {@link Assignments} instance that takes ownership of the
     * {@code initialAssignments} array.
     */
    public Assignments(int numClusters,
                       int[][] initialAssignments, 
                       Matrix matrix) {
        this.numClusters = numClusters;
        this.matrix = matrix;
        assignments = initialAssignments;
    }

    /**
     * Sets {@link Assignment} {@code i} to have value {@code assignment}.
     */
    public void set(int i, int id) {
        assignments[i] = new int[1];
        assignments[i][0] = id;
    }

    /**
     * Sets {@link Assignment} {@code i} to have value {@code assignment}.
     */
    public void setAll(int i, int[] ids) {
        if (ids.length == 0)
            throw new IllegalArgumentException(
                    "Cannot have an empty assignment.  " +
                    "Use a negative cluster id instead");

        assignments[i] = ids;
    }

    public void setAll(int i, Collection<Integer> ids) {
        if (ids.size() == 0)
            throw new IllegalArgumentException(
                    "Cannot have an empty assignment.  " +
                    "Use a negative cluster id instead");

        assignments[i] = new int[ids.size()];
        Iterator<Integer> it = ids.iterator();
        for (int j = 0; j < assignments.length; ++j)
            assignments[i][j] = it.next();
    }

    public void setAll(int i, Integer... ids) {
        if (ids.length  == 0)
            throw new IllegalArgumentException(
                    "Cannot have an empty assignment.  " +
                    "Use a negative cluster id instead");

        assignments[i] = new int[ids.length];
        for (int j = 0; j < ids.length; ++j)
            assignments[i][j] = ids[j];
    }

    /**
     * Returns the number of {@link Assignment} objects stored.
     */
    public int size() {
        return assignments.length;
    }

    /**
     * Returns an iterator over the {@link Assignment} objects stored.
     */
    public Iterator<int[]> iterator() {
        return Arrays.asList(assignments).iterator();
    }

    /**
     * Returns the {@link Assignment} object at index {@code i}.
     */
    public int get(int i) {
        return assignments[i][0];
    }

    /**
     * Returns the {@link Assignment} object at index {@code i}.
     */
    public int[] getAll(int i) {
        return assignments[i];
    }

    /**
     * Returns the number of clusters.
     */
    public int numClusters() {
        return numClusters;
    }

    /**
     * Returns the array of {@link Assignment} objects.
     */
    public int[][] assignments() {
        return assignments;
    }

    /**
     * Returns the data point indices assigned to each cluster.
     */
    public List<Set<Integer>> clusters() {
        List<Set<Integer>> clusters = new ArrayList<Set<Integer>>();
        for (int c = 0; c < numClusters; ++c)
            clusters.add(new HashSet<Integer>());
        for (int i = 0; i < assignments.length; ++i)
            for (int id : assignments[i])
                clusters.get(id).add(i);
        return clusters;
    }

    /**
     * Returns an array of dense centroid vectors of each discovered cluster
     * which are scaled according the the number of data points asisgned to that
     * cluster.  Note that this method assumes that the original {@link Matrix}
     * holding the data points contains rows of feature vectors.  
     */
    public DoubleVector[] getCentroids() {
        if (matrix == null)
            throw new IllegalArgumentException(
                    "The data matrix was not passed to Assignments.");

        // Initialzie the centroid vectors and the cluster sizes.
        DoubleVector[] centroids = new DoubleVector[numClusters];
        counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseVector(matrix.columns());

        // For each initial assignment, add the vector to it's centroid and
        // increase the size of the cluster.
        for (int row = 0; row < assignments.length; ++row) {
            int id = get(row);
            counts[id]++;
            DoubleVector centroid = centroids[id];
            VectorMath.add(centroid, matrix.getRowVector(row));
        }

        // Scale any non empty clusters by their size.
        for (int c = 0; c < numClusters; ++c)
            if (counts[c] != 0)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c],1d/counts[c]);

        return centroids;
    }

    /**
     * Returns an array of sparse centroid vectors of each discovered cluster
     * which are scaled according the the number of data points asisgned to that
     * cluster.  This assumes that the original {@link Matrix} is sparse.  Note
     * that this method assumes that the original {@link Matrix} holding the
     * data points contains rows of feature vectors.  
     */
    public SparseDoubleVector[] getSparseCentroids() {
        if (matrix == null)
            throw new IllegalArgumentException(
                    "The data matrix was not passed to Assignments.");

        SparseMatrix sm = (SparseMatrix) matrix;

        // Initialzie the centroid vectors and the cluster sizes.
        SparseDoubleVector[] centroids = new SparseDoubleVector[numClusters];

        // If for some odd reason, no clusters were found, return no centroids.
        if (numClusters == 0)
            return centroids;

        counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new CompactSparseVector(matrix.columns());

        // For each initial assignment, add the vector to it's centroid and
        // increase the size of the cluster.
        for (int row = 0; row < assignments.length; ++row) {
            int id = get(row);
            counts[id]++;
            DoubleVector centroid = centroids[id];
            VectorMath.add(centroid, sm.getRowVector(row));
        }

        // Scale any non empty clusters by their size.
        for (int c = 0; c < numClusters; ++c)
            if (counts[c] != 0)
                centroids[c] = new ScaledSparseDoubleVector(
                        centroids[c],1d/counts[c]);

        return centroids;
    }

    public int[] clusterSizes() {
        return counts;
    }
}
