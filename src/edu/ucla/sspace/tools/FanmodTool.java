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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.graph.DirectedEdge;
import edu.ucla.sspace.graph.DirectedGraph;
import edu.ucla.sspace.graph.DirectedMultigraph;
import edu.ucla.sspace.graph.DirectedTypedEdge;
import edu.ucla.sspace.graph.Edge;
import edu.ucla.sspace.graph.Fanmod;
import edu.ucla.sspace.graph.Graph;
import edu.ucla.sspace.graph.GraphIO;
import edu.ucla.sspace.graph.Multigraph;
import edu.ucla.sspace.graph.TypedEdge;
import edu.ucla.sspace.graph.UndirectedMultigraph;

import edu.ucla.sspace.graph.io.DotIO;

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
import edu.ucla.sspace.util.SerializableUtil;
import edu.ucla.sspace.util.TreeMultiMap;

import java.awt.Color;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;


// Logger helper methods
import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * A utility class for running {@link Fanmod} from the command line.
 */
public class FanmodTool {

    private static final Logger LOGGER = 
        Logger.getLogger(FanmodTool.class.getName());

    public static void main(String[] args) {
        ArgOptions opts = new ArgOptions();
        
        opts.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");
        opts.addOption('v', "verbose", "Turns on verbose output",
                          false, null, "Program Options");
        opts.addOption('V', "verbVerbose", "Turns on very verbose output",
                          false, null, "Program Options");

        opts.addOption('r', "randomGraphs", "The number of random graphs" +
                       " to use for the null model (default: 1000)",
                       true, "INT", "Algorithm Options");
        opts.addOption('z', "motifSize", "The number of vertices in the" +
                       " identified motifs (default: 3)",
                       true, "INT", "Algorithm Options");
        opts.addOption('s', "useSimpleMotifs", "If searching for motifs in a " +
                       "multigraph, counts only simple graphs as motifs",
                       false, null, "Algorithm Options");

        opts.addOption('Z', "minZScore", "The minimum Z-Score for any motif" +
                       " in the original network to be used for computing " +
                       "modularity (default: 1)",
                       true, "DOUBLE", "Algorithm Options");
        opts.addOption('O', "minOccurrences", "The minimum number of occurrences"
                       + " for any motif" +
                       " in the original network to be used for computing " +
                       "modularity (default: 1)",
                       true, "INT", "Algorithm Options");


//         opts.addOption('w', "weighted", "Uses a weighted edge simiarity",
//                           false, null, "Input Options");
        opts.addOption('d', "loadAsDirectedGraph", "Loads the input graph as " +
                       "a directed graph",
                       false, null, "Input Options");
        opts.addOption('m', "loadAsMultigraph", "Loads the input graph as " +
                       "a multigraph",
                       false, null, "Input Options");

        opts.addOption('o', "outputFormat", "The type of format to use " +
                       "when writing the graphs (default: serialized)",
                       true, "FORMAT", "Output Options");        
        opts.addOption('H', "makeHtml", "Generates an HTML rendering" +
                       "of the significant motifs",
                       true, "DIR", "Output Options");

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

        int motifSize = (opts.hasOption('z')) ? opts.getIntOption('z') : 3;
        int numRandomGraphs = (opts.hasOption('r')) 
            ? opts.getIntOption('r') : 1000;

        double minZScore = opts.hasOption('Z')
            ? opts.getDoubleOption('Z')
            : 1d;
        int minOccurrences = opts.hasOption('O')
            ? opts.getIntOption('O')
            : 1;
        Fanmod.MotifFilter filter = 
            new Fanmod.FrequencyAndZScoreFilter(minOccurrences, minZScore);

        info(LOGGER, "retaining motifs occurring at least %d times with a " +
             "z-score at or above %f", minOccurrences, minZScore);
       
        boolean isMultigraph = opts.hasOption('m');
        boolean isDirected = opts.hasOption('d');
        Fanmod fanmod = new Fanmod();
        
        try {       
            if (isMultigraph && isDirected) {
                DirectedMultigraph<String> dm = 
                    GraphIO.readDirectedMultigraph(f, vertexLabels);
                boolean findSimpleMotifs = opts.hasOption('s');
                Map<Multigraph<String,DirectedTypedEdge<String>>,Fanmod.Result> 
                    motifToZScore = fanmod.findMotifs( 
                      dm, findSimpleMotifs, motifSize, numRandomGraphs, filter);
                info(LOGGER, "found %d motifs with z-score above %f%n", 
                     motifToZScore.size(), minZScore);
                if (opts.hasOption('H')) {
                    File baseDir = new File(opts.getStringOption('H'));
                    // Check that we can create output in that directory
                    if (!baseDir.exists())
                        baseDir.mkdir();
                    DotIO dio = new DotIO();
                    
                    // Generate a consistent set of edge colors to user across
                    // all the motif visualizations
                    Map<String,Color> edgeColors = new HashMap<String,Color>();
                    ColorGenerator cg = new ColorGenerator();
                    for (String type : dm.edgeTypes())
                        edgeColors.put(type, cg.next());

                    PrintWriter pw = new PrintWriter(new File(baseDir, "index.html"));
                    PrintWriter imgScript = new PrintWriter(new File(baseDir, "img-script.sh"));
                    imgScript.println("#!/bin/bash");
                    pw.println("<html>");
                    pw.println("<head><script src=\"http://www.kryogenix.org/code/browser/sorttable/sorttable.js\"></script></head>");
                    // pw.println("<head><script src=\"sorttable.js\"></script></head>");
                    pw.println("<body><table border=\"2\" class=\"sortable\">");
                    pw.println("  <tr>" + 
                               "<td><h1><u>Motif</u></h1></td>" +
                               "<td><h1><u>Count</u></h1></td>" +
                               "<td><h1><u>Z-Score</u></h1></td>" +
                               "<td><h1><u>Mean Count in Random Graphs</u></h1></td>" +
                               "<td><h1><u>StdDev in Random Graphs</u></h1></td>" +
                               "</tr>");
                    int graphNum = 0;
                    for (Map.Entry<Multigraph<String,DirectedTypedEdge<String>>,Fanmod.Result> e :
                             motifToZScore.entrySet()) {
                        File dotFile = new File(baseDir, "graph-" + (graphNum++) + ".dot");
                        dio.writeDirectedMultigraph(e.getKey(), dotFile, edgeColors);
                        String imgFile = dotFile.getName();
                        imgFile = imgFile.substring(0, imgFile.length() - 3) + "gif";
                        imgScript.printf("dot -Tgif %s -o %s%n", dotFile.getName(), imgFile);
                        int count = e.getValue().count;
                        double zScore = e.getValue().statistic;
                        double mean = e.getValue().meanCountInNullModel;
                        double stddev = e.getValue().stddevInNullModel;
                        pw.printf("  <tr><td><img src=\"%s\"></td><td>%d</td><td>%f</td><td>%f</td><td>%f</td></tr>%n",
                                  imgFile, count, zScore, mean, stddev);
                    }
                    pw.println("</table></body></html>");
                    imgScript.close();
                    pw.close();
                }
                
                info(LOGGER, "writing final motifs to %s", 
                     opts.getPositionalArg(1));
                // Write the results to file
                File output = new File(opts.getPositionalArg(1));
                // Copy the motifs to a new HashSet to avoid writing the result
                // as a KeySet, which includes the fanmod result values.
                SerializableUtil.save(
                    new HashSet<Multigraph<String,
                        DirectedTypedEdge<String>>>(motifToZScore.keySet()), output);
            }

            else if (isMultigraph) {
                boolean findSimpleMotifs = opts.hasOption('s');
                UndirectedMultigraph<String> um = 
                    GraphIO.readUndirectedMultigraph(f, vertexLabels);
                Map<Multigraph<String,TypedEdge<String>>,Fanmod.Result> 
                    motifToZScore = fanmod.findMotifs( 
                      um, findSimpleMotifs, motifSize, numRandomGraphs, filter);
                info(LOGGER, "found %d motifs with z-score above %f%n", 
                     motifToZScore.size(), minZScore);
                if (opts.hasOption('H')) {
                    File baseDir = new File(opts.getStringOption('H'));
                    // Check that we can create output in that directory
                    if (!baseDir.exists())
                        baseDir.mkdir();
                    DotIO dio = new DotIO();
                    // Generate a consistent set of edge colors to user across
                    // all the motif visualizations
                    Map<String,Color> edgeColors = new HashMap<String,Color>();
                    ColorGenerator cg = new ColorGenerator();
                    for (String type : um.edgeTypes())
                        edgeColors.put(type, cg.next());

                    PrintWriter pw = new PrintWriter(new File(baseDir, "index.html"));
                    PrintWriter imgScript = new PrintWriter(new File(baseDir, "img-script.sh"));
                    imgScript.println("#!/bin/bash");
                    pw.println("<html>");
                    pw.println("<head><script src=\"http://www.kryogenix.org/code/browser/sorttable/sorttable.js\"></script></head>");
                    // pw.println("<head><script src=\"sorttable.js\"></script></head>");
                    pw.println("<body><table border=\"2\" class=\"sortable\">");
                    pw.println("  <tr>" + 
                               "<td><h1><u>Motif</u></h1></td>" +
                               "<td><h1><u>Count</u></h1></td>" +
                               "<td><h1><u>Z-Score</u></h1></td>" +
                               "<td><h1><u>Mean Count in Random Graphs</u></h1></td>" +
                               "<td><h1><u>StdDev in Random Graphs</u></h1></td>" +
                               "</tr>");
                    int graphNum = 0;
                    for (Map.Entry<Multigraph<String,TypedEdge<String>>,Fanmod.Result> e :
                             motifToZScore.entrySet()) {
                        File dotFile = new File(baseDir, "graph-" + (graphNum++) + ".dot");
                        dio.writeUndirectedMultigraph(e.getKey(), dotFile, edgeColors);
                        String imgFile = dotFile.getName();
                        imgFile = imgFile.substring(0, imgFile.length() - 3) + "gif";
                        imgScript.printf("dot -Tgif %s -o %s%n", dotFile.getName(), imgFile);
                        int count = e.getValue().count;
                        double zScore = e.getValue().statistic;
                        double mean = e.getValue().meanCountInNullModel;
                        double stddev = e.getValue().stddevInNullModel;
                        pw.printf("  <tr><td><img src=\"%s\"></td><td>%d</td><td>%f</td><td>%f</td><td>%f</td></tr>%n",
                                  imgFile, count, zScore, mean, stddev);
                    }
                    pw.println("</table></body></html>");
                    imgScript.close();
                    pw.close();

                    info(LOGGER, "writing final motifs to %s", 
                         opts.getPositionalArg(1));
                    // Write the results to file
                    File output = new File(opts.getPositionalArg(1));
                    // Copy the motifs to a new HashSet to avoid writing the result
                    // as a KeySet, which includes the fanmod result values.
                    SerializableUtil.save(
                        new HashSet<Multigraph<String,TypedEdge<String>>>(
                            motifToZScore.keySet()), output);

                }
            }

            else if (isDirected) {
                DirectedGraph<DirectedEdge> dg = 
                    GraphIO.readDirected(f, vertexLabels);
                throw new Error();
            }
            else {
                Graph<Edge> g = GraphIO.readUndirected(f, vertexLabels);
                throw new Error();
            }
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
            "Fanmod 1.0, " +
            "usage: java -jar fanmod.jar [options] input.graph output.serialized \n\n" 
            + options.prettyPrint() +
            "\nThe edge file format is:\n" +
            "   vertex1 vertex2 [edge_label]\n" +
            OptionDescriptions.HELP_DESCRIPTION);
    }

}