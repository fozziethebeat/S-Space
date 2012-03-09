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

package edu.ucla.sspace.dependency;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;

/**
 * A {@link DependencyPath} that supports constant time access to the nodes and
 * relations that make up its sequence.
 */
public class SimpleDependencyPath implements DependencyPath {
    
    /**
     * The list of terms and relations.
     */
    final List<DependencyRelation> path;

    /**
     * The list of terms and relations.
     */
    final List<DependencyTreeNode> nodes;

    /**
     * Creates a {@link SimpleDependencyPath} starting at the head node of the
     * first relation in the list.
     */
    public SimpleDependencyPath(List<DependencyRelation> path) {
        this(path, true);
    }

    /**
     * Creates a {@link SimpleDependencyPath} that starts at either the head
     * node or dependent node of the first relation in the list.
     */
    public SimpleDependencyPath(List<DependencyRelation> path, 
                                boolean isHeadFirst) {
        if (path == null || path.size() == 0)
            throw new IllegalArgumentException("Cannot provide empty path");
        this.path = new ArrayList<DependencyRelation>(path);        
        this.nodes = new ArrayList<DependencyTreeNode>(path.size() + 1);
        DependencyTreeNode cur = (isHeadFirst)
            ? path.get(0).headNode() : path.get(0).dependentNode();
        nodes.add(cur);
        for (DependencyRelation r : path) {
            DependencyTreeNode next = r.headNode();
            // If the head node is the last node we saw, then the dependent node
            // must be the next node in the path
            if (next.equals(cur)) 
                next = r.dependentNode();
            nodes.add(next);
            cur = next;
        }
    }

    /**
     * Creates new {@link SimpleDependencyPath} as a copy of the provided path.
     */
    public SimpleDependencyPath(DependencyPath path) {
        if (path == null || path.length() == 0)
            throw new IllegalArgumentException("Cannot provide empty path");

        int size = path.length();
        this.path = new ArrayList<DependencyRelation>(size);
        this.nodes = new ArrayList<DependencyTreeNode>(size + 1);
        DependencyTreeNode cur = path.first();
        nodes.add(cur);
        for (DependencyRelation r : path) {
            this.path.add(r);
            DependencyTreeNode next = r.headNode();
            // If the head node is the last node we saw, then the dependent node
            // must be the next node in the path
            if (next.equals(cur)) 
                next = r.dependentNode();
            nodes.add(next);
            next = cur;
        }
    }       

    /**
     * Creates a {@link SimpleDependencyPath} from a single relation, optionally
     * starting at the head node of the relation.
     */
    public SimpleDependencyPath(DependencyRelation relation, 
                                boolean startFromHead) {
        this();
        if (relation == null)
            throw new IllegalArgumentException("Cannot provide empty path");
        path.add(relation);
        if (startFromHead) {
            nodes.add(relation.headNode());
            nodes.add(relation.dependentNode());
        }
        else {
            nodes.add(relation.dependentNode());
            nodes.add(relation.headNode());           
        }
    }

    /**
     * Creates an empty dependency path
     */
    public SimpleDependencyPath() { 
        path = new ArrayList<DependencyRelation>();
        nodes = new ArrayList<DependencyTreeNode>();
    }

    /**
     * Returns a copy of this dependency path
     */
    public SimpleDependencyPath copy() {
        SimpleDependencyPath copy = new SimpleDependencyPath();
        copy.path.addAll(path);
        copy.nodes.addAll(nodes);
        return copy;
    }

    /**
     * Returns a copy of this dependency path that has the provided related
     * appended to the end of its path sequence.
     */
    public SimpleDependencyPath extend(DependencyRelation relation) {
        SimpleDependencyPath copy = copy();
        // Figure out which node is at the end of our path, and then add the new
        // node to the end of our nodes
        DependencyTreeNode last = last(); 
        copy.nodes.add((relation.headNode().equals(last)) 
                       ? relation.dependentNode() : relation.headNode());
        copy.path.add(relation);
        return copy;
    }

    public boolean equals(Object o) {
        if (o instanceof DependencyPath) {
            DependencyPath p = (DependencyPath)o;
            if (p.length() != length())
                return false;
            DependencyTreeNode f = p.first();
            DependencyTreeNode n = first();
            if (!(f == n || (f != null && f.equals(n))))
                return false;
            Iterator<DependencyRelation> it1 = iterator();
            Iterator<DependencyRelation> it2 = p.iterator();
            while (it1.hasNext())
                if (!(it1.next().equals(it2.next())))
                    return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode first() {
        return (nodes.isEmpty()) ? null : nodes.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyRelation firstRelation() {
        return (path.isEmpty()) ? null : path.get(0);
    }    

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode getNode(int position) {
        if (position < 0 || position >= nodes.size())
            throw new IndexOutOfBoundsException("Invalid node: " + position);
        return nodes.get(position);
    }

    /**
     * {@inheritDoc}
     */
    public String getRelation(int position) {
        if (position < 0 || position >= path.size())
            throw new IndexOutOfBoundsException("Invalid relation: " +position);
        DependencyRelation r = path.get(position);
        return r.relation();
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<DependencyRelation> iterator() {
        return path.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public DependencyTreeNode last() {
        return nodes.get(nodes.size() - 1);
    }

    /**
     * {@inheritDoc}
     */
    public DependencyRelation lastRelation() {
        return path.get(path.size() - 1);
    }    
    
    /**
     * {@inheritDoc}
     */
    public int length() {
        return path.size();
    }

    /**
     * Returns the path in order with words and relations space delimited.
     */
    public String toString() {
         int size = nodes.size();
         StringBuilder sb = new StringBuilder(8 * size);
         sb.append(nodes.get(0).word());
         for (int i = 1; i < size; ++i)
             sb.append(' ')
                 .append(path.get(i-1).relation())
                 .append(' ').append(nodes.get(i).word());
         return sb.toString();
    }
}
