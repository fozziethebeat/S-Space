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

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;


/**
 * A decorator around a {@code Matrix} that keeps only the upper triangular
 * values while providing a symmetric view of the data.  This class only records
 * changes values where row &gt; col.  For all other values, the row and column
 * values are swapped and then the backing matrix is updated.  Note, that if the
 * provided backing matrix has existing values for indices row &lt; col, these
 * values will be ignored and never returned from any method.  Note the original
 * perfomance characteristics of the backing matrix are retained by this class.
 *
 * <p>The primary benfit of this class is for storing large symmetric matrices
 * in half of the memory.
 *
 * @author David Jurgens
 *
 * @see SparseSymmetricMatrix
 */
public class SymmetricMatrix extends AbstractMatrix 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The matrix whose values back this matrix.
     */
    private final Matrix backing;

    /**
     * Constructs a sparse matrix with the specified dimensions.
     */
    public SymmetricMatrix(Matrix backing) {
        this.backing = backing;
    }
    
    /**
     * {@inheritDoc}
     */
    public int columns() {
        return backing.columns();
    }

    /**
     * {@inheritDoc}
     */
    @Override public double get(int row, int column) {
        // Swap the ordering so only the upper triangular is read
        if (row > column) {
            int tmp = column;
            column = row;
            row = tmp;
        }
        return backing.get(row, column);
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        int rows = rows();
        DenseVector col = new DenseVector(rows);
        for (int r = 0; r < rows; ++r)
            col.set(r, get(r, column));
        return col;
    }
    
    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        int cols = columns();
        DenseVector rowVec = new DenseVector(cols);
        for (int c = 0; c < cols; ++c)
            rowVec.set(c, get(row, c));
        return rowVec;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return backing.rows();
    }

    /**
     * {@inheritDoc}
     */
    @Override public void set(int row, int column, double val) {
        // Swap the ordering so only the upper triangular is written to
        if (row > column) {
            int tmp = column;
            column = row;
            row = tmp;
        }
        backing.set(row, column, val);
    }
}