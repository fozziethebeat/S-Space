/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.clustering.Assignment;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.GraphIO;
import edu.ucla.sspace.graph.KPartiteLinkClustering;
import edu.ucla.sspace.graph.LinkClustering;

import edu.ucla.sspace.mains.OptionDescriptions;

import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.ArrayMap;
import edu.ucla.sspace.util.ColorGenerator;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.Indexer;
import edu.ucla.sspace.util.LineReader;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.ObjectIndexer;
import edu.ucla.sspace.util.TreeMultiMap;

import java.awt.Color;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;


import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;


// Logger helper methods
import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * A utility class for running {@link LinkClustering} from the command line.
 */
public class LinkClusteringTool {

    private static final Logger LOGGER = 
        Logger.getLogger(LinkClusteringTool.class.getName());

    public static void main(String[] args) {
        ArgOptions opts = new ArgOptions();
        
        opts.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");
//         opts.addOption('w', "weighted", "Uses a weighted edge simiarity",
//                           false, null, "Program Options");
        opts.addOption('k', "kpartite", "Uses the k-partite link clustering " +
                       "with the provided file that maps a vertex to " +
                       "its partition (note: not its community)",
                       true, "FILE", "Program Options");
        
         opts.addOption('o', "offCore", "Recomputes data structures and " +
                           "values as needed, which slows processing but " +
                           "allows much larger graphs to be clustered",
                           false, null, "Program Options");
        opts.addOption('d', "printDensities", "Prints all the cluster " +
                       "densities to the specified file", true, "FILE",
                       "Program Options");
        opts.addOption('a', "saveAllSolutions", "Saves the communities for all"+
                       "possible partitionings", true, "FILE_PREFIX",
                       "Program Options");
        opts.addOption('n', "saveNthSolutions", "Saves only every nth solution"+
                       " when -a is used", true, "INT", "Program Options");
        opts.addOption('v', "verbose", "Turns on verbose output",
                          false, null, "Program Options");
        opts.addOption('V', "verbVerbose", "Turns on very verbose output",
                          false, null, "Program Options");
        opts.addOption('z', "visualizeSolution", "Writes a .gml file with"+
                       "each edge labeled with its cluster and uniquely " +
                       "colored ", true, "FILE", "Program Options");


        opts.parseOptions(args);

        if (opts.numPositionalArgs() < 2 || opts.hasOption("help")) {
            usage(opts);
            return;
        }

        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (opts.hasOption('v')) 
            LoggerUtil.setLevel(Level.FINE);
        if (opts.hasOption('V')) 
            LoggerUtil.setLevel(Level.FINER);


        LOGGER.info("Loading graph file");
        Indexer<String> vertexLabels = new ObjectIndexer<String>();
        File f = new File(opts.getPositionalArg(0));
        Graph<Edge> graph = null;
        try {
            graph = GraphIO.readUndirected(f, vertexLabels);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        if (opts.hasOption('o'))
            System.getProperties().setProperty(
                LinkClustering.SMALL_EDGE_INDEX_PROPERTY, "true");

        Assignment[] assignments = null;
        LinkClustering lc = null;

        if (opts.hasOption('k')) {
            // Load the partition mapping for the vertices.  We'll map each
            // partition to a specific index and the wrap that as a Map
            File partitionFile = new File(opts.getStringOption('k'));
            Integer[] vertexToPartition = new Integer[graph.order()];
            Indexer<String> partitionIndices = new ObjectIndexer<String>();
            for (String line : new LineReader(partitionFile)) {
                String[] arr = line.split("\\s+");
                if (arr.length < 2) 
                    throw new IllegalArgumentException(
                        "Missing data on line: " + line);
                String vertex = arr[0];
                String partition = arr[1];
                int vIndex = vertexLabels.find(vertex);
                // Skip unknown vertices, rather than throw an exception, as it
                // makes life easier to use a giant vertex mapping and then test
                // on subgraphs of that without generating new mappings for each
                if (vIndex < 0)
                    continue;
                int pIndex = partitionIndices.index(partition);
                vertexToPartition[vIndex] = pIndex;
            }

            // Wrap the array as a Map for faster access when computing the
            // density
            Map<Integer,Integer> partitions = 
                new ArrayMap<Integer>(vertexToPartition);
            // Check that all the vertices were mapped
            if (partitions.size() != graph.order()) {
                throw new IllegalStateException(
                    "Not all vertices are mapped to partitions: " +
                    partitions.size() + " < " + graph.order());
            }
            
            // Create a new special-case link clustering.  Note that we need the
            // reference to the subtype in order to call the special cluster()
            // method that takes in the partitions as a parameter
            KPartiteLinkClustering kplc = new KPartiteLinkClustering();
            lc = kplc;
            assignments = kplc.cluster(graph, System.getProperties(), 
                                       partitions);
        }
        else {
            lc = new LinkClustering();
            assignments = lc.cluster(graph, System.getProperties());
        }
        
        info(LOGGER, "writing partitioning with highest density");
        writeCommunities(assignments, vertexLabels.mapping(), 
                         opts.getPositionalArg(1));

        if (opts.hasOption('a')) {
            String outputSolPrefix = opts.getStringOption('a');
            int stepSize = (opts.hasOption('n'))
                ? opts.getIntOption('n')
                : 1;
            int numSolutions = lc.numberOfSolutions();
            for (int i = 0; i < numSolutions; i+= stepSize) {
                Assignment[] solution = lc.getSolution(i);
                writeCommunities(solution, vertexLabels.mapping(),
                                 outputSolPrefix + "_" + i);
            }
        }

        if (opts.hasOption('d')) {
            String densitiesFile = opts.getStringOption('d');
            try {
                PrintWriter pw = new PrintWriter(densitiesFile);
                int numSolutions = lc.numberOfSolutions();
                for (int i = 0; i < numSolutions; i++) {
                    pw.println(i + " " + lc.getSolutionDensity(i));
                }
                pw.close();
            }
            catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
        
        if (opts.hasOption('z')) {
            String labeledGraphFile = opts.getStringOption('z');
            try {
                writeGexfVisualization(new File(labeledGraphFile), lc,
                                       vertexLabels);
//                 PrintWriter pw = new PrintWriter(labeledGraphFile);
//                 pw.println("graph [");
//                 // Write the indices
//                 for (Map.Entry<String,Integer> v : vertexLabels) {
//                     pw.println("  node [");
//                     pw.println("    id " + v.getValue());
//                     pw.println("    label \"" + v.getKey() + "\"");
//                     pw.println("    graphics [");
//                     pw.println("      fill \"#666666\"");
//                     pw.println("      type \"ellipse\"");
//                     pw.println("      w 40.0");
//                     pw.println("      h 40.0");
//                     pw.println("      outline \"#000000\"");
//                     pw.println("      outline_width 0.0");
//                     pw.println("    ]");
//                     pw.println("  ]");
//                 }
//                 MultiMap<Integer,Edge> edgeClusters = 
//                     lc.getEdgeClusters(lc.getReturnedSolutionNumber());
//                 Iterator<Color> colorGenerator = new ColorGenerator();
//                 for (Integer clusterId : edgeClusters.keySet()) {
//                     Color c = colorGenerator.next();
//                     String rgb = Integer.toHexString(c.getRGB());
//                     rgb = rgb.substring(2, rgb.length());
//                     for (Edge e : edgeClusters.get(clusterId)) {
//                         pw.println("  edge [");
//                         pw.println("    source " + e.from());
//                         pw.println("    target " + e.to());
//                         pw.println("    label \"cluster " + clusterId + "\"");
//                         pw.println("    graphics [");
//                         pw.println("      fill \"#" + rgb + "\"");
//                         pw.println("      type \"line\"");
//                         pw.println("      Line [ ]");
//                         pw.println("      width 12");
//                         pw.println("      source_arrow 0");
//                         pw.println("      target_arrow 0");
//                         pw.println("      outline \"#000000\"");
//                         pw.println("      outline_width 0.0");
//                         pw.println("    ]");
//                         pw.println("  ]");
//                     }
//                 }
//                 pw.println(']');
//                 pw.close();
            } catch (Exception ioe) {
                throw new Error(ioe);
            }
        }
    }


    private static void writeGexfVisualization(File outputFile,
                                               LinkClustering lc,
                                               Indexer<String> vertexLabels) 
            throws Exception {
        
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
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
        graph.setAttribute("mode","static");
        root.appendChild(graph);

        Element nodes = doc.createElement("nodes");
        graph.appendChild(nodes);
        Element edges = doc.createElement("edges");
        graph.appendChild(edges);

        for (Map.Entry<String,Integer> v : vertexLabels) {
            Element node = doc.createElement("node");
            node.setAttribute("id", "" + v.getValue());
            node.setAttribute("label", v.getKey());
            nodes.appendChild(node);            
        }

        MultiMap<Integer,Edge> edgeClusters = 
            lc.getEdgeClusters(lc.getReturnedSolutionNumber());
        Iterator<Color> colorGenerator = new ColorGenerator();

        int edgeId = 0;
        for (Integer clusterId : edgeClusters.keySet()) {
            Color c = colorGenerator.next();

            for (Edge e : edgeClusters.get(clusterId)) {
                Element edge = doc.createElement("edge");
                edges.appendChild(edge);
                edge.setAttribute("id", "" + (edgeId++));
                edge.setAttribute("source", "" + e.from());
                edge.setAttribute("target", "" + e.to());
                edge.setAttribute("label", clusterId.toString());
                Element color = doc.createElement("viz:color");
                color.setAttribute("r", "" + c.getRed());
                color.setAttribute("g", "" + c.getGreen());
                color.setAttribute("b", "" + c.getBlue());
                edge.appendChild(color);
            }
        }

        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans = transfac.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");
        
        //create string from xml tree
        StreamResult result = new StreamResult(outputFile);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
    }

    private static void writeCommunities(Assignment[] assignments, 
                                         Map<Integer,String> rowToKey,
                                         String fileName) {
        // Write the result
        MultiMap<Integer,Integer> clusterToRows = 
            new TreeMultiMap<Integer,Integer>();

        // Calculate the cluster mapping
        for (int i = 0; i < assignments.length; ++i) {
            for (int clusterId : assignments[i].assignments()) {
                clusterToRows.put(clusterId, i);
            }
        }                   
        
        try {
            PrintWriter output = new PrintWriter(fileName);
            for (Integer clusterId : clusterToRows.keySet()) {
                Set<Integer> rows = clusterToRows.get(clusterId);
                StringBuilder sb = new StringBuilder();
                sb.append(clusterId).append(':');
                for (Integer row : rows) {
                    String t = rowToKey.get(row);
                    sb.append(' ').append(t);
                }
                output.println(sb);
            }
            
            output.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Prints the options and supported commands used by this program.
     *
     * @param options the options supported by the system
     */
    private static void usage(ArgOptions options) {
        System.out.println(
            "Link Clustering 1.0, " +
            "based on the community detection method of\n\n" +
            "\tYong-Yeol Ahn, James P. Bagrow, and Sune Lehmann. 2010.\n" +
            "\tLink communities reveal multiscale complexity in networks.\n" +
            "\tNature, (466):761-764, August.\n\n" +
            "usage: java -jar lc.jar [options] edge_file communities.txt \n\n" 
            + options.prettyPrint() +
            "\nThe edge file format is:\n" +
            "   vertex1 vertex2 [weight] [partition_for_v1 partition_for_v2]\n" +
            "where vertices may be named using any contiguous sequence of " +
            "characters or\n" +
            "numbers.  Weights may be any non-zero double value.  Partitions " +
            "can be\n" +
            "named using any contiguous sequence of characters or numbers.  " +
            "Lines beginning\n" +
            "with '#' are treated as comments and skipped.\n\n"+
            OptionDescriptions.HELP_DESCRIPTION);
    }

}