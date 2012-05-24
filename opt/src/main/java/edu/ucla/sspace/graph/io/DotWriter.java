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

package edu.ucla.sspace.graph.io;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;

import edu.ucla.sspace.util.Indexer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * A class for reading graph instances from <a
 * href="http://en.wikipedia.org/wiki/DOT_language">DOT</a> graph files.
 */
public class DotWriter {

    //private final Indexer<String> indexer;
    private final Map<Integer,String> vertexLabels;

    public DotWriter() {
        vertexLabels = null;
    }

    public DotWriter(Indexer<String> indexer) {
        this(indexer.mapping());
    }

    public DotWriter(Map<Integer,String> vertexLabels) {
        this.vertexLabels = vertexLabels;
    }

    public void write(Graph<? extends Edge> g, File f) throws IOException {
        PrintWriter pw = new PrintWriter(f);
        pw.println("graph g {");
        String vertexLabel = null;
        for (int v : g.vertices()) {
            pw.print("\t" + v);
            if (vertexLabels != null && (vertexLabel = vertexLabels.get(v)) != null) {
                pw.printf(" [label=\"%s\"]", vertexLabel);
            }
            pw.println(';');
        }
        for (Edge e : g.edges()) {
            pw.printf("\t%d -- %d\n", e.from(), e.to());
        }
        pw.println("}");
        pw.close();
    }

    public void write(DirectedGraph<? extends DirectedEdge> g, File f) 
            throws IOException {
        this.write(g, f, Collections.<Set<Integer>>emptySet());
    }

    public void write(DirectedGraph<? extends DirectedEdge> g, File f, 
                      Collection<Set<Integer>> groups) throws IOException {
        PrintWriter pw = new PrintWriter(f);
        pw.println("digraph g {");
        String vertexLabel = null;
        for (int v : g.vertices()) {
            pw.print("\t" + v);
            if (vertexLabels != null && (vertexLabel = vertexLabels.get(v)) != null) {
                pw.printf(" [label=\"%s\"]", vertexLabel);
            }
            pw.println(';');
        }
        for (DirectedEdge e : g.edges()) {
            pw.printf("\t%d -> %d\n", e.from(), e.to());
        }
        if (!groups.isEmpty()) {
            for (Set<Integer> group : groups) {
                pw.print("\t{ rank=same; ");
                for (Integer i : group) {
                    pw.print(i);
                    pw.print(' ');
                }
                pw.println('}');
            }
        }
        pw.println("}");
        pw.close();
    }

    public <T> void write(Multigraph<T, ? extends TypedEdge<T>> g, File f) 
            throws IOException {
        PrintWriter pw = new PrintWriter(f);
        pw.println("graph g {");
        String vertexLabel = null;
        for (int v : g.vertices()) {
            pw.print("\t" + v);
            if (vertexLabels != null && (vertexLabel = vertexLabels.get(v)) != null) {
                pw.printf(" [label=%s]", vertexLabel);
            }
            pw.println(';');
        }
        for (TypedEdge<T> e : g.edges()) {
            pw.printf("\t%d -- %d [label=\"%s\"]\n", e.from(), e.to(), e.edgeType());
        }
        pw.println("}");
        pw.close();
    }
    
}