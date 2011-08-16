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

import edu.ucla.sspace.basis.BasisMapping;

import edu.ucla.sspace.dependency.DependencyTreeNode;


/**
 * A {@link DependencyContextGenerator} that marks each co-occurrence with part
 * of speech information.
 *
 * @author Keith Stevens
 */
public class PartOfSpeechDependencyContextGenerator
        extends AbstractOccurrenceDependencyContextGenerator{

    /**
     * Constructs a new {@link PartOfSpeechDependencyContextGenerator}.
     */
    public PartOfSpeechDependencyContextGenerator(
            BasisMapping<String, String> basis,
            int windowSize) {
        super(basis, windowSize);
    }

    /**
     * Returns a string with the node's word plus it's part of speech, with a
     * hyphen between the two.
     */
    protected String getFeature(DependencyTreeNode node, int index) {
        return node.word() + "-" + node.pos();
    }
}
