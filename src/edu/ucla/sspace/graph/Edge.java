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

package edu.ucla.sspace.graph;


/**
 * An interface for representing an undirected edge between two vertices.  The
 * {@code from} and {@code to} methods reflect only a naming convention used to
 * distinguish between the two vertices connected by this edge.
 *
 * <p> Note that futher refinements of the {@code Edge} interface may contain
 * further conditions for edge equality.  In order to guarantee symmetry of the
 * {@link Object#equals(Object) equals} method, implementations are encouraged
 * to implement equals using a symmetric check:
 *
 *<pre>
 *    public boolean equals(Object o) {
 *        if (o instanceof Edge) {
 *            Edge e = (Edge)o;
 *            boolean isEqual = true; // fill in with implementation specifics
 *            return isEqual &amp;&amp; o.equals(e); 
 *        }
 *    }
 *</pre>
 */
public interface Edge {

    /**
     * Clones the contents associated with this edge, returning the copy mapped
     * to the provided vertices.
     */
    <T extends Edge> T clone(int from, int to);

    /**
     * Returns {@code true} if {@code o} connects the same two vertices
     * regardless of the edge orientation.
     */
    boolean equals(Object o);

    /**
     * Returns a copy of this edge with the {@code from} and {@code to} vertices swapped.
     *
     * @param T the type of the {@link Edge} instance returned.  <b>In almost
     *        every use-case imaginable, {@code T} should be set to the type of
     *        {@code Edge} on which this method is called.</b>
     */
    <T extends Edge> T flip();

    /**
     * Returns the index of the tail vertex
     */
    int from();

    /**
     * Returns the index of the head vertex
     */
    int to();
    
}