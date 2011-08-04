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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseVector;

/**
 * An interface for sparse {@code Matrix} implementations whose backing data
 * storage permits accessing rows and columns with {@link SparseVector} objects.
 *
 * @see Matrix
 * @see SparseDoubleVector
 *
 * @author DavidJurgens
 */
public interface SparseMatrix extends Matrix {

    /**
     * Returns the column as a sparse vector.  Whether updates to the vector are
     * written through to the backing matrix is left open to the implementation.
     *
     * @param column The column to return a {@code DoubleVector} for
     *
     * @return A {@code DoubleVector} representing the column at {@code column}
     */
    SparseDoubleVector getColumnVector(int column);

    /**
     * Returns the row as a sparse vector.  Whether updates to the vector are
     * written through to the backing matrix is left open to the implementation.
     *
     * @param row the index of row to return
     *
     * @return A {@code SparseDoubleVector} of the row's data
     */
    SparseDoubleVector getRowVector(int row);

}