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

package edu.ucla.sspace.vector;


/**
 * Tests for the {@link SparseHashDoubleVector} class.
 */
public class SparseHashDoubleVectorTest extends AbstractTestSparseDoubleVector {

    protected SparseDoubleVector newNoLengthVector() {
        return new SparseHashDoubleVector();
    }

    protected SparseDoubleVector newLengthVector(int length) {
        return new SparseHashDoubleVector(length);
    }

    protected SparseDoubleVector newCopy(SparseDoubleVector other) {
        return new SparseHashDoubleVector(other);
    }

    protected SparseDoubleVector newFromArray(double[] values) {
        return new SparseHashDoubleVector(values);
    }

    protected SparseDoubleVector newFromValues(int[] nonZeros, 
                                               double[] values,
                                               int length) {
        return new SparseHashDoubleVector(nonZeros, values, length);
    }
}
