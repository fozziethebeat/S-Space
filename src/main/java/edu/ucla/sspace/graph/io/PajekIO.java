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
import edu.ucla.sspace.graph.DirectedTypedEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleDirectedEdge;
import edu.ucla.sspace.graph.SimpleDirectedTypedEdge;
import edu.ucla.sspace.graph.SparseDirectedGraph;
import edu.ucla.sspace.graph.TypedEdge;

import edu.ucla.sspace.util.ColorGenerator;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.LineReader;
import edu.ucla.sspace.util.ObjectIndexer;

import edu.ucla.sspace.util.primitive.IntIterator;

import java.awt.Color;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class for reading and writing graphs in the Pajek format.
 */
public class PajekIO {

    private final Indexer<String> vertexIndex;

    public PajekIO() {
        this(new ObjectIndexer<String>());
    }

    public PajekIO(Indexer<String> vertexIndex) {
        this.vertexIndex = vertexIndex;
    }

    public static Graph<DirectedEdge> readPajek(File f) throws IOException {
        Graph<DirectedEdge> g = new SparseDirectedGraph();
        int lineNo = 0;
        boolean seenVertices = false;
        boolean seenEdges = false;
        Map<String,Integer> labelToVertex = new HashMap<String,Integer>();

        for (String line : new LineReader(f)) {
            ++lineNo;
            // Skip comments and blank lines
            if (line.matches("\\s*%.*") || line.matches("\\s+"))
                continue;
            else if (line.startsWith("*vertices")) {
                if (seenVertices) {
                    throw new IOException("Duplicate vertices definiton on " +
                                          "line " + lineNo);
                }
                String[] arr = line.split("\\s+");
                if (arr.length < 2) 
                    throw new IOException("Missing specification of how many " +
                                          "vertices");
                int numVertices = -1;
                try {
                    numVertices = Integer.parseInt(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new IOException("Invalid number of vertices: " +
                                          arr[1], nfe);
                }
                if (numVertices < 1)
                    throw new IOException("Must have at least one vertex");

                // Add the vertices to the graph
                for (int i = 0; i < numVertices; ++i)
                    g.add(i);

                seenVertices = true;
            }
            else if (line.startsWith("*edges") 
                     || line.startsWith("*arcs")) {
                if (!seenVertices)
                    throw new IOException("Must specify vertices before edges");
                if (seenEdges) 
                    throw new IOException("Duplicate edges definition on line" 
                                          + lineNo);
                seenEdges = true;
            }
            // If the edges flag is true all subsequent lines should be an edge
            // specifaction
            else if (seenEdges) { 
                String[] arr = line.split("\\s+");
                if (arr.length < 2) 
                    throw new IOException("Missing vertex declaration(s) for " +
                                          "edge definition: " + line);
                int v1 = -1;
                int v2 = -1;
                try {
                    v1 = Integer.parseInt(arr[0]);
                    v2 = Integer.parseInt(arr[1]);
                } catch (NumberFormatException nfe) {
                    throw new IOException("Invalid vertex value: " + line, nfe);
                }
                g.add(new SimpleDirectedEdge(v1, v2));
            }
            else if (seenVertices) {
                // Handle labels here?
            }
            else
                throw new IOException("Unknown line content type: " + line);
        }
        
        return g;
    }
 
    public <E extends Edge> void writeUndirectedGraph(Graph<E> g, File f) 
            throws IOException {

        PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        Indexer<Integer> vertexMap = new ObjectIndexer<Integer>();
        vertexMap.index(-1); // dummy since Pajek must start at 1
        pw.println("*Vertices " + g.order());
        // Write the vertices, which may be disconnected and must start at 1
        IntIterator iter = g.vertices().iterator();
        while (iter.hasNext()) {
            int v = iter.next(); 
            pw.println(vertexMap.index(v) + " \"" + vertexIndex.lookup(v) + "\"");
        }
        pw.println("*Edges " + g.size());
        // Write the edges that connect the vertices, noting that Pajek vertices
        // start at 1
        for (E e : g.edges()) 
            pw.println(vertexMap.index(e.from()) + " " + vertexMap.index(e.to()));
        pw.close();
    }
}