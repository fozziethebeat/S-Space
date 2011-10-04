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
 * A symmetric dense matrix that only stores the values of the lower triangular.
 * This class only records changes values where row &gt; col.  For all other
 * values, the row and column values are swapped and then the backing matrix is
 * updated.  
 *
 * </p>
 *
 * The primary benfit of this class is for storing large symmetric matrices in
 * half of the memory.
 *
 * @author David Jurgens
 */
public class SymmetricMatrix extends AbstractMatrix 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final double[][] values;

    private final int rows;

    private final int columns;

    /**
     * Constructs a new {@link SymmetricMatrix} with the specified dimensions.
     */
    public SymmetricMatrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;

        values = new double[rows][];
        for (int r = 0; r < rows; ++r)
            values[r] = new double[r+1];
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
    @Override public double get(int row, int column) {
        // Swap the ordering so only the lower triangular is read
        if (column > row) {
            int tmp = column;
            column = row;
            row = tmp;
        }
        return values[row][column];
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getColumnVector(int column) {
        DenseVector col = new DenseVector(rows);
        for (int r = 0; r < rows; ++r)
            col.set(r, get(r, column));
        return col;
    }
    
    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        DenseVector rowVec = new DenseVector(columns);
        for (int c = 0; c < columns; ++c)
            rowVec.set(c, get(row, c));
        return rowVec;
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
    @Override public void set(int row, int column, double val) {
        // Swap the ordering so only the lower triangular is written to
        if (column > row) {
            int tmp = column;
            column = row;
            row = tmp;
        }
        values[row][column] = val;
    }
}
