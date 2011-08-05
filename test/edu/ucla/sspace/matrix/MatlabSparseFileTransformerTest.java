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

import edu.ucla.sspace.matrix.MatrixIO.Format;

import org.junit.Test;


/**
 * @author Keith Stevens
 */
public class MatlabSparseFileTransformerTest {

    public static final double[][] VALUES = {
        {1, 1, 1, 4, 5},
        {5, 3, 1, 0, 0},
        {0, 1, 5, 0, 2},
    };

    public static final Matrix MATRIX = new ArrayMatrix(VALUES);

    @Test public void testTransform() {
        FileTransformer transformer = new MatlabSparseFileTransformer();
        FileTransformUtil.testTransform(MATRIX, Format.MATLAB_SPARSE,
                                        transformer);
    }
}

