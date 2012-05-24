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
import edu.ucla.sspace.graph.SimpleWeightedDirectedTypedEdge;
import edu.ucla.sspace.graph.SimpleWeightedEdge;
import edu.ucla.sspace.graph.SparseDirectedGraph;
import edu.ucla.sspace.graph.SparseWeightedGraph;
import edu.ucla.sspace.graph.SparseUndirectedGraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.UndirectedMultigraph;
import edu.ucla.sspace.graph.WeightedDirectedMultigraph;
import edu.ucla.sspace.graph.WeightedEdge;
import edu.ucla.sspace.graph.WeightedGraph;
import edu.ucla.sspace.graph.WeightedDirectedTypedEdge;

import edu.ucla.sspace.util.HashIndexer;
import edu.ucla.sspace.util.Indexer;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;

import static edu.ucla.sspace.util.LoggerUtil.verbose;
import static edu.ucla.sspace.util.LoggerUtil.veryVerbose;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.ParserAdapter;



/**
 * An {@link GraphReader} implementation that supports reading files in the <a
 * href="http://graphml.graphdrawing.org/primer/graphml-primer.html> GraphML
 * </a> language.
 */
public class GraphMLReader extends GraphReaderAdapter implements GraphReader {

    /**
     * The logger to which clustering status updates will be written.
     */
    private static final Logger LOGGER =
        Logger.getLogger(GraphMLReader.class.getName());


    public GraphMLReader() { }


