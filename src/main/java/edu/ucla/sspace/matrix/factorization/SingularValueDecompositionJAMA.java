/*
 * Copyright (c) 2013. Earl J. Wagner
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
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixFactorization;
import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.MatlabSparseMatrixBuilder;

import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.SVD.Algorithm;

import java.io.IOError;
import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;


/**
 * A wrapper around the JAMA
 *
 * @author Earl J. Wagner
 */
public class SingularValueDecompositionJAMA extends AbstractSvd {

    private static final Logger LOG = 
        Logger.getLogger(SingularValueDecompositionJAMA.class.getName());

    /**
     * {@inheritDoc}
     */
    public void factorize(SparseMatrix matrix, int dimensions) {
        Matrix[] svd = SVD.svd(matrix, Algorithm.JAMA, dimensions);

        dataClasses = svd[0];
		U = dataClasses;
        scaledDataClasses = false;

        classFeatures = svd[2];
		V = classFeatures;
        scaledClassFeatures = false;

        singularValues = new double[dimensions];
        for (int k = 0; k < dimensions; ++k)
            singularValues[k] = svd[1].get(k, k);
    }

    /**
     * {@inheritDoc}
     */
    public void factorize(MatrixFile mFile, int dimensions) {
		Matrix[] svd = SVD.svd(mFile.getFile(), Algorithm.JAMA,
							   mFile.getFormat(), dimensions);

		dataClasses = svd[0];
		U = dataClasses;
		scaledDataClasses = false;

		classFeatures = svd[2];
		V = classFeatures;
		scaledClassFeatures = false;

		singularValues = new double[dimensions];
		for (int k = 0; k < dimensions; ++k)
			singularValues[k] = svd[1].get(k, k);
    }

    /**
     * {@inheritDoc}
     */
    public MatrixBuilder getBuilder() {
        return new MatlabSparseMatrixBuilder();
    }
}
