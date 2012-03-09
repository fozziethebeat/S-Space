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

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.AbstractMatrix;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;


/**
 * A {@link Matrix} implementation whose data is backed by a {@link
 * SemanticSpace}.  This class provides a bridge between numerical operations
 * that use the space's data and the results of the algorithm itself.
 */
public class SemanticSpaceMatrix extends AbstractMatrix {

    /**
     * The {@code SemanticSpace} whose data backs this matrix.
     */
    private final SemanticSpace sspace;    
    
    /**
     * A mapping from each row to the word in the {@code SemanticSpace} whose
     * vector corresponds to that row.
     */
    private final BiMap<Integer,String> rowToWord;

    /**
     * The number of columns in the matrix.  This value is cached, rather than
     * computed, in order to check for later modifications to the underlying
     * {@code SemanticSpace}, which might render this class's index-word mapping
     * invalid.
     */
    private final int columns;

    /**
     * Creates a {@code Matrix} whose data is backed by the provided {@code
     * SemanticSpace}.  The rows of this matrix are not guaranteed to be mapped
     * to any specific ordering of the words in the {@code SemanticSpace}.
     * Further, once this matrix is created, the underlying semantic space
     * should not be modified in such a way as to add new rows or to change the
     * vector length.
     */
    public SemanticSpaceMatrix(SemanticSpace sspace) {
        this.sspace = sspace;
        rowToWord = new HashBiMap<Integer,String>();
        columns = sspace.getVectorLength();

        for (String word : sspace.getWords())
            rowToWord.put(rowToWord.size(), word);
    }

    /**
     * Returns {@code true} if the underlying {@link SemanticSpace} was modified
     * after the initial row mapping performed in the constructor.
     */
    private boolean checkModifications() {
        return !(sspace.getWords().size() == rowToWord.size()
                 && columns == sspace.getVectorLength());
    }

    /**
     * Returns the row to which the provided term is mapped or {@link null} if
     * the term is not mapped to any row.
     */
    public Integer getRowIndex(String term) {
        return rowToWord.inverse().get(term);
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector getRowVector(int row) {
        if (row < 0 || row >= rowToWord.size())
            throw new IndexOutOfBoundsException("Row is out of bounds: " + row);
        return Vectors.asDouble(sspace.getVector(rowToWord.get(row)));
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
    public int rows() {
        return rowToWord.size();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        throw new UnsupportedOperationException(
            "Cannot modify SemanticSpace-backed matrix");
    }
    
    /**
     * {@inheritDoc}
     */
    public void setColumn(int column, DoubleVector values) {
        throw new UnsupportedOperationException(
            "Cannot modify SemanticSpace-backed matrix");
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, DoubleVector values) {
        throw new UnsupportedOperationException(
            "Cannot modify SemanticSpace-backed matrix");
    }
}