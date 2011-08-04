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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.io.Serializable;

import java.util.Properties;
import java.util.Random;


/**
 * A Generator for vectors where each entry is from a guassian distribution
 * having some mean and standard deviation.  This class supports the following
 * properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #STANDARD_DEVIATION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_STANDARD_DEVIATION }
 *
 * <dd style="padding-top: .5em">This variable is the standard deviation used
 * when generating random numbers from a gaussian distribution</p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #MEAN_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_MEAN}
 *
 * <dd style="padding-top: .5em">This variable is the mean used for generating
 * values from gaussian distribution for vectors</p>
 *
 * </dl>
 *
 * @author Keith Stevens
 */
public class GaussianVectorGenerator
        implements DoubleVectorGenerator<DoubleVector>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The base property prefix.
     */
    private static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.index.GuassianVectorGenerator";

    /**
     * The property for setting the standard deviation.
     */
    public static final String STANDARD_DEVIATION_PROPERTY =
        PROPERTY_PREFIX + ".stdev";

    /**
     * The property for setting the standard deviation.
     */
    public static final String MEAN_PROPERTY =
        PROPERTY_PREFIX + ".mean";

    public static final double DEFAULT_STANDARD_DEVIATION = 1;

    public static final String DEFAULT_MEAN = "0";

    /**
     * The standard deviation used for generating a new index vector for terms.
     */
    private double stdev;

    private final int indexVectorLength;

    /**
     * The mean used each element in an generated {@link Vector}.
     */
    private double mean;

    /**
     * A random number generator which produces values for index vectors.
     */
    private Random randomGenerator;

    /**
     * Create a {@code GaussianVectorGenerator} that uses the system properties
     * for setup.
     */
    public GaussianVectorGenerator(int indexVectorLength) {
        this(indexVectorLength, System.getProperties());
    }

    /**
     * Create a {@code GaussianVectorGenerator} which uses {@code
     * vectorLength} as the size of each generated {@code Vector}.
     *
     * @param vectorLength The length of each index and semantic {@code Vector}
     *                     used in this {@code IndexVectorGenerator}.
     */
    public GaussianVectorGenerator(int indexVectorLength, Properties prop) {
        // Generate utility classes.
        randomGenerator = new Random();

        this.indexVectorLength = indexVectorLength;

        String stdevProp = prop.getProperty(
                STANDARD_DEVIATION_PROPERTY);
        stdev = (stdevProp != null)
            ? Double.parseDouble(stdevProp)
            : DEFAULT_STANDARD_DEVIATION;
        mean = Double.parseDouble(prop.getProperty(
                    MEAN_PROPERTY, DEFAULT_MEAN));
    }

    /**
     * Generate a new random vector using a guassian distribution for each
     * value.
     */
    public synchronized DoubleVector generate() {
        DoubleVector termVector = new DenseVector(indexVectorLength);
        for (int i = 0; i < indexVectorLength; i++)
            termVector.set(i, mean + (randomGenerator.nextGaussian() * stdev));
        return termVector;
    }
}
