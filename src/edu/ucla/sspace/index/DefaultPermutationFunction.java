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

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.Serializable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * A generic permutation function.  This class precomputes the permutations as
 * necessary and only requires {@code O(k)} time to compute a single
 * permutation, where {@code k} is the number of non-zero elements in the {@code
 * Vector}.
 *
 * @author David Jurgens
 */
public class DefaultPermutationFunction
        implements PermutationFunction<Vector>, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Random RANDOM = RandomIndexVectorGenerator.RANDOM;

    /**
     * A mapping from a distance to a corresponding permutation.
     */
    private final Map<Integer, Function> permutationToReordering;
    
    /**
     * Creates an empty {@code DefaultPermutationFunction}.
     */
    public DefaultPermutationFunction() {
        permutationToReordering = new HashMap<Integer,Function>();
    }

    /**
     * Returns the bijective mapping for each integer in the form of an array
     * based on the the current exponent of the permutation.
     *
     * @param exponent the exponent for the current permutation 
     * @param dimensions the number of dimenensions in the index vector being
     *        permuted
     *
     * @return the mapping for each index to its new index
     */
    private Function getFunction(int exponent, int dimensions) {
        // Base case: we keep the same ordering.  Create this function on the
        // fly to save space, since the base case should rarely get called.
        if (exponent == 0) {
            int[] func = new int[dimensions];
            for (int i = 0; i < dimensions; ++i) {
                func[i] = i;
            }
            return new Function(func, func);
        }

        exponent = Math.abs(exponent);

        Function function = permutationToReordering.get(exponent);
        
        // If there wasn't a funcion for that exponent then created one by
        // permuting the lower exponents value.  Use recursion to access the
        // lower exponents value to ensure that any non-existent lower-exponent
        // functions are created along the way.
        if (function == null) {
            synchronized (this) {
                function = permutationToReordering.get(exponent);
                if (function == null) {
                    // lookup the prior function
                    int priorExponent = exponent - 1;
                    Function priorFunc =
                        getFunction(priorExponent, dimensions);
                    
                    // convert to an object based array to use
                    // Collections.shuffle()
                    Integer[] objFunc = new Integer[dimensions];
                    for (int i = 0; i < dimensions; ++i) {
                        objFunc[i] = Integer.valueOf(priorFunc.forward[i]);
                    }

                    // then shuffle it to get a new permutation
                    java.util.List<Integer> list = Arrays.asList(objFunc);
                    Collections.shuffle(list, RANDOM);
                    
                    // convert back to a primitive array
                    int[] forwardMapping = new int[dimensions];
                    int[] backwardMapping = new int[dimensions];
                    for (int i = 0; i < dimensions; ++i) {
                        forwardMapping[i] = objFunc[i].intValue();
                        backwardMapping[objFunc[i].intValue()] = i;
                    }            
                    function = new Function(forwardMapping, backwardMapping);
                    // store it in the function map for later usee
                    permutationToReordering.put(exponent, function);
                }
            }
        }

        return function;
    }

    /**
     * {@inheritDoc}
     */
    public Vector permute(Vector v , int numPermutations) {
        if (v instanceof TernaryVector)
            return permute((TernaryVector) v, numPermutations, v.length());

        Vector result = Vectors.instanceOf(v);
        int[] dimensions = null;
        int[] oldDims = null;
        if (v instanceof SparseVector) {
            oldDims = ((SparseVector) v).getNonZeroIndices();
            dimensions = Arrays.copyOf(oldDims, oldDims.length);
        } else {
            dimensions = new int[v.length()];
            for (int i = 0; i < v.length(); ++i)
                dimensions[i] = i;
        }

        boolean isInverse = numPermutations < 0;
        
        // NB: because we use the signum and !=, this loop will work for both
        // positive and negative numbers of permutations
        int totalPermutations = Math.abs(numPermutations);

        for (int count = 1; count <= totalPermutations; ++count) {            
            // load the reordering funcion for this iteration of the permutation
            Function function = getFunction(count, v.length());

            // based on whether this is an inverse permutation, select whether
            // to use the forward or backwards mapping.
            int[] reordering = (isInverse) 
                ? function.backward : function.forward;

            oldDims = Arrays.copyOf(dimensions, dimensions.length);
            
            for (int i = 0; i < oldDims.length; ++i) {
                dimensions[i] = reordering[oldDims[i]];
            }
        }

        for (int d : dimensions)
            result.set(d, v.getValue(d));

        return result;
    }

    /**
     * An optimized instance of permute for TernaryVectors.  In this case, only
     * the positive and negative values are permuted, and a {@code
     * TernaryVector} is returned.
     */
    private Vector permute(TernaryVector v, int numPermutations, int length) {
        int[] oldPos = v.positiveDimensions();
        int[] oldNeg = v.negativeDimensions();

        // create new arrays to hold the permuted locations of the vectors's
        // positive and negative values.
        //
        // NB: we use a copy here to ensure that the function works for the 0
        // permutation (i.e. effectively a no-op);
        int[] positive = Arrays.copyOf(oldPos, oldPos.length);
        int[] negative = Arrays.copyOf(oldNeg, oldNeg.length);

        boolean isInverse = numPermutations < 0;
        
        // NB: because we use the signum and !=, this loop will work for both
        // positive and negative numbers of permutations
        int totalPermutations = Math.abs(numPermutations);

        for (int count = 1; count <= totalPermutations; ++count) {            

            // load the reordering funcion for this iteration of the permutation
            Function function = getFunction(count, length);

            // based on whether this is an inverse permutation, select whether
            // to use the forward or backwards mapping.
            int[] reordering = (isInverse) 
                ? function.backward : function.forward;
            
            // create a copy of the previous permuted values for positive and
            // negative.  We need this array because the permutation cannot be
            // done in place
            oldPos = Arrays.copyOf(positive, positive.length);
            oldNeg = Arrays.copyOf(negative, negative.length);
            
            // The reordering array specifies for index i the positive of i in
            // the permuted array.  Since the positive and negative indices are
            // the only non-zero indicies, we can simply create new arrays for
            // them of the same length and then set their new positions based on
            // the values in the reordering array.
            for (int i = 0; i < oldPos.length; ++i) {
                positive[i] = reordering[oldPos[i]];
            }

            for (int i = 0; i < oldNeg.length; ++i) {
                negative[i] = reordering[oldNeg[i]];
            }
        }

        return new TernaryVector(length, positive, negative);
    }

    /**
     * Returns the name of this class
     */
    public String toString() {
        return "DefaultPermutationFunction";
    }

    /**
     * A bijective, invertible mapping between indices.
     */
    private static class Function implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int[] forward;
        private final int[] backward;

        public Function(int[] forward, int[] backward) {
            this.forward = forward;
            this.backward = backward;
        }

    }

}
