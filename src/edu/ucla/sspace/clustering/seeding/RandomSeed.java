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

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.BitSet;


/**
 * This {@link KMeansSeed} implementation selects data points at random from any
 * given data set to serve as the initial centroid seeds.
 *
 * @author Keith Stevens
 */
public class RandomSeed implements KMeansSeed {

    /**
     * {@inheritDoc}
     */
    public DoubleVector[] chooseSeeds(int numCentroids, Matrix dataPoints) {
        DoubleVector[] centers = new DoubleVector[numCentroids];

        // In the random chance that there are fewer data points than seeds,
        // simply set the data points as the seeds.
        if (numCentroids >= dataPoints.rows()) {
            for (int i = 0; i < dataPoints.rows(); ++i)
                centers[i] = dataPoints.getRowVector(i);

            // For any extra slots that do not have a data point, use an empty
            // vector, which will have zero similarity with any other data
            // point.
            for (int i = dataPoints.rows(); i < numCentroids; ++i)
                centers[i] = new DenseVector(dataPoints.columns());

            return centers;
        }

        // Select a subset of data points to be the new centroids.
        BitSet selectedCentroids = Statistics.randomDistribution(
                numCentroids, dataPoints.rows());

        // Convert the selection indices into vectors.
        for (int c = 0, i = selectedCentroids.nextSetBit(0); i >= 0;
                c++, i = selectedCentroids.nextSetBit(i+1))
            centers[c] = dataPoints.getRowVector(i);

        return centers;
    }
}
