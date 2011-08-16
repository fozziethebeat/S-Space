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

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.svs.StructuredVectorSpace;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Iterator;


/**
 * A {@link DependencyContextGenerator} that marks each co-occurrence with
 * ordering information. 
 *
 * @author Keith Stevens
 */
public class SelPrefDependencyContextGenerator
        implements DependencyContextGenerator {

    public static final String EMPTY_STRING = "";

    private final StructuredVectorSpace svs;

    private final DependencyPathAcceptor acceptor;

    /**
     * Constructs a new {@link SelPrefDependencyContextGenerator}.
     */
    public SelPrefDependencyContextGenerator(StructuredVectorSpace svs) {
        this.svs = svs;
        acceptor = new UniversalPathAcceptor();
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                              int focusIndex) {
        Iterator<DependencyPath> paths = new FilteredDependencyIterator(
                tree[focusIndex], acceptor, 1);

        // Get the first contextualized meaning.  Return any empty vector in
        // case we don't have any paths for the word of interest (this should
        // never happen).
        if (!paths.hasNext())
            return new CompactSparseVector();
        SparseDoubleVector focusMeaning = contextualize(paths.next());

        // If this focus word isn't connected to any other word, just return the
        // contextualized vector that we have.
        if (!paths.hasNext())
            return focusMeaning;
        SparseDoubleVector secondMeaning = contextualize(paths.next());

        // If we have two relations for the focus word, multiply each
        // contextualized vector from the relations and return that as the final
        // meaning.
        return VectorMath.multiplyUnmodified(focusMeaning, secondMeaning);
    }

    private SparseDoubleVector contextualize(DependencyPath path) {
        DependencyRelation rel = path.iterator().next();
        String relation = rel.relation();
        String focusTerm = path.first().word();
        String otherTerm = path.last().word();

        // Skip any filtered features.
        if (otherTerm.equals(EMPTY_STRING))
            return null;
        boolean isFocusHead = !rel.headNode().word().equals(focusTerm);
        return svs.contextualize(focusTerm, relation, otherTerm, isFocusHead);
    }

    /**
     * A No-op
     */
    public void setReadOnly(boolean readOnly) {
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return svs.getVectorLength();
    }
}
