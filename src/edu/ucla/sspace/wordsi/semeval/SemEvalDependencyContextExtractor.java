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

package edu.ucla.sspace.wordsi.semeval;

import edu.ucla.sspace.wordsi.DependencyContextExtractor;
import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.Wordsi;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import java.io.BufferedReader;
import java.io.IOException;


/**
 * A SenseEval/SemEval based {@link DependencyContextExtractor}.  This extractor
 * assumes that the instance id is the first line in the document and that each
 * document only contains the sentences for that instance id.  The dependency
 * tree node for the focus word of each context should match the instance id.
 * The primary key will be the raw token of the instance id and the secondary
 * key will be the instance id. 
 *
 * @author Keith Stevens
 */
public class SemEvalDependencyContextExtractor
        extends DependencyContextExtractor {

    /**
     * Creates a new {@link SemEvalDependencyContextExtractor}.
     *
     * @param extractor The {@link DependencyExtractor} that parses the document
     *        and returns a valid dependency tree
     * @param basisMapping A mapping from dependency paths to feature indices
     * @param weighter A weighting function for dependency paths
     * @param acceptor An accepting function that validates dependency paths
     *        which may serve as features
     */
    public SemEvalDependencyContextExtractor(
            DependencyExtractor extractor,
            DependencyContextGenerator generator) {
        super(extractor, generator, true);
    }

    /**
     * Returns true if the {@code focusWord} equals the {@code contextHeader}.
     */
    protected boolean acceptWord(DependencyTreeNode focusNode,
                                 String contextHeader,
                                 Wordsi wordsi) {
        return wordsi.acceptWord(focusNode.word()) &&
               focusNode.lemma().equals(contextHeader);
    }

    /**
     * Returns the word of the {@code focusNode}
     */
    protected String getSecondaryKey(DependencyTreeNode focusNode,
                                     String contextHeader) {
        return focusNode.lemma();
    }
}
