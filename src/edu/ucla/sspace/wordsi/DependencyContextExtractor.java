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

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;


/**
 * This {@link ContextExtractor} reads in documents that have been dependency
 * parsed.  Contexts are defined by a {@link FilteredDependencyIterator}, which
 * is used to traverse all possible dependency paths rooted at each word of
 * interest in a document.  Each reachable and valid {@link DependencyPath}
 * forms a feature and is weighted by a {@link DependencyPathWeight}.
 *
 * @author Keith Stevens
 */
public class DependencyContextExtractor implements ContextExtractor {

    /**
     * The {@link DependencyExtractor} used to extract parse trees from the
     * already parsed documents
     */
    protected final DependencyExtractor extractor;

    /**
     * The {@link DependencyContextGenerator} responsible for processing a
     * {@link DependencyTreeNode} and turning it into a context vector.
     */
    protected final DependencyContextGenerator generator;

    /**
     * If true, the first line in a dependency document will be treated as the
     * header of the document, and not part of the parse tree.
     */
    protected final boolean readHeader;

    /**
     * Creates a new {@link DependencyContextExtractor}.
     *
     * @param extractor The {@link DependencyExtractor} that parses the document
     *        and returns a valid dependency tree
     * @param generator The {@link DependencyContextGenerator} used to created
     *        context vectors based on a {@link DependencyTreeNode}.
     */
    public DependencyContextExtractor(DependencyExtractor extractor,
                                      DependencyContextGenerator generator) {
        this(extractor, generator, false);
    }

    /**
     * Creates a new {@link DependencyContextExtractor}.
     *
     * @param extractor The {@link DependencyExtractor} that parses the document
     *        and returns a valid dependency tree
     * @param generator The {@link DependencyContextGenerator} used to created
     *        context vectors based on a {@link DependencyTreeNode}.
     * @param readheader If true, the first line in a dependency tree document
     *        will be discarded from the tree and used as a header.
     */
    public DependencyContextExtractor(DependencyExtractor extractor,
                                      DependencyContextGenerator generator,
                                      boolean readHeader) {
        this.extractor = extractor;
        this.generator = generator;
        this.readHeader = readHeader;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return generator.getVectorLength();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document, Wordsi wordsi) {
        try {
            // Handle the context header, if one exists.  Context headers are
            // assumed to be the first line in a document.
            String contextHeader = handleContextHeader(document);

            // Iterate over all of the parseable dependency parsed sentences in
            // the document.
            DependencyTreeNode[] nodes = extractor.readNextTree(document);

            // Skip empty documents.
            if (nodes.length == 0)
                    return;

            // Examine the paths for each word in the sentence.
            for (int wordIndex = 0; wordIndex < nodes.length; ++wordIndex) {
                DependencyTreeNode focusNode = nodes[wordIndex];

                // Get the focus word, i.e., the primary key, and the
                // secondary key.  These steps are made as protected methods
                // so that the SenseEvalDependencyContextExtractor
                // PseudoWordDependencyContextExtractor can manage only the
                // keys, instead of the document traversal.
                String focusWord = getPrimaryKey(focusNode);
                String secondarykey = getSecondaryKey(focusNode, contextHeader);

                // Ignore any focus words that are unaccepted by Wordsi.
                if (!acceptWord(focusNode, contextHeader, wordsi))
                    continue;

                // Create a new context vector.
                SparseDoubleVector focusMeaning = generator.generateContext(
                                nodes, wordIndex);
                wordsi.handleContextVector(
                        focusWord, secondarykey, focusMeaning);
            }
            document.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns true if {@link Wordsi} should generate a context vector for
     * {@code focusWord}.    
     */
    protected boolean acceptWord(DependencyTreeNode focusNode,
                                 String contextHeader,
                                 Wordsi wordsi) {
        return wordsi.acceptWord(focusNode.word());
    }

    /**
     * Returns the token for the primary key, i.e. the focus word.  This is just
     * the text of the {@code focusNode}.
     */
    protected String getPrimaryKey(DependencyTreeNode focusNode) {
        return focusNode.word();
    }

    /**
     * Returns the token for the secondary key.  If a {@code contextHeader} is
     * provided, this is the {@code contextHeader}, otherwise it is the word for
     * the {@code focusNode}.
     */
    protected String getSecondaryKey(DependencyTreeNode focusNode,
                                     String contextHeader) {
        return (contextHeader == null) ? focusNode.word() : contextHeader;
    }

    /**
     * Returns the string for the context header.  If {@link readHeader} is
     * true, this returns the first line, otherwise it returns {@code null}.
     */
    protected String handleContextHeader(BufferedReader document)
            throws IOException {
        return (readHeader) ? document.readLine().trim() : null;
    }
}
