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

import edu.ucla.sspace.vector.DoubleVector;


/**
 * An interface for KMeans seeding algorithms.   Implementations must compose
 * initial centroid seeds from a data set by either choosing an already existing
 * data point or composing a linear combination of existing data points.
 *
 * </p>
 *
 * Implementations must be state free and threadsafe.
 *
 * @author Keith Stevens
 */
public interface KMeansSeed {

    /**
     * Returns an array of length {@code numCentroids} that contains centroids
     * composed of either vectors from {@code dataPoints} or a linar combination
     * of vectors from {@code dataPoints}.
     */
    DoubleVector[] chooseSeeds(int numCentroids, Matrix dataPoints);
}
