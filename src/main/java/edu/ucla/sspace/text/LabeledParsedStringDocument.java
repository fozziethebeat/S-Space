/*
 * Copyright 2011 David Jurgens
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

package edu.ucla.sspace.text;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import java.io.IOError;
import java.io.IOException;

import java.util.regex.Pattern;


/**
 * An abstraction for a document that has been (or will be) dependency parsed to
 * generate an accompanying parse tree of its contents.
 */
public class LabeledParsedStringDocument {
    
    // private final DependencyTreeNode[] nodes;

    // public LabeledParsedStringDocument(String label, 
    //                                    DependencyExtractor extractor,
    //                                    String parse) {
    //     super(label, parse);
    //     try {
    //         nodes = extractor.readNextTree(reader());
    //     } catch (IOException ioe) {
    //         // NOTE: this should never happen because we're being given the
    //         // parse string itself, so the reader passed to readNextTree() will
    //         // be reading from an in-memory buffer, rather than file
    //         throw new IOError(ioe);
    //     }
    // }

    // /**
    //  * {@inheritDoc}
    //  */
    // public DependencyTreeNode[] parsedDocument() {
    //     return nodes;
    // }

    // /**
    //  * {@inheritDoc}
    //  */
    // public String text() {
    //     StringBuilder sb = new StringBuilder(nodes.length * 8);
    //     for (int i = 0; i < nodes.length; ++i) {
    //         String token = nodes[i].word();
    //         sb.append(token);
    //         if (i+1 < nodes.length)
    //             sb.append(' ');
    //     }
    //     return sb.toString();
    // }

    // /**
    //  * {@inheritDoc}
    //  */
    // public String prettyPrintText() {
    //     Pattern punctuation = Pattern.compile("[!,-.:;?`]");
    //     StringBuilder sb = new StringBuilder(nodes.length * 8);
    //     boolean evenSingleQuote = false;
    //     boolean evenDoubleQuote = false;
    //     // For quotations
    //     boolean skipSpace = false;
    //     for (int i = 0; i < nodes.length; ++i) {
    //         String token = nodes[i].word();
    //         // If the first token, append it to start the text
    //         if (i == 0)
    //             sb.append(token);
    //         // For all other tokens, decide whether to add a space between this
    //         // token and the preceding.
    //         else {
    //             // If the token is punctuation, or is a contraction, e.g., 's or
    //             // n't, then append it directly
    //             if (punctuation.matcher(nodes[i].pos()).matches()
    //                     || punctuation.matcher(token).matches()
    //                     || token.equals(".") // special case for end of sentence
    //                     || token.equals("n't")
    //                     || token.equals("'m")
    //                     || token.equals("'ll")
    //                     || token.equals("'re")
    //                     || token.equals("'ve")
    //                     || token.equals("'s"))
    //                 sb.append(token);
    //             else if (token.equals("'")) {
    //                 if (evenSingleQuote) 
    //                     sb.append(token);
    //                 else {
    //                     sb.append(' ').append(token);
    //                     skipSpace = true;
    //                 }
    //                 evenSingleQuote = !evenSingleQuote;
    //             }
    //             else if (token.equals("\"")) {
    //                 if (evenDoubleQuote)
    //                     sb.append(token);
    //                 else {
    //                     sb.append(' ').append(token);
    //                     skipSpace= true;
    //                 }
    //                 evenDoubleQuote = !evenDoubleQuote;
    //             }
    //             else if (token.equals("$")) {
    //                 sb.append(' ').append(token);
    //                 skipSpace= true;                    
    //             }
    //             // For non-punctuation tokens
    //             else {
    //                 if (skipSpace) {
    //                     sb.append(token);
    //                     skipSpace = false;
    //                 }
    //                 else 
    //                     sb.append(' ').append(token);
    //             }
    //         }
    //     }
    //     return sb.toString();
    // }
}
