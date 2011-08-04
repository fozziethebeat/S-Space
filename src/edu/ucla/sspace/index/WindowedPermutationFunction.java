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

import edu.ucla.sspace.vector.Vector;

import java.util.Properties;


/**
 * A permutation function that provides windows of positions which are permuted
 * using the same function.  For instance, with a window size of 2, words 1 and
 * 2 positions away will be permuted once, words 3 and 4 positions away will be
 * permuted twice, etc.  This class supports the following properties:
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_LIMIT_PROPERTY }
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_LIMIT}
 *
 * <dd style="padding-top: .5em">This variable sets the size of a permutation
 * window.</p>
 *
 * </dl>
 *
 * @author Keith Stevens
 */
public class WindowedPermutationFunction
        implements PermutationFunction<Vector> {

    /**
     * The prefix for naming public properties.
     */
    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.index.WindowedPermutationFunction";

    /**
     * The property to set the window size.
     */
    public static final String WINDOW_LIMIT_PROPERTY =
        PROPERTY_PREFIX + ".window";

    /**
     * The default window limit to place on permutations.
     */
    public static final String DEFAULT_WINDOW_LIMIT = "1";

    /**
     * The backing permutation function to use.
     */
    private final PermutationFunction<Vector> function;

    /**
     * The window size for permutations.
     */
    private final int windowSize;

    /**
     * Creates a {@code WindowedPermutationFunction} using the default system
     * {@code Properties}.
     */
    public WindowedPermutationFunction() {
        this(System.getProperties());
    }

    /**
     * Creates a {@code WindowedPermutationFunction} using a passed in {@code
     * Properties}.
     */
    public WindowedPermutationFunction(Properties props) {
        function = new DefaultPermutationFunction();
        windowSize = Integer.parseInt(props.getProperty(
                    WINDOW_LIMIT_PROPERTY, DEFAULT_WINDOW_LIMIT));
    }
    
    /**
     * {@inheritDoc}
     */
    public Vector permute(Vector v, int numPermutations) {
        return function.permute(v, numPermutations/windowSize);
    }
}
