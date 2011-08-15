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
import java.util.Iterator;
import java.util.List;

/**
 * The return value for all {@link Clustering} implementations.  This class
 * records the number of clusters created, the assignments for each value, and
 * helper methods for constructing the centroids of a cluster.
 *
 * @author Keith Stevens
 */
public class Assignments implements Iterable<Assignment> {

    /**
     * The {@link Assignment}s made for each data point.
     */
    private Assignment[] assignments;

    /**
     * The number of clusters found from a particular algorithm.
     */
    private int numClusters;

    /**
     * Creates a new {@link Assignments} instance that can hold up to {@code
     * numAssignments} {@link Assignment}s.
     */
    public Assignments(int numClusters, int numAssignments) {
        this.numClusters = numClusters;
        assignments = new Assignment[numAssignments];
    }

    /**
     * Creates a new {@link Assignments} instance that takes ownership of the
     * {@code initialAssignments} array.
     */
    public Assignments(int numClusters, Assignment[] initialAssignments) {
        this.numClusters = numClusters;
        assignments = initialAssignments;
    }

    /**
     * Sets {@link Assignment} {@code i} to have value {@code assignment}.
     */
    public void set(int i, Assignment assignment) {
        assignments[i] = assignment;
    }

    /**
     * Returns the number of {@link Assignment} objects stored.
     */
    public int length() {
        return assignments.length;
    }

    /**
     * Returns an iterator over the {@link Assignment} objects stored.
     */
    public Iterator<Assignment> iterator() {
        return new ArrayIterator();
    }

    /**
     * Returns the {@link Assignment} object at index {@code i}.
     */
    public Assignment get(int i) {
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
    public Assignment[] assignments() {
        return assignments;
    }

    /**
     * Returns an array of dense centroid vectors based on the data points in
     * {@code dataMatrix} and the assignments given so far.  The number of rows
     * in {@code dataMatrix} must be equal to the length of this {@link
     * Assignments} instance.
     */
    public DoubleVector[] getCentroids(Matrix dataMatrix) {
        // Initialzie the centroid vectors and the cluster sizes.
        DoubleVector[] centroids = new DoubleVector[numClusters];
        int[] counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseVector(dataMatrix.columns());

        // For each initial assignment, add the vector to it's centroid and
        // increase the size of the cluster.
        int row = 0;
        for (Assignment assignment : assignments) {
            if (assignment.length() != 0) {
                counts[assignment.assignments()[0]]++;
                DoubleVector centroid = centroids[assignment.assignments()[0]];
                VectorMath.add(centroid, dataMatrix.getRowVector(row));
            }
            row++;
        }

        // Scale any non empty clusters by their size.
        for (int c = 0; c < numClusters; ++c)
            if (counts[c] != 0)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c],1d/counts[c]);

        return centroids;
    }

    /**
     * Returns an array of sparse centroid vectors based on the data points in
     * {@code dataMatrix} and the assignments given so far.  The number of rows
     * in {@code dataMatrix} must be equal to the length of this {@link
     * Assignments} instance.
     */
    public SparseDoubleVector[] getCentroids(SparseMatrix dataMatrix) {
        // Initialzie the centroid vectors and the cluster sizes.
        SparseDoubleVector[] centroids = new SparseDoubleVector[numClusters];

        // If for some odd reason, no clusters were found, return no centroids.
        if (numClusters == 0)
            return centroids;

        int[] counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new CompactSparseVector(dataMatrix.columns());

        // For each initial assignment, add the vector to it's centroid and
        // increase the size of the cluster.
        int row = 0;
        for (Assignment assignment : assignments) {
            if (assignment.length() != 0) {
                counts[assignment.assignments()[0]]++;
                SparseDoubleVector centroid =
                    centroids[assignment.assignments()[0]];
                VectorMath.add(centroid, dataMatrix.getRowVector(row));
            }
            row++;
        }

        // Scale any non empty clusters by their size.
        for (int c = 0; c < numClusters; ++c)
            if (counts[c] > 0)
                centroids[c] = new ScaledSparseDoubleVector(
                        centroids[c], 1d/counts[c]);

        return centroids;
    }

    /**
     * An internal {@link Iterator} for accessing {@link Assignment} objects.
     */
    private class ArrayIterator implements Iterator<Assignment> {

        /**
         * The current index of the iterator.
         */
        int index;

        /**
         * Creates a new {@link ArrayIterator} that starts at index 0.
         */
        public ArrayIterator() {
            index = 0;
        }

        /**
         * Unsupported.
         */
        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove from an ArrayIterator");
        }

        /**
         * {@inheritDoc}
         */
        public boolean hasNext() {
            return index < assignments.length;
        }

        /**
         * {@inheritDoc}
         */
        public Assignment next() {
            return assignments[index++];
        }
    }
}
