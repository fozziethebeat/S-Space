/*
 * Copyright (c) 2011, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.matrix.factorization;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class SingularValueDecompositionTestUtil {

    public static final double[][] VALUES = {
        {1, 1, 0, 0, 0, 0, 1},
        {0, 1, 1, 0, 0, 0, 0},
        {1, 0, 0, 0, 0, 0, 0},
        {0, 0, 0, 1, 1, 1, 0},
        {0, 0, 0, 1, 0, 1, 1},
    };

    public static final SparseMatrix matrix = new YaleSparseMatrix(VALUES);

    public static final double[][] EXPECTED_U = {
        {0.38846, -0.73615},
        {0.117616, -0.425017},
        {0.0902812, -0.26945},
        {0.596357, 0.425017},
        {0.686639, 0.155567},
    };

    public static final double[] EXPECTED_S = {2.30278, 1.93185};

    public static final double[][] EXPECTED_V = {
        {0.207897,0.219768,0.0510758,0.557152,0.258973,0.557152,0.466871},
        {-0.520537,-0.601064,-0.220005,0.300532,0.220005,0.300532,-0.300532},
    };

    public static void testReductionFile(MatrixFactorization reducer,
                                         Format format) {
        try {
            File mFile = File.createTempFile("TestSvdMatrix", "dat");
            mFile.deleteOnExit();
            MatrixIO.writeMatrix(matrix, mFile, format);
            reducer.factorize(new MatrixFile(mFile, format), 2);
        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

            validateResults(reducer);
    }

    public static void testReductionMatrix(MatrixFactorization reducer) {
        reducer.factorize(matrix, 2);
        validateResults(reducer);
    }

    public static void validateResults(MatrixFactorization reducer) {
        Matrix U = reducer.dataClasses();
        assertEquals(matrix.rows(), U.rows());
        assertEquals(2, U.columns());

        for (int r = 0; r < matrix.rows(); ++r)
            for (int c = 0; c < 2; ++c)
                assertEquals(Math.abs(EXPECTED_U[r][c] * EXPECTED_S[c]),Math.abs(U.get(r,c)),.001);

        Matrix V = reducer.classFeatures();
        assertEquals(2, V.rows());
        assertEquals(matrix.columns(), V.columns());

        for (int r = 0; r < 2; ++r)
            for (int c = 0; c < matrix.columns(); ++c)
                assertEquals(Math.abs(EXPECTED_V[r][c] * EXPECTED_S[r]),Math.abs(V.get(r,c)),.001);
    }
}
