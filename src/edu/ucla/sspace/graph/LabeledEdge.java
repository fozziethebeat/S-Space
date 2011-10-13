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

public class LabeledEdge implements Edge, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final int from;

    private final int to;

    private final String fromLabel;
    
    private final String toLabel;

    public LabeledEdge(int from, int to, String fromLabel, String toLabel) {
        this.from = from;
        this.to = to;
        this.fromLabel = fromLabel;
        this.toLabel = toLabel;
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Edge> T clone(int from, int to) {
        return (T)(new LabeledEdge(from, to, fromLabel, toLabel));
    }   
    
    public boolean equals(Object o) {
        if (o instanceof Edge) {
            Edge e = (Edge)o;
            return (e.from() == from && e.to() == to)
                || (e.to() == from && e.from() == to);
        }
        return false;
    }

    public int hashCode() {
        return from ^ to;
    }

    @SuppressWarnings("unchecked")
    public <T extends Edge> T flip() {        
        return (T)(new LabeledEdge(to, from, toLabel, fromLabel));
    }

    public int from() {
        return from;
    }

    public int to() { 
        return to;
    }

    public String toString() {
        return "(" + fromLabel + "<-->" + toLabel + ")";
    }
}