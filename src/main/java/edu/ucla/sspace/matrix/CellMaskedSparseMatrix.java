/*
 * Copyright 2010 Keith Stevens 
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
import edu.ucla.sspace.vector.MaskedSparseDoubleVectorView;

import java.util.HashMap;
import java.util.Map;


/**
 * This {@link SparseMatrix} decorator allows every row and column index to be
 * remapped to new indices.  The size of this matrix will depend on the number
 * of mapped rows and columns.  Write-through access is allowed for each cell in
 * the underlying matrix through the {@code set} method.  Array based accessors
 * and getters are disabled when using this decorator.
 *
 * @author Keith Stevens
 */
public class CellMaskedSparseMatrix extends CellMaskedMatrix
                                    implements SparseMatrix {

    /**
     * The original underlying matrix.
     */
    private final SparseMatrix matrix;

    private final int[] rowMaskMap;

    private final Map<Integer, Integer> reverseRowMaskMap;

    private final int[] colMaskMap;

    private final Map<Integer, Integer> reverseColMaskMap;

    /**
     * Creates a new {@link CellMaskedSparseMatrix} from a given {@link
     * SparseMatrix} and maps, one for the row indices and one for the column
     * indices.  Only valid mappings should be included.
     *
     * @param matrix The underlying matrix to decorate
     * @param rowMaskMap A mapping from new indices to old indices in the
     *        original map for rows.
     * @param colMaskMap A mapping from new indices to old indices in the
     *        original map for columns.
     */
    public CellMaskedSparseMatrix(SparseMatrix matrix,
                                  int[] rowMaskMap,
                                  int[] colMaskMap) {
        super(matrix, rowMaskMap, colMaskMap);
        this.matrix = matrix;
        this.rowMaskMap = rowMaskMap;
        this.colMaskMap = colMaskMap;
        reverseRowMaskMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < rowMaskMap.length; ++i)
            reverseRowMaskMap.put(rowMaskMap[i], i);
        reverseColMaskMap = new HashMap<Integer, Integer>();
        for (int i = 0; i < colMaskMap.length; ++i)
            reverseColMaskMap.put(colMaskMap[i], i);
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getRowVector(int row) {
        row = getIndexFromMap(rowMaskMap, row);
        return new MaskedSparseDoubleVectorView(
                matrix.getRowVector(row), colMaskMap, reverseColMaskMap);
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getColumnVector(int col) {
        col = getIndexFromMap(colMaskMap, col);
        return new MaskedSparseDoubleVectorView(
                matrix.getColumnVector(col), rowMaskMap, reverseRowMaskMap);
    }
}
