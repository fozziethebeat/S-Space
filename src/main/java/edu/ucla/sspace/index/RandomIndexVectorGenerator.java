/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.IntegerVector;

import java.io.Serializable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.Set;


/**
 * An class that generates {@link RandomTernaryVector} instances based on
 * configurable properties.  This class supports two properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #VALUES_TO_SET_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_INDEX_VECTOR_VALUES}
 *
 * <dd style="padding-top: .5em">This variable sets the number of bits to set in
 *      an index vector. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #INDEX_VECTOR_VARIANCE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_INDEX_VECTOR_VARIANCE}
 *
 * <dd style="padding-top: .5em">This variable sets the variance in the number
 *      of bits to set in an index vector.  For example, having {@value
 *      #VALUES_TO_SET_PROPERTY}{@code =4} and setting this property to {@code
 *      2} would mean that {@code 4 &plusmn; 2} value would be randomly set in
 *      each index vector. <p>
 *
 * </dl>
 */
public class RandomIndexVectorGenerator
        implements IntegerVectorGenerator<TernaryVector>, Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * A random number generator that can be accessed to other classes which
     * will rely on the same source of random values.
     */
    public static final Random RANDOM = new Random();

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.index.RandomIndexVectorGenerator";

    /**
     * The property to specify the number of values to set in an {@link
     * TernaryVector}.
     */
    public static final String VALUES_TO_SET_PROPERTY = 
        PROPERTY_PREFIX + ".values";

    /**
     * The property to specify the variance in the number of values to set in an
     * {@link TernaryVector}.
     */
    public static final String INDEX_VECTOR_VARIANCE_PROPERTY = 
        PROPERTY_PREFIX + ".variance";

    /**
     * The default number of values to set in an {@link TernaryVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_VALUES = 4;

    /**
     * The default number of dimensions to create in each {@code TernaryVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_LENGTH = 20000;

    /**
     * The default random variance in the number of values that are set in an
     * {@code TernaryVector}.
     */
    public static final int DEFAULT_INDEX_VECTOR_VARIANCE = 0;

    /**
     * The number of values to set in an {@link TernaryVector}.
     */
    private int numVectorValues;

    /**
     * The variance in the number of values that are set in an {@code
     * TernaryVector}.
     */
    private int variance;

    private int indexVectorLength;

    /**
     * Constructs this instance using the system properties.
     */
    public RandomIndexVectorGenerator(int indexVectorLength) {
        this(indexVectorLength, System.getProperties());
    }

    /**
     * Constructs this instance using the provided properties.
     */
    public RandomIndexVectorGenerator(int indexVectorLength,
                                      Properties properties) {
        this.indexVectorLength = indexVectorLength;

        String numVectorValuesProp = 
            properties.getProperty(VALUES_TO_SET_PROPERTY);
        numVectorValues = (numVectorValuesProp != null)
            ? Integer.parseInt(numVectorValuesProp)
            : DEFAULT_INDEX_VECTOR_VALUES;

        String varianceProp =
            properties.getProperty(INDEX_VECTOR_VARIANCE_PROPERTY);
        variance = (varianceProp != null)
            ? Integer.parseInt(varianceProp)
            : DEFAULT_INDEX_VECTOR_VARIANCE;
    }

    /**
     * Creates an {@code TernaryVector} with the provided length.
     *
     * @param length the length of the index vector
     *
     * @return an index vector
     */
    public TernaryVector generate() {
        HashSet<Integer> pos = new HashSet<Integer>();
        HashSet<Integer> neg = new HashSet<Integer>();
        
        // Randomly decide how many bits to set in the index vector based on the
        // variance.
        int bitsToSet = numVectorValues +
            (int)(RANDOM.nextDouble() * variance *
                  ((RANDOM.nextDouble() > .5) ? 1 : -1));

        for (int i = 0; i < bitsToSet; ++i) {
            boolean picked = false;
            // loop to ensure we actually pick the full number of bits
            while (!picked) {
                // pick some random index
                int index = RANDOM.nextInt(indexVectorLength);
                    
                // check that we haven't already added this index
                if (pos.contains(index) || neg.contains(index))
                    continue;
                    
                // decide positive or negative
                ((RANDOM.nextDouble() > .5) ? pos : neg).add(index);
                picked = true;
            }
        }
            
        int[] positive = new int[pos.size()];
        int[] negative = new int[neg.size()];

        Iterator<Integer> it = pos.iterator();
        for (int i = 0; i < positive.length; ++i) 
            positive[i] = it.next();

        it = neg.iterator();
        for (int i = 0; i < negative.length; ++i) 
            negative[i] = it.next();                

        // sort so we can use a binary search in getValue()
        Arrays.sort(positive);
        Arrays.sort(negative);
        return new TernaryVector(indexVectorLength, positive, negative);
    }
}
