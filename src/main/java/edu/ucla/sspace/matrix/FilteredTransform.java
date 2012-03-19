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

import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.TransformStatistics.MatrixStatistics;

import java.io.File;


/**
 * This {@link Transform} filteres the filters returned by an inner {@link
 * Transform} to be within some fixed range.  Any transformed value for a matrix
 * that falls outside of the specified range will be replaced with 0.
 *
 * </p>
 *
 * Note that this currently only works with {@link Transform}s that are
 * subclasses of {@link GlobalTransform}.
 *
 * @author Keith Stevens
 */
public class FilteredTransform extends BaseTransform {

    /**
     * The original {@link Transform} that will have it's values filtered.
     */
    private final BaseTransform base;

    /**
     * The maximum transformed value permitted.
     */
    private double max;

    /**
     * The minimum transformed value permitted.
     */
    private double min;

    /**
     * Creates a new {@link FilteredGlobalTransform} that will limit the values
     * returned by {@code base} to be within the range of {@code min} and {@link
     * Double#MAX_VALUE}.
     */
    public FilteredTransform(BaseTransform base, double min) {
        this(base, min, Double.MAX_VALUE);
    }

    /**
     * Creates a new {@link FilteredGlobalTransform} that will limit the values
     * returned by {@code base} to be within the range of {@code min} and {@code
     * max}.
     */
    public FilteredTransform(BaseTransform base, double min, double max) {
        this.base = base;
        this.min = min;
        this.max = max;
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(Matrix matrix) {
        return new FilteredGlobalTransform(base.getTransform(matrix));
    }

    /**
     * {@inheritDoc}
     */
    protected GlobalTransform getTransform(File input,
                                           MatrixIO.Format format) {
        return new FilteredGlobalTransform(base.getTransform(input, format));
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
        return "Filtered-" + base.toString();
    }

    public class FilteredGlobalTransform
            implements GlobalTransform {

        /**
         * The {@link GlobalTransform} whose values will be filtered.
         */
        private final GlobalTransform base;

        /**
         * Creates an instance of {@code FilteredTransform}
         * from a given {@link Matrix}.
         */
        public FilteredGlobalTransform(GlobalTransform base) {
            this.base = base;
        }

        /**
         * Computes the filtered value returned by the inner {@link
         * GlobalTransform}.  If the base transform returns a value outside of
         * the range of {@code min} and {@code max}, the value will be replaced
         * with {@code 0}.
         *
         * @param row The index specifying the row being observed
         * @param col The index specifying the col being observed
         * @param value The number of ocurrances of row and col together
         */
        public double transform(int row, int col, double value) {
            double v = base.transform(row,col,value);
            if (v > max)
                return 0;
            return (v < min) ? 0 : v;
        }
    }
}
