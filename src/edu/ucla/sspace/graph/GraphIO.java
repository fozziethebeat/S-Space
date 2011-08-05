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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.LineReader;
import edu.ucla.sspace.util.ObjectIndexer;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * A collection of static utility methods for reading and writing graphs.
 */
public class GraphIO {

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(GraphIO.class.getName());

    public enum GraphType {
        UNDIRECTED,
        DIRECTED,
        WEIGHTED,
        MULTIGRAPH
    }

    public enum GraphFileFormat {
        PAJEK
    }

    private GraphIO() { }

    public static Graph<? extends Edge> read(File f, GraphType type) throws IOException {
        switch (type) {
        case UNDIRECTED:
            return readUndirected(f);
        case DIRECTED:
            return readDirected(f);
        default: 
            throw new Error("Reading GraphType " + type + " is current unsupported");
        }
    }

    public static Graph<Edge> readUndirected(File f) throws IOException {
        return readUndirected(f, new ObjectIndexer<String>());
    }

    public static Graph<Edge> readUndirected(File f, Indexer<String> vertexIndexer)
            throws IOException {        
        BufferedReader br = new BufferedReader(new FileReader(f));
        Graph<Edge> g = //new GenericGraph<Edge>();
            new SparseUndirectedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) {
                throw new IOException("Missing vertex on line " + lineNo);
            }
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            g.add(new LabeledEdge(v1, v2, arr[0], arr[1]));
            if (lineNo % 100000 == 0)
                verbose(LOGGER, "read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read undirected graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }

    public static DirectedGraph<DirectedEdge> readDirected(File f) throws IOException {
        return readDirected(f, new ObjectIndexer<String>());
    }

    public static DirectedGraph<DirectedEdge> readDirected(File f, Indexer<String> vertexIndexer)
            throws IOException {        

        BufferedReader br = new BufferedReader(new FileReader(f));
        DirectedGraph<DirectedEdge> g = new SparseDirectedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) {
                throw new IOException("Missing vertex on line " + lineNo);
            }
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            g.add(new SimpleDirectedEdge(v1, v2));
        }
        verbose(LOGGER, "Read directed graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }


    public static DirectedMultigraph<String> readDirectedMultigraph(File f) throws IOException {
        return readDirectedMultigraph(f, new ObjectIndexer<String>());
    }

    public static DirectedMultigraph<String> readDirectedMultigraph(
            File f, Indexer<String> vertexIndexer) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(f));
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 3) {
                throw new IOException("Missing vertex or type on line " + lineNo);
            }
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            String type = arr[2];
            g.add(new SimpleDirectedTypedEdge<String>(type, v1, v2));

            if (lineNo % 100000 == 0) {
                verbose(LOGGER, "read %d lines from %s, graph now has %d " + 
                        "vertices, %d edges, and %d types", lineNo, f, 
                        g.order(), g.size(), g.edgeTypes().size());
            }
        }
        verbose(LOGGER, "Read directed multigraph with %d vertices, %d edges, and %d types", 
                g.order(), g.size(), g.edgeTypes().size());
        return g;
    }

    public static UndirectedMultigraph<String> readUndirectedMultigraph(File f) throws IOException {
        return readUndirectedMultigraph(f, new ObjectIndexer<String>());
    }

    public static UndirectedMultigraph<String> readUndirectedMultigraph(
            File f, Indexer<String> vertexIndexer) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(f));
        UndirectedMultigraph<String> g = new UndirectedMultigraph<String>();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 3) {
                throw new IOException("Missing vertex or type on line " + lineNo);
            }
            if (arr[0].equals(arr[1])) {
                System.out.println("skipping self edge: " + line);
                continue;
            }                
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            String type = arr[2];
            g.add(new SimpleTypedEdge<String>(type, v1, v2));

            if (lineNo % 100000 == 0) {
                verbose(LOGGER, "read %d lines from %s, graph now has %d " + 
                        "vertices, %d edges, and %d types", lineNo, f, 
                        g.order(), g.size(), g.edgeTypes().size());
            }
        }
        if (g.order() != vertexIndexer.highestIndex() + 1) {
            System.out.printf("%d != %d%n", g.order(), vertexIndexer.highestIndex());
            throw new Error();
        }
        verbose(LOGGER, "Read undirected multigraph with %d vertices, %d edges, and %d types", 
                g.order(), g.size(), g.edgeTypes().size());
        return g;
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
}