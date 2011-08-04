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

import edu.ucla.sspace.vector.Vector;


/**
 * An interface for functions that permute the ordering of {@code
 * TernaryVector}s.  Implementations are expected to be thread safe when
 * performing permutations.
 */
public interface PermutationFunction <T extends Vector> {

    /**
     * Permutes the provided {@code TernaryVector} the specified number of
     * times.
     *
     * @param v an index vector to permute
     * @param numPermutations the number of times the permutation function
     *                        should be applied to the provided index vector.
     *
     * @return the original index vector permuted the specified number of times
     */
    T permute(T v, int numPermutations);
}
