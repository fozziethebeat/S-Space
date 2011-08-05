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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * A tiled view of a {@code SparseMatrix} instance where selected rows of the
 * instance a represented as a single, contiguous matrix.  This effectively
 * creates a {@code SparseMatrix} out of a possibly non-contiguous selection of
 * the rows of the original.  This class is intended to be use when a large
 * matrix has been created and submatrices of the large matrix need to be
 * treated as full {@code SparseMatrix} instances; rather than copy the data,
 * this class provides a way of representing the original data as a partial
 * view.<p>
 *
 * All methods are write-through to the original backing matrix.
 *
 * @author David Jurgens
 *
 * @see RowMaskedMatrix
 */
public class SparseRowMaskedMatrix extends RowMaskedMatrix 
        implements SparseMatrix {

    private final SparseMatrix matrix;

    /**
     * Creates a partial view of the provided sparse matrix using the bits set
     * to {@code true} as the rows that should be included
     *
     * @throws IllegalArgumentException if {@code included} has a bit set whose
     *         index is greater than the number of rows present in {@code
     *         matrix}
     */
    public SparseRowMaskedMatrix(SparseMatrix matrix, BitSet included) {
        super(matrix, included);
        this.matrix = matrix;
    }

    /**
     * Creates a partial view of the provided sparse matrix using the integers
     * in the set to specify which rows should be included in the matrix.  Note
     * that the ordering of the rows in the set does not matter; rows will be
     * mapped to the respective indices based on the numeric ordering of the
     * values in the set.
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of rows present in
     *         {@code matrix}
     */
    public SparseRowMaskedMatrix(SparseMatrix matrix, Set<Integer> included) {
        super(matrix, included);
        this.matrix = matrix;
    }

    /**
     * Creates a partial view of the provided sparse matrix using the the
     * integer mapping to specify which rows should be included in the matrix.  
     *
     * @throws IllegalArgumentException if {@code included} specifies a value
     *         that is less than 0 or greater than the number of rows present in
     *         {@code matrix}
     */
    public SparseRowMaskedMatrix(SparseMatrix matrix, int[] reordering) {
        super(matrix, reordering);
        this.matrix = matrix;
    }


    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int column) {
        int rows = rows();
        SparseDoubleVector v = new SparseHashDoubleVector(rows);
        for (int row = 0; row < rows; ++row) {
            double d = get(row, column);
            if (d != 0)
                v.set(row, d);
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        return matrix.getRowVector(getRealRow(row));
    }
}