    public WeightedDirectedMultigraph<String> readWeightedDirectedMultigraph(
            File f, Indexer<String> vertexLabels) throws IOException {        
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            ParserAdapter pa = new ParserAdapter(sp.getParser());
            
            GraphMLParser parser = new GraphMLParser(vertexLabels);
            
            pa.setContentHandler(parser);
            pa.setErrorHandler(parser);
            pa.parse(new InputSource(new BufferedInputStream(new FileInputStream(f))));
            return parser.g;
        } catch (SAXException saxe) {
            throw new IOException(saxe);
        } catch (ParserConfigurationException saxe) {
            throw new IOException(saxe);
        }
    }

    public WeightedDirectedMultigraph<String> readWeightedDirectedMultigraphFromDOM(
            File f, Indexer<String> vertexLabels) throws IOException {        

        try {
            DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbfac.newDocumentBuilder();
            Document graphDoc = db.parse(f);
            verbose(LOGGER, "Finished parsing %s", f);
            
            // <key id="nd0" for="node" attr.name="type" attr.type="int"><default>0</default></key>
            // <key id="nd1" for="node" attr.name="group" attr.type="int"><default>0</default></key>
            // <key id="sd0" for="edge" attr.name="weight" attr.type="double"><default>0.0</default></key>
            // <key id="sd1" for="edge" attr.name="type" attr.type="int"><default>0</default></key>
            
            NodeList graphElemList = graphDoc.getElementsByTagName("graph");
            if (graphElemList.getLength() == 0) 
                throw new IOException("Missing <graph> element");
            if (graphElemList.getLength() > 1) 
                LOGGER.warning(f + " has more than one <graph> element"
                               + "; returning only the first");
            Element graphElem = (Element)(graphElemList.item(0));
            
            String weightKeyId = null;
            String typeKeyId = null;
            
            WeightedDirectedMultigraph<String> g = 
                new WeightedDirectedMultigraph<String>();

            NodeList keyElemList = graphElem.getElementsByTagName("key");
            for (int i = 0; i < keyElemList.getLength(); ++i) {
                Element key = (Element)(keyElemList.item(i));
                if (key.getAttribute("for").equals("edge")) {
                    if (key.getAttribute("attr.name").equals("weight"))
                        weightKeyId = key.getAttribute("id");
                    else if (key.getAttribute("attr.name").equals("type"))
                        typeKeyId = key.getAttribute("id");
                }
            }          
            
            NodeList nodeElemList = graphElem.getElementsByTagName("node");
            for (int i = 0; i < nodeElemList.getLength(); ++i) {
                Element node = (Element)(nodeElemList.item(i));
                String id = node.getAttribute("id");
                g.add(vertexLabels.index(id));
                if ((i+1) % 1000 == 0)
                    verbose(LOGGER, "Added %d vertices", i);
            }
            verbose(LOGGER, "Found %d total vertices", g.order());

            NodeList edgeElemList = graphElem.getElementsByTagName("edge");
            for (int i = 0; i < edgeElemList.getLength(); ++i) {
                Element edge = (Element)(edgeElemList.item(i));
                String fromId = edge.getAttribute("source");
                String toId = edge.getAttribute("target");
                // Get its children which have the weight and type attributes
                
                String weightStr = null;
                String type = null;

                NodeList dataElemList = edge.getElementsByTagName("data");
                for (int j = 0; j < dataElemList.getLength(); ++j) {
                    Element data = (Element)(dataElemList.item(j));
                    if (data.getAttribute("key").equals(weightKeyId)) 
                        weightStr = data.getTextContent();
                    else if (data.getAttribute("key").equals(typeKeyId)) 
                        type = data.getTextContent();
                }

                if (weightStr == null)
                    throw new IOException("No weight specified for edge " +
                                          edge.getAttribute("id"));
                if (type == null)
                    throw new IOException("No type specified for edge " +
                                          edge.getAttribute("id"));

                int from = vertexLabels.find(fromId);
                if (from < 0)
                    throw new IOException("Unknown source node for edge " +
                                      edge.getAttribute("id") + ": " + fromId);
                int to = vertexLabels.find(toId);
                if (to < 0)
                    throw new IOException("Unknown target node for edge " +
                                      edge.getAttribute("id") + ": " + toId);
               
                double weight = 0;
                try {
                    weight = Double.parseDouble(weightStr);
                } catch (NumberFormatException nfe) {
                    throw new IOException("Invalid weight for edge " +
                                    edge.getAttribute("id") + ": " + weightStr);
                }

                WeightedDirectedTypedEdge<String> e = 
                    new SimpleWeightedDirectedTypedEdge<String>(
                        type, from, to, weight);
                g.add(e);
                if ((i+1) % 1000 == 0)
                    verbose(LOGGER, "Added %d edges", i);

            }

            verbose(LOGGER, "Loaded a directed, weighted multigraph with %d " +
                    "vertices and %d edges", g.order(), g.size());
            return g;
        } 
        catch (IOException ioe) {
            throw ioe; // rethrow
        }
        // Generic catch all for all the XML exception
        catch (Exception e) { 
            throw new IOException(e);
        }
    }

    
    public class GraphMLParser extends DefaultHandler {
        
        // State variables when parsing edges
        private int from;
        private int to;
        private String type;
        private double weight;
	
	private static final String NODE = "node";
	private static final String EDGE = "edge";
        private static final String DATA = "data";

        private final WeightedDirectedMultigraph<String> g;
        private final Indexer<String> vertexLabels;

        private String weightKeyId;
        private String typeKeyId;

        private String curDataKey = null;
        private String curData = null;

	GraphMLParser(Indexer<String> vertexLabels) {
            this.vertexLabels = vertexLabels;
            this.g = new WeightedDirectedMultigraph<String>();
	}

	public void startDocument() { }

	public void endDocument() throws SAXException { 
            verbose(LOGGER, "Loaded a directed, weighted multigraph with %d " +
                    "vertices and %d edges", g.order(), g.size());
        }

	public void startElement(String namespace, String localName, String qName,
                                 Attributes atts) throws SAXException {
            if (qName.equals("key")) {
                if (atts.getValue("for").equals(EDGE)) {
                    if (atts.getValue("attr.name").equals("weight"))
                        weightKeyId = atts.getValue("id");
                    else if (atts.getValue("attr.name").equals("type"))
                        typeKeyId = atts.getValue("id");
                }
            } 
            else if (qName.equals(NODE)) {
                String id = atts.getValue("id");
                g.add(vertexLabels.index(id));
                if (g.order() % 1000 == 0)
                    verbose(LOGGER, "Added %d vertices", g.order());
            }
            else if (qName.equals(EDGE)) {
                String fromId = atts.getValue("source");
                String toId = atts.getValue("target");
                
                from = vertexLabels.find(fromId);
                if (from < 0)
                    throw new SAXException("Unknown source node for edge " 
                                          + fromId);
                to = vertexLabels.find(toId);
                if (to < 0)
                    throw new SAXException("Unknown target node for edge " 
                                          + toId);
            }
            else if (qName.equals(DATA)) {
                curDataKey = atts.getValue("key");
            }
	}

	public void characters(char[] ch, int start, int length) {
            curData = new String(ch, start, length);
        }

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
            if (qName.equals(EDGE)) {
                if (from == to)
                    return;
                WeightedDirectedTypedEdge<String> e = 
                    new SimpleWeightedDirectedTypedEdge<String>(
                        type, from, to, weight);
                g.add(e);
                if (g.size() % 1000 == 0)
                    verbose(LOGGER, "Added %d edges", g.size());
            }

            else if (qName.equals(DATA)) {
                if (curDataKey.equals(weightKeyId)) {                   
                    try {
                        weight = Double.parseDouble(curData);
                    } catch (NumberFormatException nfe) {
                        throw new SAXException("Invalid weight: " + curData);
                    }
                }
                else if (curDataKey.equals(typeKeyId)) {
                    type = curData;
                }                              
            }
        }
    }    
}