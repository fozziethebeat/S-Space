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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;


/**
 * A {@code SparseMatrix} backed by vectors that provide amortized O(1) access
 * to their elements.  Each row is implemented using a hashing-based vector.
 * This class provides an alternate implementation to {@link YaleSparseMatrix};
 * this class potentially uses more memory than {@code YaleSparseMatrix}, but
 * provides O(1) access instead of O(log(n)).  The size of this matrix is fixed,
 * and attempts to access rows or columns beyond the size will throw an {@link
 * IndexOutOfBoundsException}.
 *
 * @author David Jurgens
 */
public class SparseHashMatrix extends AbstractMatrix implements SparseMatrix {

    /**
     * The number of rows contained in this {@code SparseMatrix}.
     */
    private final int rows;

    /**
     * The number of columns contained in this {@code SparseMatrix}.
     */
    private final int columns;

    /**
     * Each row is defined as a {@link SparseHashDoubleVector} which does most
     * of the work.
     */
    private final SparseHashDoubleVector[] sparseMatrix;

    /**
     * Constructs a sparse matrix with the specified dimensions.
     */
    public SparseHashMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        sparseMatrix = new SparseHashDoubleVector[rows];
        for (int r = 0; r < rows; ++r)
            sparseMatrix[r] = new SparseHashDoubleVector(columns);
    }

    /**
     * Checks that the indices are within the bounds of this matrix or throws an
     * {@link IndexOutOfBoundsException} if not.
     */        
    private void checkIndices(int row, int col) {
        if (row < 0 || col < 0 || row >= rows || col >= columns) {
            throw new IndexOutOfBoundsException();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public int columns() {
        return columns;
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int column) {
        SparseHashDoubleVector col = new SparseHashDoubleVector(rows);
        for (int r = 0; r < rows(); ++r)
            col.set(r, getRowVector(r).get(column));
        return col;
    }
    
    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        return Vectors.immutable(sparseMatrix[row]);
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);
        sparseMatrix[row].set(col, val);
    }
}