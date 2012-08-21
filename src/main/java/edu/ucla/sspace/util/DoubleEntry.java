/*
 * Copyright 2010 David Jurgens
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


/**
 * An object that represents an index that has an associated {@code double}
 * value.  This class is intended to support other classes that provide iterator
 * access over their indexable values without needing to incur auto-boxing
 * overhead.
 */
public class DoubleEntry {

    public int index;

    public double value;

    public DoubleEntry(int index, double value) {
        this.index = index;
        this.value = value;
    }

    /**
     * Returns the index position of this entry.
     */
    public int index() {
        return index;
    }

    /**
     * Returns the value at this entry's index.
     */
    public double value() {
        return value;
    }

    public String toString() {
        return "[" + index + "->" + value + "]";
    }
}
