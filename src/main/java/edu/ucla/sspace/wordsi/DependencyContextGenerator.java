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

import edu.ucla.sspace.vector.SparseDoubleVector;


/**
 * An interface for generating context vectors from raw unparsed text.  A
 * context vector can be generated from a sliding window of text.  {@link
 * ContextGenerator}s are a subcomponent of {@link ContextExtractor}s.  As a
 * {@link ContextExtractor} examines text and finds a word which is being
 * represented in the {@link Wordsi} space, it will call the {@link
 * ContextGenerator} and pass the created context vector to a {@link Wordsi}
 * implementation.
 *
 * </p>
 *
 * {@link ContextGenerator}s are recomended to be made serializable.  They will
 * serve as the core representation method of {@link Wordsi} implementations and
 * can thus be re-used in multiple evaluations.  For example, after training a
 * {@link Wordsi} model, it may need to be evaluated in a psuedo-word
 * disambiguation task or a SemEval task.  In both cases, the feature space must
 * remain the same turing training and evaluation.
 *
 * </p>
 *
 * For evaluation purposes, an added option is available: a read only mode.
 * When in read only mode, {@link ContextGenerator}s should not create any new
 * features.  If some co-occurring term does not exist in the feature space, it
 * should be left out of the context vector, only feature which already exist in
 * the space should contribute to the context vector.  In standard mode, the
 * generator is permitted to decided which words should serve as features using
 * any method.
 *
 * @author Keith Stevens
 */
public interface DependencyContextGenerator {

    /**
     * Returns a {@link SparseDoubleVector} that represents the context composed
     * of the set of {@code prevWords} before the focus word and the set of
     * {@code nextWords} after the focus word.  Since sparse vectors are
     * returned, if a second order vector is generated, it is recommended that
     * the vector also be sparsed or have very few dimensions.
     */
    SparseDoubleVector generateContext(DependencyTreeNode[] tree,
                                        int focusIndex);

    /**
     * Returns the maximum number of dimensions used to represent any given
     * context.
     */
    int getVectorLength();

    /**
     * Sets the read only mode of the {@link ContextGenerator}.  When set to
     * read only, it prevents any new features from being generated.
     */
    void setReadOnly(boolean readOnly);
}
