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
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;

import edu.ucla.sspace.util.ColorGenerator;
import edu.ucla.sspace.util.Indexer;

import edu.ucla.sspace.util.primitive.IntIterator;

import java.awt.Color;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import javax.xml.parsers.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


/**
 * An implementation for writing {@link Graph} instances as <a
 * href="http://gexf.net/format/">gexf</a>-formatted files, which are commonly
 * used with the <a href="http://gephi.org">Gephi</a> graph program.
 */
public class GexfIO {

    /**
     * Writes the {@link Graph} to file in {@code gexf} format.
     */
    public void write(Graph<? extends Edge> g, File gexfFile) 
            throws IOException {
        write(g, gexfFile, null);
    }

    /**
     * Writes the {@link Graph} to file in {@code gexf} format.
     */
    public void write(Graph<? extends Edge> g, 
                      File gexfFile, Indexer<String> vertexLabels) 
            throws IOException {

        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new IOError(new IOException(pce));
        }
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("gexf");
        root.setAttribute("xmlns","http://www.gexf.net/1.2draft");
        root.setAttribute("xmlns:viz", "http://www.gexf.net/1.2draft/viz");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("version","1.2");
        root.setAttribute("xsi:schemaLocation","http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd");
        doc.appendChild(root);

        Element graph = doc.createElement("graph");
        graph.setAttribute("defaultedgetype","undirected");
        root.appendChild(graph);

        Element nodes = doc.createElement("nodes");
        graph.appendChild(nodes);
        Element edges = doc.createElement("edges");
        graph.appendChild(edges);

        IntIterator vIter = g.vertices().iterator();
        while (vIter.hasNext()) {
            int vertex = vIter.next();
            Element node = doc.createElement("node");
            String vLabel = (vertexLabels == null)
                ? String.valueOf(vertex)
                : vertexLabels.lookup(vertex);
            if (vLabel == null)
                vLabel = String.valueOf(vertex);
            node.setAttribute("id", vLabel);
            node.setAttribute("label", vLabel);
            nodes.appendChild(node);
        }

        int edgeId = 0;
        for (Edge e : g.edges()) {
            Element edge = doc.createElement("edge");
            edges.appendChild(edge);
            edge.setAttribute("id", "" + (edgeId++));

            String sourceLabel = (vertexLabels == null)
                ? String.valueOf(e.from())
                : vertexLabels.lookup(e.from());
            if (sourceLabel == null)
                sourceLabel = String.valueOf(e.from());

            String targetLabel = (vertexLabels == null)
                ? String.valueOf(e.to())
                : vertexLabels.lookup(e.to());
            if (targetLabel == null)
                targetLabel = String.valueOf(e.to());

            edge.setAttribute("source", sourceLabel);
            edge.setAttribute("target", targetLabel);
        }

        // Set up a transformer
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
        
