/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.util;

import java.awt.Color;

import java.util.Iterator;
import java.util.Random;


/**
 * A utility for generating a random sequence of colors.  This class also
 * provides functality for generating new colors from mixing a base "seed" color
 * with a random color, which often has the effect of generating a sequence of
 * pastels when a lighter color is used as a seed.
 */
public class ColorGenerator implements Iterator<Color> {

    private final Random rand;

    private final Color seed;

    public ColorGenerator() { 
        this(null);
    }

    public ColorGenerator(Color seed) { 
        this.seed = seed;
        rand = new Random();
    }

    /**
     * Always returns {@code true}.
     */
    public boolean hasNext() {
        return true;
    }
  
    /**
     * Returns the next random color
     */
    public Color next() {
        int r = rand.nextInt(256);
        int g = rand.nextInt(256);
        int b = rand.nextInt(256);
        return (seed == null)
            ? new Color(r, g, b)
            : new Color((r + seed.getRed()) / 2,
                        (g + seed.getGreen()) / 2,
                        (b + seed.getBlue()) / 2);
    }
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove colors");
    }
}