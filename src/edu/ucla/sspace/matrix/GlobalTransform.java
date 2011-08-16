/*
 * Copyright 2009 Keith Stevens 
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
 * An interface for matrix transformations.  Transformations that can be done
 * with either no knowledge of the given matrix or with only global knowledge,
 * such as row and column summations, can be implemented with this
 * transformation.  This is a supplement to the more general {@link Transform}
 * interface to fascilitate.  By implementing this interface, a transformation
 * can be done on any file type and any matrix type with little extra work.
 *
 * </p>Implementations are expected to gather any required statistics through
 * their constructor.  Afterwards, users of the transform may request each entry
 * in a matrix to be transformed based on it's row, column, and value.  Each
 * entry is welcome to interpret the meaning of row, column, and value
 * differently.  It is recomended that implementations be made a subclass of a
 * {@link Transform} implementation.  A good example is the {@link
 * TfIdfTransform}.
 *
 * </p> Note that most instances of a {@link GlobalTransform} should be used
 * with only one matrix.  Training on one matrix and applying the transformation
 * to another will often result in an unintelligible transformation.
 * Transformations that do not need to compute any statistics can be applied to
 * any matrix without any complications, such as a transformation that adds 5 to
 * every entry.
 *
 * @author Keith Stevens
 */
public interface GlobalTransform {

    /**
     * Returns the transformed value for a given matrix entry.
     *
     * @param row The row of the entry to be transformed
     * @param column The column of the entry to be transformed
     * @param value The old value of the entry to be transformed
     *
     * @return The new value of the entry located at {@code row}, {@code column}
     */
    double transform(int row, int column, double value);
}
