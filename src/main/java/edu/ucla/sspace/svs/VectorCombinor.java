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

package edu.ucla.sspace.svs;

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * This interface combines two {@link SparseDoubleVector} to create a new {@link
 * SparseDoubleVector} that a combination or mixture of features from both
 * {@link SparseDoubleVector}s.
 *
 * @author Keith Stevens
 */
public interface VectorCombinor {

    /**
     * Combines features from {@code v1} and {@code v2} to produce a new vector.
     * {@link VectorCombinor} are allowed to modify {@code v1} if needed.
     */
    SparseDoubleVector combine(SparseDoubleVector v1, SparseDoubleVector v2);

    /**
     * Combines features from {@code v1} and {@code v2} to produce a new vector.
     * {@link VectorCombinor} are <b>not</b> allowed to modify {@code v1} if
     * needed.
     */
    SparseDoubleVector combineUnmodified(SparseDoubleVector v1, 
                                         SparseDoubleVector v2);
}

