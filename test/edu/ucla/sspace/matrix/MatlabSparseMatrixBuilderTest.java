/*
 * Copyright 2011 Keith Stevens
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

import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.io.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class MatlabSparseMatrixBuilderTest {
    
    private static final double[][] values = {{1, 0, 1, 2, 4},
                                              {3, 5, 0, 0, 1},
                                              {1, 1, 1, 0, 0}};

    public Matrix denseMatrix() {
        return new ArrayMatrix(values);
    }

    public SparseMatrix sparseMatrix() {
        SparseMatrix m = new YaleSparseMatrix(values.length, values[0].length);
        for (int r = 0; r < m.rows(); ++r)
            for (int c = 0; c < m.columns(); ++c)
                if (values[r][c] != 0d)
                    m.set(r,c, values[r][c]);
        return m;
    }

    @Test public void testAddSparseVectorColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        MatrixBuilderTestUtil.testMatrixBuild(builder, sparseMatrix(), true);
    }

    @Test public void testAddSparseVectorColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        MatrixBuilderTestUtil.testMatrixBuild(builder, sparseMatrix());
    }

    @Test public void testAddDoubleColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        MatrixBuilderTestUtil.testArrayBuild(builder, sparseMatrix(), true);
    }

    @Test public void testAddDoubleColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        MatrixBuilderTestUtil.testArrayBuild(builder, sparseMatrix());
    }

    @Test public void testAddDenseVectorColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        MatrixBuilderTestUtil.testArrayBuild(builder, denseMatrix(), true);
    }

    @Test public void testAddDenseVectorColumnTranpose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        MatrixBuilderTestUtil.testArrayBuild(builder, denseMatrix());
    }

    @Test public void testAddSparseArrayColumn() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder();
        MatrixBuilderTestUtil.testSparseArrayBuild(builder,sparseMatrix(),true);
    }

    @Test public void testAddSparseArrayColumnTranspose() {
        MatrixBuilder builder = new MatlabSparseMatrixBuilder(true);
        MatrixBuilderTestUtil.testSparseArrayBuild(builder, sparseMatrix());
    }
}
