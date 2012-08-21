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

package edu.ucla.sspace.graph.io;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.WeightedDirectedMultigraph;
import edu.ucla.sspace.graph.WeightedEdge;

import edu.ucla.sspace.util.Indexer;

import edu.ucla.sspace.util.primitive.IntIterator;
import edu.ucla.sspace.util.primitive.IntPair;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * A class for writing graph instances to files in Pajek's <a
 * href="https://gephi.org/users/supported-graph-formats/pajek-net-format/">NET
 * format</a>.
 */
public class PajekWriter {

    public PajekWriter() { }

    /**
     * Writes this {@link WeightedDirectedMultigraph} instance to the specified
     * file.  Note that because the Pajek format does not support parallel
     * edges, all parallel edges in one direction are flatted into a single
     * directed edge.
     */
    public <T> void write(WeightedDirectedMultigraph<T> g, File f) 
            throws IOException {
        write(g, f, null);
    }

    /**
     * Writes this {@link WeightedDirectedMultigraph} instance to the specified
     * file using the provided {@link Indexer} to lookup labels for the graph's
     * vertices.  Note that because the Pajek format does not support parallel
     * edges, all parallel edges in one direction are flatted into a single
     * directed edge.
     *
     * @param vertexLabels the {@code Indexer} used to look up labels for the
     *        vertices.  If {@link Indexer#lookup(int)} returns {@code null} no
     *        label is supplied for the vertex.
     */
    public <T> void write(WeightedDirectedMultigraph<T> g, File f, 
                          Indexer<String> vertexLabels) 
            throws IOException {

        PrintWriter pw = new PrintWriter(f);
        pw.println("*Vertices " + g.order());
        if (vertexLabels != null) {
            IntIterator iter = g.vertices().iterator();
            while (iter.hasNext()) {
                int v = iter.nextInt();
                String label = vertexLabels.lookup(v);
                if (label != null)
                    pw.printf("%d \"%s\"%n", v, label);
            }
        }
        
        // We will flatten all the parallel edges together, which requires
        // iterative over the edges to see how many unique compacted edges there
        // are.  We need to know the number for the pajek formatting
        pw.println("*Edges");
        IntIterator iter = g.vertices().iterator();
        while (iter.hasNext()) {
            int v1 = iter.nextInt();
            IntIterator iter2 = g.getNeighbors(v1).iterator();
            while (iter2.hasNext()) {
                int v2 = iter2.nextInt();
                if (v1 < v2)
                    continue;
                Set<? extends WeightedEdge> edges = g.getEdges(v1, v2);
                double fromWeight = 0;
                double toWeight = 0;
                for (WeightedEdge e : edges) {
                    if (e.from() == v1)
                        fromWeight += e.weight();
                    else
                        toWeight += e.weight();
                }
                if (fromWeight != 0)
                    pw.printf("%d %d %f%n", v1, v2, fromWeight);
                if (toWeight != 0)
                    pw.printf("%d %d %f%n", v2, v1, toWeight);
            }
        }
        
        pw.close();
    }   
}