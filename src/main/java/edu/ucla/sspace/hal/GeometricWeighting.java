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

package edu.ucla.sspace.hal;

/**
 * A geometically-decreasing weighting scheme for specifying how a {@link
 * HyperspaceAnalogueToLanguage} instance should weigh co-occurrences based on
 * the word distance.
 */
public class GeometricWeighting implements WeightingFunction {

    /**
     * Returns the weighed value where the closest words receive a weight equal
     * to the window size and the most distance words receive a weight of {@code
     * 1}, using a geometric (1 / 2<sup>n</sup>) decrease for in-between values.
     *
     * @param positionOffset {@inheritDoc}
     * @param windowSize {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    public double weight(int positionOffset, int windowSize) {
	return ((1 << (windowSize - (Math.abs(positionOffset) - 1))) / 
		(double)(1 << windowSize)) * windowSize;
    }

}