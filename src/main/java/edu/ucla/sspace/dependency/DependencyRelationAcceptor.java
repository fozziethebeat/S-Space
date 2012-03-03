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
 * An interface for providing restrictions on dependency relations.  When a
 * dependency path is being generated, a {@link DependencyRelationAcceptor} will
 * be called for each link in the path.  A path will be terminated before the
 * first unacceptable link.
 *
 * </p>
 *
 * Implementations are recomended to be a thread-safe and stateless.  This
 * restricts acceptors to be limited to rejecting a link at a time, as opposed
 * to rejecting a link based on the prior link.
 *
 * @author Keith Stevens
 */
public interface DependencyRelationAcceptor {

    /**
     * Returns wether or not the given dependency relation should be accepted.
     * Relations may be rejected based solely on the part of speech tags, the
     * dependency relation, or on some combination of the three features.
     *
     * @param relation the relation to evaluate
     *
     * @return {@code true} if the relation is acceptable, {@code false} otherwise
     */
    public boolean accept(DependencyRelation relation);
}
