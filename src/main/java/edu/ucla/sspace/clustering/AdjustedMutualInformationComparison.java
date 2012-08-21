/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.AdjustedMutualInformation;


/**
 * A {@link PartitionComparison} that depends on the <a
 * href="http://en.wikipedia.org/wiki/Adjusted_mutual_information">Adjusted
 * Mutual Information</a> similarity measure.  Values range from 0 to 1
 * inclusive and are symmetric.  A score of 1 indicates that two {@link
 * {Partition}s match perfectly while a score of 0 indicates that they share no
 * information.
 * 
 * @see AdjustedMutualInformation
 *
 * @author Keith Stevens
 */
public class AdjustedMutualInformationComparison 
        extends MatrixAggregateComparison {

    /**
     * Creates a new {@link AdjustedMutualInformationComparison}.
     */
    public AdjustedMutualInformationComparison() {
        super(new AdjustedMutualInformation());
    }

    /**
     * Returns false.
     */
    public boolean isDistance() {
        return false;
    }
}
