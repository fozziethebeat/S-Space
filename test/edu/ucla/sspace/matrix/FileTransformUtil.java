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

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class FileTransformUtil {

    public static final GlobalTransform transform =
        new OnePlusGlobalTransform();

    public static void testTransform(Matrix m,
                                     Format format,
                                     FileTransformer transformer) {
        try {
            File mFile = File.createTempFile("toBeTransformed", "dat");
            MatrixIO.writeMatrix(m, mFile, format);
            File transformedFile = File.createTempFile("transformed", "dat");
            transformer.transform(mFile, transformedFile, transform);

            Matrix out = MatrixIO.readMatrix(transformedFile, format);

            assertEquals(out.rows(), m.rows());
            assertEquals(out.columns(), m.columns());

            for (int r = 0; r < out.rows(); ++r)
                for (int c = 0; c < out.columns(); ++c)
                    if (m.get(r,c) != 0d)
                        assertEquals(m.get(r,c), out.get(r,c)-1, .00001);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public static class OnePlusGlobalTransform implements GlobalTransform {
        public double transform(int row, int col, double value) {
            return value+1;
        }
    }
}
