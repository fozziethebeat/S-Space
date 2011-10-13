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

import java.awt.Color;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A class for reading graph instances from <a
 * href="http://en.wikipedia.org/wiki/DOT_language">DOT</a> graph files.
 */
public class DotIO {

    private final Indexer<String> indexer;

    private final Pattern UNDIRECTED_EDGE = Pattern.compile("\\s*([\\-\\w]+)\\s*--\\s*([\\-\\w]+)\\s*(\\[.*\\])\\s*;");
    private final Pattern DIRECTED_EDGE = //Pattern.compile("\\s*([\\-\\w]+)\\s*->\\s*([\\-\\w]+)\\s*(\\[.*\\])\\s*;");
        Pattern.compile("\\s*([\\-\\w]+)\\s*->\\s*([\\-\\w]+)\\s*;");

    /**
     * A mapping from an edge type to the color used to illustrate it
     */
    private static final Map<Object,Color> EDGE_COLORS =
        new HashMap<Object,Color>();


    public DotIO() {
        this(new ObjectIndexer<String>());
    }

    public DotIO(Indexer<String> indexer) {
        this.indexer = indexer;
    }

    public Graph readGraph(File dotFile) {
        return null;
    }

    public DirectedGraph<DirectedEdge> readDirectedGraph(File dotFile) {
        DirectedGraph<DirectedEdge> g = new SparseDirectedGraph();
        // REMINDER: add a lot more error checking
        for (String line : new LineReader(dotFile)) {
            // System.out.println(line);
            Matcher m = DIRECTED_EDGE.matcher(line);
            while (m.find()) {
                String from = m.group(1);
                String to = m.group(2);
                //String meta = m.group(3);
                // System.out.printf("%s -> %s (%s)%n", from, to, meta);
                g.add(new SimpleDirectedEdge(indexer.index(from), 
                                             indexer.index(to)));
            }
        }
        return g;
    }

    public DirectedMultigraph readDirectedMultigraph(File dotFile) {
        DirectedMultigraph<String> g = new DirectedMultigraph<String>();
        // REMINDER: add a lot more error checking
        for (String line : new LineReader(dotFile)) {
            Matcher m = DIRECTED_EDGE.matcher(line);
            while (m.find()) {
                String from = m.group(1);
                String to = m.group(2);
                String meta = m.group(3);
                Pattern type = Pattern.compile("label=\"(\\w+)\"");
                Matcher m2 = type.matcher(meta);
                if (!m2.find())
                    throw new Error("could not find label: " + line);
                String label = m2.group(1);
                // System.out.printf("%s -> %s%n", from, to);
                g.add(new SimpleDirectedTypedEdge<String>(
                          label, indexer.index(from), indexer.index(to)));
            }
        }
        return g;

    }

    public <E extends Edge> void writeUndirectedGraph(Graph<E> g, File f) 
            throws IOException {

        PrintWriter pw = new PrintWriter(f);
        pw.println("graph g {");
        // Write the vertices, which may be disconnected
        for (int v : g.vertices()) 
            pw.println("  " + v + ";");
        pw.println();
        // Write the edges that connect the vertices
        for (E e : g.edges()) 
            pw.printf("  %d -- %d;%n", e.from(), e.to());
        
        pw.println("}");
        pw.close();
    }

    public <E extends DirectedEdge> void writeDirectedGraph(DirectedGraph<E> g,
                                                            File f) 
            throws IOException {

        PrintWriter pw = new PrintWriter(f);
        pw.println("digraph g {");
        // Write the vertices, which may be disconnected
        for (int v : g.vertices()) 
            pw.println("  " + v + ";");
        pw.println();
        // Write the edges that connect the vertices
        for (E e : g.edges()) 
            pw.printf("  %d -> %d;%n", e.from(), e.to());
        
        pw.println("}");
        pw.close();
    }

    public <T,E extends TypedEdge<T>> void writeUndirectedMultigraph(
                        Multigraph<T,E> g, File f) throws IOException {
        Map<T,Color> edgeColors = new HashMap<T,Color>();
        ColorGenerator cg = new ColorGenerator();
        for (T type : g.edgeTypes())
            edgeColors.put(type, cg.next());
        this.writeUndirectedMultigraph(g, f, edgeColors);
    }

    /**
     * Writes the provided multigraph to the specified DOT file, using {@code
     * edgeColors} as a guide for how to display parallel edges of different
     * types.
     *
     * @param f the file where the DOT graph will be written.  Any existing file
     *        contents will be overwritten
     * @param edgeColor a mapping from an edge type to a color.  Types that do
     *        not have colors will be randomly assigned one and the {@code
     *        edgeColors} map will be updated appropriately.
     */
    public <T,E extends TypedEdge<T>> void writeUndirectedMultigraph(
                        Multigraph<T,E> g, File f, Map<T,Color> edgeColors) 
                        throws IOException {
        PrintWriter pw = new PrintWriter(f);
        ColorGenerator cg = new ColorGenerator();
        pw.println("graph g {");
        // Write the vertices, which may be disconnected
        for (int v : g.vertices()) {
            pw.println("  " + v + ";");
        }
        for (E e : g.edges()) {
            Color c = edgeColors.get(e.edgeType());
            if (c == null) {
                c = cg.next();
                edgeColors.put(e.edgeType(), c);
            }
            String hexColor = Integer.toHexString(c.getRGB());
            hexColor = hexColor.substring(2, hexColor.length());
            pw.printf("  %d -- %d [label=\"%s\", color=\"#%s\"]%n", e.from(), e.to(),
                      e.edgeType(), hexColor);
        }
        pw.println("}");
        pw.close();
    }

    public <T,E extends DirectedTypedEdge<T>> void writeDirectedMultigraph(
                        Multigraph<T,E> g, File f) throws IOException {
        Map<T,Color> edgeColors = new HashMap<T,Color>();
        ColorGenerator cg = new ColorGenerator();
        for (T type : g.edgeTypes())
            edgeColors.put(type, cg.next());
        this.writeDirectedMultigraph(g, f, edgeColors);
    }

    public <T,E extends DirectedTypedEdge<T>> void writeDirectedMultigraph(
            Multigraph<T,E> g, File f, Map<T,Color> edgeColors) 
            throws IOException {
    
        PrintWriter pw = new PrintWriter(f);
        pw.println("digraph g {");
        // Write the vertices, which may be disconnected
        for (int v : g.vertices()) {
            pw.println("  " + v + ";");
        }
        for (E e : g.edges()) {
            String hexColor = Integer.toHexString(
                edgeColors.get(e.edgeType()).getRGB());            
            hexColor = hexColor.substring(2, hexColor.length());
            pw.printf("  %d -> %d [label=\"%s\", color=\"#%s\"]%n", e.from(), e.to(),
                      e.edgeType(), hexColor);
        }
        pw.println("}");
        pw.close();
    }
}