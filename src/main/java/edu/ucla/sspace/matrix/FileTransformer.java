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

import java.io.File;


/**
 * An interface for transforming matrix files of a particular format.  Since
 * each format has specific details regarding iteration, immplementations can
 * efficiently do a transform simply by iterating through the file once and
 * request the transformed value for each entry from an {@code GlobalTransform}
 * instance.
 *
 * @author Keith Stevens
 */
public interface FileTransformer {

    /**
     * Transforms a given matrix file into a new matrix file using the provided
     * {@code GlobalTransform} method provided.
     *
     * @param inputMatrixFile The input matrix file of a specific format that
     *                        will be transformed
     * @param outputMatrixFile The output matrix file specificying where the
     *                        resulting matrix should be stored.
     * @param transform A transformation instance that will convert values at a
     *                  particular row and column into a new value.
     *
     * @return {@code outputMatrixFile}
     */
    File transform(File inputMatrixFile,
                   File outputMatrixFile,
                   GlobalTransform transform);
}
