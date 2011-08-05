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

package edu.ucla.sspace.wordsi.psd;

import edu.ucla.sspace.wordsi.DependencyContextExtractor;
import edu.ucla.sspace.wordsi.DependencyContextGenerator;
import edu.ucla.sspace.wordsi.Wordsi;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode;

import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Map;


/**
 * A pseudo word based {@link DependencyContextExtractor}.  Given a mapping from
 * raw tokens to pseudo words, this extractor will automatically change the text
 * for any dependency node that has a valid pseudo word mapping.  The pseudo
 * word will serve as the primary key for assignments and the original token
 * will serve as the secondary key.
 *
 * @author Keith Stevens
 */
public class PseudoWordDependencyContextExtractor 
      extends DependencyContextExtractor {

    /**
     * The mapping used between tokens and their pseudoword replacement.
     */
    private Map<String, String> pseudoWordMap;

    /**
     * Creates a new {@link PseudoWordDependencyContextExtractor}.
     *
     * @param extractor The {@link DependencyExtractor} that parses the document
     *        and returns a valid dependency tree
     * @param basisMapping A mapping from dependency paths to feature indices
     * @param weighter A weighting function for dependency paths
     * @param acceptor An accepting function that validates dependency paths
     *        which may serve as features
     * @param pseudoWordMap A mapping from raw tokens to pseudo words
     */
    public PseudoWordDependencyContextExtractor(
            DependencyExtractor extractor,
            DependencyContextGenerator generator,
            Map<String, String> pseudoWordMap) {
        super(extractor, generator, true);
        this.pseudoWordMap = pseudoWordMap;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        try {
            // Handle the context header, if one exists.  Context headers are
            // assumed to be the first line in a document and to contain an
            // integer specifying which line the focus word is on..
            String contextHeader = handleContextHeader(document);
            String[] contextTokens = contextHeader.split("\\s+");
            int focusIndex = Integer.parseInt(contextTokens[3]);

            // Extract the dependency trees and skip any that are empty.
            DependencyTreeNode[] nodes = extractor.readNextTree(document);
            if (nodes.length == 0)
                return;
            DependencyTreeNode focusNode = nodes[focusIndex];

            // Get the focus word, i.e., the primary key, and the secondary key.
            String focusWord = getPrimaryKey(focusNode);
            String secondarykey = pseudoWordMap.get(focusWord);

            // Ignore any focus words that have no mapping.
            if (secondarykey == null)
                return;

            // Ignore any focus words that are unaccepted by Wordsi.
            if (!acceptWord(focusNode, contextTokens[1], wordsi))
                return;

            // Create a new context vector and send it to the Wordsi model.
            SparseDoubleVector focusMeaning = generator.generateContext(
                    nodes, focusIndex);
            wordsi.handleContextVector(secondarykey, focusWord, focusMeaning);

            // Close up the document.
            document.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns true if {@code focusWord} is a known pseudo word.
     */
    protected boolean acceptWord(DependencyTreeNode focusNode,
                                 String contextHeader,
                                 Wordsi wordsi) {
        return pseudoWordMap.containsKey(focusNode.word()) &&
               focusNode.word().equals(contextHeader);
    }
}