            // Create string from xml tree
            BufferedOutputStream bos = 
                new BufferedOutputStream(new FileOutputStream(gexfFile));
            StreamResult result = new StreamResult(bos);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            bos.close();         
        } catch (TransformerException te) {
            throw new IOError(new IOException(te));
        }
    }

    /**
     * Writes the provided multigraph to the specified {@code gexf} file with
     * all edge colors appearing the same.
     *
     * @param f the file where the {@code gexf} graph will be written.  Any existing file
     *        contents will be overwritten
         */
    public <T,E extends TypedEdge<T>> void write(
                        Multigraph<T,E> g, File f) throws IOException {
        write(g, f, null, false, null, false);
    }

    /**
     * Writes the provided multigraph to the specified {@code gexf} file, using
     * {@code edgeColors} as a guide for how to display parallel edges of
     * different types.
     *
     * @param f the file where the {@code gexf} graph will be written.  Any
     *        existing file contents will be overwritten
     * @param edgeColor a mapping from an edge type to a color.  Types that do
     *        not have colors will be randomly assigned one and the {@code
     *        edgeColors} map will be updated appropriately.
     */
    public <T,E extends TypedEdge<T>> void write(
                        Multigraph<T,E> g, File f, Map<T,Color> edgeColors) 
                        throws IOException {
        write(g, f, edgeColors, true, null, false);
    }

    /**
     * Writes the provided multigraph to the specified {@code gexf} file, using
     * {@code vertexLabels} to name the vertices.
     *
     * @param f the file where the {@code gexf} graph will be written.  Any
     *        existing file contents will be overwritten
     * @param vertexLabels a mapping from vertex value to a string name for the
     *        vertex
     */
    public <T,E extends TypedEdge<T>> void write(
                        Multigraph<T,E> g, File f, 
                        Indexer<String> vertexLabels) 
                        throws IOException {
        write(g, f, null, false, vertexLabels, true);
    }

    /**
     * Writes the provided multigraph to the specified {@code gexf} file, using
     * {@code edgeColors} as a guide for how to display parallel edges of
     * different types, and {@code vertexLabels} to name the vertices.
     *
     * @param f the file where the {@code gexf} graph will be written.  Any
     *        existing file contents will be overwritten
     * @param edgeColor a mapping from an edge type to a color.  Types that do
     *        not have colors will be randomly assigned one and the {@code
     *        edgeColors} map will be updated appropriately.
     * @param vertexLabels a mapping from vertex value to a string name for the
     *        vertex
     */
    public <T,E extends TypedEdge<T>> void write(
                        Multigraph<T,E> g, File f, Map<T,Color> edgeColors, 
                        Indexer<String> vertexLabels) 
                        throws IOException {
        write(g, f, edgeColors, true, vertexLabels, true);
    }

    /**
     * Writes the provided multigraph to the specified {@code gexf} file,
     * optionally coloring the edges if specified.
     */
    private <T,E extends TypedEdge<T>> void write(
                         Multigraph<T,E> g, File f, Map<T,Color> edgeColors, 
                         boolean useColors, Indexer<String> vertexLabels,
                         boolean useLabels) throws IOException {

        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new IOError(new IOException(pce));
        }
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("gexf");
        root.setAttribute("xmlns","http://www.gexf.net/1.2draft");
        root.setAttribute("xmlns:viz", "http://www.gexf.net/1.2draft/viz");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("version","1.2");
        root.setAttribute("xsi:schemaLocation","http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd");
        doc.appendChild(root);

        Element graph = doc.createElement("graph");
        graph.setAttribute("defaultedgetype","undirected");
        root.appendChild(graph);

        Element nodes = doc.createElement("nodes");
        graph.appendChild(nodes);
        Element edges = doc.createElement("edges");
        graph.appendChild(edges);

        IntIterator vIter = g.vertices().iterator();
        while (vIter.hasNext()) {
            int vertex = vIter.next();
            Element node = doc.createElement("node");
            node.setAttribute("id", String.valueOf(vertex));
            if (useLabels) 
                node.setAttribute("label", vertexLabels.lookup(vertex));
            else
                node.setAttribute("label", String.valueOf(vertex));
            nodes.appendChild(node);
        }

        ColorGenerator cg = null;
        if (useColors)
            cg = new ColorGenerator();

        int edgeId = 0;
        for (E e : g.edges()) {
            Element edge = doc.createElement("edge");
            edges.appendChild(edge);
            edge.setAttribute("id", "" + (edgeId++));
            edge.setAttribute("source", String.valueOf(e.from()));
            edge.setAttribute("target", String.valueOf(e.to()));
            edge.setAttribute("label", String.valueOf(e.edgeType()));
            if (useColors) {
                Element cEdge = doc.createElement("viz:color");
                edge.appendChild(cEdge);
                Color c = edgeColors.get(e.edgeType());
                if (c == null) {
                    c = cg.next();
                    edgeColors.put(e.edgeType(), c);
                }
                cEdge.setAttribute("r", String.valueOf(c.getRed()));
                cEdge.setAttribute("g", String.valueOf(c.getGreen()));
                cEdge.setAttribute("b", String.valueOf(c.getBlue()));
            }
        }

        // Set up a transformer
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
        
            // Create string from xml tree
            BufferedOutputStream bos = 
                new BufferedOutputStream(new FileOutputStream(f));
            StreamResult result = new StreamResult(bos);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            bos.close();         
        } catch (TransformerException te) {
            throw new IOError(new IOException(te));
        }
    }

    /**
     * Writes the {@link WeightedGraph} to file in {@code gexf} format.
     */
    public void write(WeightedGraph<? extends WeightedEdge> g, File gexfFile) 
            throws IOException {
        write(g, gexfFile, null);
    }

    /**
     * Writes the {@link WeightedGraph} to file in {@code gexf} format.
     */
    public void write(WeightedGraph<? extends WeightedEdge> g, 
                      File gexfFile, Indexer<String> vertexLabels) 
            throws IOException {

        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = null;
        try {
            docBuilder = dbfac.newDocumentBuilder();
        } catch (ParserConfigurationException pce) {
            throw new IOError(new IOException(pce));
        }
        Document doc = docBuilder.newDocument();

        Element root = doc.createElement("gexf");
        root.setAttribute("xmlns","http://www.gexf.net/1.2draft");
        root.setAttribute("xmlns:viz", "http://www.gexf.net/1.2draft/viz");
        root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        root.setAttribute("version","1.2");
        root.setAttribute("xsi:schemaLocation","http://www.gexf.net/1.2draft http://www.gexf.net/1.2draft/gexf.xsd");
        doc.appendChild(root);

        Element graph = doc.createElement("graph");
        graph.setAttribute("defaultedgetype","undirected");
        root.appendChild(graph);

        Element nodes = doc.createElement("nodes");
        graph.appendChild(nodes);
        Element edges = doc.createElement("edges");
        graph.appendChild(edges);

        IntIterator vIter = g.vertices().iterator();
        while (vIter.hasNext()) {
            int vertex = vIter.next();
            Element node = doc.createElement("node");
            String vLabel = (vertexLabels == null)
                ? String.valueOf(vertex)
                : vertexLabels.lookup(vertex);
            if (vLabel == null)
                vLabel = String.valueOf(vertex);
            node.setAttribute("id", vLabel);
            node.setAttribute("label", vLabel);
            nodes.appendChild(node);
        }

        int edgeId = 0;
        for (WeightedEdge e : g.edges()) {
            Element edge = doc.createElement("edge");
            edges.appendChild(edge);
            edge.setAttribute("id", "" + (edgeId++));

            String sourceLabel = (vertexLabels == null)
                ? String.valueOf(e.from())
                : vertexLabels.lookup(e.from());
            if (sourceLabel == null)
                sourceLabel = String.valueOf(e.from());

            String targetLabel = (vertexLabels == null)
                ? String.valueOf(e.to())
                : vertexLabels.lookup(e.to());
            if (targetLabel == null)
                targetLabel = String.valueOf(e.to());

            edge.setAttribute("source", sourceLabel);
            edge.setAttribute("target", targetLabel);
            edge.setAttribute("weight", String.valueOf(e.weight()));
        }

        // Set up a transformer
        try {
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(
                "{http://xml.apache.org/xslt}indent-amount", "2");
        
            // Create string from xml tree
            BufferedOutputStream bos = 
                new BufferedOutputStream(new FileOutputStream(gexfFile));
            StreamResult result = new StreamResult(bos);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            bos.close();         
        } catch (TransformerException te) {
            throw new IOError(new IOException(te));
        }
    }

}
