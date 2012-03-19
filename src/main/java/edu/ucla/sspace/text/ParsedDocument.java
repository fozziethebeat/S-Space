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

import java.util.Iterator;


/**
 * A {@link Document} implementation that centers around a parsed array of
 * {@link DependencyTreeNode}s.  {@link Iterator}s over tokens will be created
 * based on the tokenization provided by the parsed tree.
 */
public class ParsedDocument implements Document {

    private final DependencyTreeNode[] tree;

    private final String title;

    private final long timestamp;

    public ParsedDocument(DependencyTreeNode[] tree) {
        this(tree, "", System.currentTimeMillis());
    }

    public ParsedDocument(DependencyTreeNode[] tree, String title) {
        this(tree, title, System.currentTimeMillis());
    }

    public ParsedDocument(DependencyTreeNode[] tree,
                          String title,
                          long timestamp) {
        this.tree = tree;
        this.title = title;
        this.timestamp = timestamp;
    }

    public String title() {
        return title;
    }

    public long timeStamp() {
        return timestamp;
    }

    public DependencyTreeNode[] parseTree() {
        return tree;
    }

    public Iterator<String> iterator() {
        return new TreeTokenIterator(tree);
    }

    private static class TreeTokenIterator implements Iterator<String> {

        private int index;

        private final DependencyTreeNode[] tree;

        public TreeTokenIterator(DependencyTreeNode[] tree) {
            this.tree = tree;
            this.index = 0;
        }

        public boolean hasNext() {
            return index < tree.length;
        }

        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove elements from a TreeTokenIterator");
        }

        public String next() {
            return tree[index++].word();
        }
    }
}
