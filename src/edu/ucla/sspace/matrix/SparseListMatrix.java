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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;

import java.util.ArrayList;
import java.util.List;


/**
 * A sub class of {@link ListMatrix} for {@link SparseDoubleVector}s.  Set
 * methods for vectors  are optimized to work efficiently for {@link
 * SparseDoubleVector}s.  Other behavior remains the same.
 *
 * @author Keith Stevens
 */
class SparseListMatrix<T extends SparseDoubleVector> extends ListMatrix<T>
                                                     implements SparseMatrix {

    /**
     * Creates a new {@code SparseListMatrix} from a list of {@link
     * SparseDoubleVectors}.  Immutable versions of each vector are stored.
     */
    public SparseListMatrix(List<T> vectorList) {
        super(vectorList);
    }

    public SparseListMatrix(List<T> vectorList, int columns) {
        super(vectorList, columns);
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int column) {
        int i = 0;
        SparseDoubleVector columnValues =
            new CompactSparseVector(vectors.size());

        for (DoubleVector vector : vectors)
            columnValues.set(i++, vector.get(column));
        return columnValues;
    }

    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, SparseDoubleVector values) {
        int i = 0;
        int[] nonZeros = values.getNonZeroIndices();
        for (int index : nonZeros)
            vectors.get(index).set(column, values.get(index));
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, SparseDoubleVector values) {
        SparseDoubleVector v = vectors.get(row);
        int[] nonZeros = values.getNonZeroIndices();
        for (int index : nonZeros)
            v.set(index, values.get(index));
    }
}
