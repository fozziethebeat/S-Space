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

package edu.ucla.sspace.matrix;


/**
 * An interface for any algorithm that decomposes a {@link Matrix} into two sub
 * {@link Matrix}s that are linked by some form of latent variable.   Examples
 * of these decomposition algorithms are Singular Value Decomposition, Principal
 * Component Analysis, and Non-Negative Matrix Factorization.  
 *
 * </br>
 *
 * Given a {@link MatrixFactorization} algorithm, call {@link factorize} with a
 * {@link SparseMatrix} representing the data points and the desired number of
 * dimensions.  The algorithm will then split the matrix into two smaller {@link
 * Matrix}s: a data point by latent variable {@link Matrix} and a latent
 * variable by feature {@link Matrix}.  The interpretations of these matrices
 * differer for each algorithm, but in all cases, these can be used as reduced
 * representations of either the data points or the features.  with a minimal
 * loss of information.
 *
 * @author Keith Stevens
 */
public interface MatrixFactorization {

    /**
     * Returns a {@link MatrixBuilder} that is optimized for {@link MatrixFile}s
     * that will be processed by this {@link MatrixFactorization} algorithm.
     */
    MatrixBuilder getBuilder();

    /**
     * Factorizes the {@link SparseMatrix} {@code m}.  If {@code m} is of size
     * MxN, this will generate two matrices: one of size Mx{@code numDimensions}
     * and one of size {@code numDimensions}xN.  This method must be called
     * before calling {@link dataClasses} and {@link classFeatures}.
     */
    void factorize(SparseMatrix m, int numDimensions);

    /**
     * Factorizes the {@link MatrixFile} {@code m}.  If {@code m} is of size
     * MxN, this will generate two matrices: one of size Mx{@code numDimensions}
     * and one of size {@code numDimensions}xN.  This method must be called
     * before calling {@link dataClasses} and {@link classFeatures}.
     */
    void factorize(MatrixFile m, int numDimensions);

    /**
     * Returns the data point by latent class {@link Matrix}.  This matrix
     * represents the degree by which each data point can be explained by the
     * discovered latent classes.  Actual interpretations of the latent classes
     * depends on the actual algorithm used.  This {@link Matrix} can be used as
     * a reduced representation of the data points themselves.
     */
    Matrix dataClasses();

    /**
     * Returns the latent class by feature {@link Matrix}.  This matrix
     * represents the degree by which each feature affects each latent class.
     * Actual interpretations of this interaction depends on the actual
     * algorithm used.  This {@link Matrix} can be used as a reduced
     * representation of the features.
     */
    Matrix classFeatures();
}

