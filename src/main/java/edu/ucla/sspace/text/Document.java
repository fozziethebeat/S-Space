/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.  *
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

import java.util.Iterator;


/**
 * An abstraction for a document that allows document processors to access text
 * in a uniform manner.
 */
public interface Document extends Iterable<String> {
    
    /**
     * Returns the title of the document, or the empty string if no title was
     * provided in the document.
     */
    String title();

    /**
     * Returns the time at which this document was created.  If the source
     * data did not provide a timestamp, this should return the time at which
     * the {@link Document} object was created.
     */
    long timeStamp();

    /**
     * Returns an iterator over the tokens in a {@link Document}.
     */
    Iterator<String> iterator();

    /**
     * Returns a Dependency Tree over the text in this {@link Document}.  If a
     * full parse tree is not available, this should return a disconnected tree
     * of {@link DependencyTreeNode}s.
     */
    DependencyTreeNode[] parseTree();
}
