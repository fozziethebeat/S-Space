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

import java.util.List;


/**
 * An interface for weighting, or scoring, dependency paths.  Implementations
 * are suggested to be thread-safe and stateless .
 *
 * @author Keith Stevens
 */
public interface DependencyPathWeight {

    /**
     * Returns the score of the provided {@link DependencyPath}.  The score may
     * be a function of the length of the path, arbitrary, e.g., 1 for all
     * paths, or may be a function of the relations and terms in the path.
     *
     * @param path A list of the term,relation links in the {@link
     *        DependencyPath} being scored
     *
     * @return The score of the dependecy path
     */
    double scorePath(DependencyPath path);
}
