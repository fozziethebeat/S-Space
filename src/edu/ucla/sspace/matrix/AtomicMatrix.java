/*
 * Copyright 2009 David Jurgens 
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
 * An interface for any {@code Matrix} which wants to support atomic behavior.
 * All implementations must be thread safe such that each operation is atomic.
 * It is acceptable for several operations to be many read, but anytime a
 * modification operation takes place, there must be only one thread performing
 * a modification and no other threads reading data.
 *
 * @author David Jurgens
 */
public interface AtomicMatrix extends Matrix {

    /**
     * Return the value stored at ({@code row}, {@code col}), and add {@code
     * delta} to the same index.
     *
     * @param row The row of the cell to return and modify.
     * @param col The column of the cell to return and modify.
     * @param delta The amount to change the specified cell.
     *
     * @return The value of the cell at ({@code row}, {@code col}) prior to
     *         adding {@code delta}.
     */
    double getAndAdd(int row, int col, double delta);

    /**
     * Return the value stored at ({@code row}, {@code col}) plus {@code delta},
     * and then increment the specified cell by {@code delta}.
     *
     * @param row The row of the cell to modify and return.
     * @param col The column of the cell to modify and return.
     * @param delta The amount to change the specified cell.
     *
     * @return The value of the cell at ({@code row}, {@code col}) after
     *         adding {@code delta}.
     */
    double addAndGet(int row, int col, double delta);
}
