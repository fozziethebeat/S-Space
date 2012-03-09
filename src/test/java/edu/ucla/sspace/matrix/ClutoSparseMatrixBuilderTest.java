/*
 * Copyright (c) 2010, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
 *
 * This file is part of the C-Cat package and is covered under the terms and
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

import org.junit.Test;


/**
 * @author Keith Stevens
 */
public class ClutoSparseMatrixBuilderTest {

    public static final double[][] VALUES = {
            {1, 2, 3, 4, 5},
            {5, 4, 3, 2, 1},
            {0, 1, 0, 1, 0},
    };

    @Test public void testMatrixBuild() {
        Matrix m = new ArrayMatrix(VALUES);
        MatrixBuilder builder = new ClutoSparseMatrixBuilder();
        MatrixBuilderTestUtil.testMatrixBuild(builder, m);
    }

    @Test public void testArrayBuild() {
        Matrix m = new ArrayMatrix(VALUES);
        MatrixBuilder builder = new ClutoSparseMatrixBuilder();
        MatrixBuilderTestUtil.testArrayBuild(builder, m);
    }

    @Test public void testSparseArrayBuild() {
        Matrix m = new ArrayMatrix(VALUES);
        MatrixBuilder builder = new ClutoSparseMatrixBuilder();
        MatrixBuilderTestUtil.testSparseArrayBuild(builder, m);
    }
}

