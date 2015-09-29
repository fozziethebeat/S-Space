/*
 * Copyright 2012 David Jurgens
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

import edu.ucla.sspace.dependency.DependencyTreeNode;

import java.io.BufferedReader;


/**
 * An abstraction for a document that has been (or will be) dependency parsed to
 * generate an accompanying parse tree of its contents.
 */
public interface ParsedDocument {

    /**
     * Returns the dependency tree of the next document as a sequence of {@link
     * DependencyTreeNode} instances.
     */
    DependencyTreeNode[] parsedDocument();

    /**
     * Returns the text of the parsed document without any of the
     * parsing-related annotation, with each parsed token separated by
     * whitespace.
     */
    String text();

    /**
     * Returns a pretty-printed version of the document's text without any of
     * the parsing-related annotation and using heuristics to appropriately
     * space punctuation, quotes, and contractions.  This methods is intended as
     * only a useful way to displaying the document's text in a more readable
     * format than {@link #text()}, but makes no claims as to reproducing the
     * original surface form of the document prior to parsing.
     */
    String prettyPrintText();
}
