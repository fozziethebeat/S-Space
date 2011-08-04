/*
 * Copyright 2010 Keith Stevens
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

package edu.ucla.sspace.dependency;


/**
 * A {@link DependencyPathWeight} that scores paths based on the sum of the
 * relations in the path.  Links in the path are weighted as follows:
 * <ul>
 *   </li>"subj" = 5;
 *   </li>"obj" = 4;
 *   </li>"obl" = 3;
 *   </li>"gen" = 2;
 *   </li>other links = 1;
 * <ul>
 *
 * @author Keith Stevens
 */
public class RelationSumPathWeight implements DependencyPathWeight {

    /**
     * {@inheritDoc}
     */
    public double scorePath(DependencyPath path) {
        double score = 0;
        for (DependencyRelation wordRelation : path) {
            if (wordRelation.relation().equals("SBJ"))
                score += 5;
            else if (wordRelation.relation().equals("OBJ"))
                score += 4;
            else if (wordRelation.relation().equals("OBL"))
                score += 3;
            else if (wordRelation.relation().equals("GEN"))
                score += 2;
            else 
                score += 1;
        }
        return score;
    }
}
