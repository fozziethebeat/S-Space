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

package edu.ucla.sspace.clustering.seeding;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;

/**
 * This {@link KMeansSeed} implementation takes in a text file that contains a
 * clustering assignment for each data point in the data set.  Based on these
 * assignments, {@link OptimalSeed} will compose centroids by summing the data
 * points assigned to each cluster and then scaling the centroid by the number
 * of assigned data points.
 *
 * </p>
 *
 * The data file should have one cluster assignment per line, with cluster ids
 * starting from 0 and going up to numClusters - 1.
 *
 * @author Keith Stevens
 */
public class OptimalSeed implements KMeansSeed {

    /**
     * A base for all properties.
     */
    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.clustering.seeding.OptimalSeed";

    /**
     * The property for setting the assignment file.
     */
    public static final String ASSIGNMENT_FILE_PROPERTY =
        PROPERTY_PREFIX + ".assignmentFile";

    /**
     * The assignment file which stores a known assignment for each data point
     * that will be encountered by this seeding class.
     */
    private File assignmentFile;

    /**
     * Constructs an {@link OptimalSeed} instance using the value set for the
     * {@link #ASSIGNMENT_FILE_PROPERTY} property.
     */
    public OptimalSeed() {
        this(new File(System.getProperty(ASSIGNMENT_FILE_PROPERTY)));
    }

    /**
     * Constructs an {@link OptimalSeed} instance using the values in {@code
     * assignmentFile}.
     */
    public OptimalSeed(File assignmentFile) {
        this.assignmentFile = assignmentFile;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector[] chooseSeeds(int numCentroids, Matrix dataPoints) {
        // Initialize the centroids.
        DoubleVector[] centroids = new DoubleVector[numCentroids];
        for (int i = 0; i < numCentroids; ++i)
            centroids[i] = new DenseVector(dataPoints.columns());

        try {
            // Read each line in the assignment file and compose the centroid
            // for each computed centroid.
            double[] clusterSizes = new double[numCentroids];
            BufferedReader br = new BufferedReader(new FileReader(
                        assignmentFile));
            int i = 0;
            for (String line = null; (line = br.readLine()) != null; ) {
                int assignment = Integer.parseInt(line.trim());

                // Inspect for any out of bounds assignments.
                if (assignment >= numCentroids)
                    throw new IllegalArgumentException(
                            "Cluster ids for the assignment file must " +
                            "be less than the number of clusters");

                // Add the associated data point to it's assigned centroid.
                VectorMath.add(centroids[assignment],
                               dataPoints.getRowVector(i++));
                clusterSizes[assignment]++;
            }

            // Scale each centroid by the number of data points assigned to it.
            for (int c = 0; c < numCentroids; ++c)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c], 1/clusterSizes[c]);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return centroids;
    }
}
