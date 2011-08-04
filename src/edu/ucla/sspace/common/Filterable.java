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

package edu.ucla.sspace.common;

import java.util.Set;


/**
 * A marker interface that indicates that this class supports selectively
 * filtering which words have their semantics retained.  The {@link
 * #setSemanticFilter(Set)} method can be used to speficy which words should
 * have their semantics retained.  Note that the words that are filtered out
 * will still be used in computing the semantics of <i>other</i> words.  This
 * behavior is intended for use with a large corpora where retaining the
 * semantics of all words in memory is infeasible.<p>
 *
 * @see SemanticSpace
 */
public interface Filterable {

    /**
     * Specifies the set of words that should have their semantics retained,
     * where all other words do not.
     *
     * @param semanticsToRetain the set of words that should have their
     *        semantics retained in memory
     */
    void setSemanticFilter(Set<String> semanticsToRetain);

}
