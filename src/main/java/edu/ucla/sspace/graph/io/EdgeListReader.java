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
import edu.ucla.sspace.graph.DirectedTypedEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.SimpleDirectedEdge;
import edu.ucla.sspace.graph.SimpleDirectedTypedEdge;
import edu.ucla.sspace.graph.SimpleEdge;
import edu.ucla.sspace.graph.SimpleTypedEdge;
import edu.ucla.sspace.graph.SimpleWeightedEdge;
import edu.ucla.sspace.graph.SparseDirectedGraph;
import edu.ucla.sspace.graph.SparseWeightedGraph;
import edu.ucla.sspace.graph.SparseUndirectedGraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.UndirectedMultigraph;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;

import edu.ucla.sspace.util.HashIndexer;
import edu.ucla.sspace.util.Indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;


/**
 *
 */
public class EdgeListReader extends GraphReaderAdapter implements GraphReader {

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(EdgeListReader.class.getName());


    public EdgeListReader() { }

    @Override public Graph<Edge> readUndirected(File f) throws IOException {
        return readUndirected(f, new HashIndexer<String>());
    }

    @Override public Graph<Edge> readUndirected(File f, Indexer<String> vertexIndexer)
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
            g.add(new SimpleEdge(v1, v2));
            if (lineNo % 100000 == 0)
                verbose(LOGGER, "Read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read undirected graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }

    @Override public WeightedGraph<WeightedEdge> readWeighted(File f) throws IOException {
        return readWeighted(f, new HashIndexer<String>());
    }

    @Override public WeightedGraph<WeightedEdge> readWeighted(
            File f, Indexer<String> vertexIndexer) throws IOException {        

        BufferedReader br = new BufferedReader(new FileReader(f));
        WeightedGraph<WeightedEdge> g = new SparseWeightedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) 
                throw new IOException("Missing vertex on line " + lineNo);
            if (arr.length < 3) 
                throw new IOException("Missing edge weight on line " + lineNo);
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            double weight = Double.parseDouble(arr[2]);
            g.add(new SimpleWeightedEdge(v1, v2, weight));

            if (lineNo % 10000 == 0)
                veryVerbose(LOGGER, "Read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read directed graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }

    @Override public WeightedGraph<WeightedEdge> readWeighted(
            File f, Indexer<String> vertexIndexer, double minWeight) throws IOException {        

        BufferedReader br = new BufferedReader(new FileReader(f));
        WeightedGraph<WeightedEdge> g = new SparseWeightedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) 
                throw new IOException("Missing vertex on line " + lineNo);
            if (arr.length < 3) 
                throw new IOException("Missing edge weight on line " + lineNo);
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            double weight = Double.parseDouble(arr[2]);
            if (weight >= minWeight)
                g.add(new SimpleWeightedEdge(v1, v2, weight));

            if (lineNo % 100000 == 0)
                veryVerbose(LOGGER, "Read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read directed graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }

    /**
     * Reads in an undirected network from a file containing weighted edges,
     * only keeping those undirected edges whose weight was above the specified
     * threshold
     */
    @Override public Graph<Edge> readUndirectedFromWeighted(
            File f, Indexer<String> vertexIndexer, double minWeight) throws IOException {        

        BufferedReader br = new BufferedReader(new FileReader(f));
        Graph<Edge> g = new SparseUndirectedGraph();

        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) 
                throw new IOException("Missing vertex on line " + lineNo);
            if (arr.length < 3) 
                throw new IOException("Missing edge weight on line " + lineNo);
            int v1 = vertexIndexer.index(arr[0]);
            int v2 = vertexIndexer.index(arr[1]);
            double weight = Double.parseDouble(arr[2]);
            if (weight >= minWeight)
                g.add(new SimpleEdge(v1, v2));

            if (lineNo % 100000 == 0)
                veryVerbose(LOGGER, "Read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read directed graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }


    @Override public DirectedGraph<DirectedEdge> readDirected(File f) throws IOException {
        return readDirected(f, new HashIndexer<String>());
    }

    @Override public DirectedGraph<DirectedEdge> readDirected(File f, Indexer<String> vertexIndexer)
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
            if (lineNo % 100000 == 0)
                veryVerbose(LOGGER, "read %d lines from %s", lineNo, f);
        }
        verbose(LOGGER, "Read directed graph with %d vertices and %d edges", 
                g.order(), g.size());
        return g;
    }


    @Override public DirectedMultigraph<String> readDirectedMultigraph(File f) throws IOException {
        return readDirectedMultigraph(f, new HashIndexer<String>());
    }

    @Override public DirectedMultigraph<String> readDirectedMultigraph(
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
                veryVerbose(LOGGER, "read %d lines from %s, graph now has %d " + 
                            "vertices, %d edges, and %d types", lineNo, f, 
                            g.order(), g.size(), g.edgeTypes().size());
            }
        }
        verbose(LOGGER, "Read directed multigraph with %d vertices, %d edges, and %d types", 
                g.order(), g.size(), g.edgeTypes().size());
        return g;
    }

    @Override public UndirectedMultigraph<String> readUndirectedMultigraph(File f) throws IOException {
        return readUndirectedMultigraph(f, new HashIndexer<String>());
    }

    @Override public UndirectedMultigraph<String> readUndirectedMultigraph(
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
                veryVerbose(LOGGER, "read %d lines from %s, graph now has %d " + 
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
}