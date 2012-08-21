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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
        opts.addOption('W', "minWeight", "Loads a weighted graph as " +
                       "unweighted, keeping only those edges with weight "+
                       "at least the specified value",
                       true, "Double", "Input Options");
        opts.addOption('v', "verbose", "Turns on verbose output",
                          false, null, "Program Options");
        opts.addOption('V', "verbVerbose", "Turns on very verbose output",
                          false, null, "Program Options");
        
        ////////
        //
        // OPTIONS TO BE ADDED AT SOME POINT IN THE FUTURE...
        //
        ///////

//         opts.addOption('w', "weighted", "Uses a weighted edge simiarity",
//                        false, null, "Program Options");
//         opts.addOption('k', "kpartite", "Uses the k-partite link clustering " +
//                        "with the provided file that maps a vertex to " +
//                        "its partition (note: not its community)",
//                        true, "FILE", "Program Options");

//         opts.addOption('d', "printDensities", "Prints all the cluster " +
//                        "densities to the specified file", true, "FILE",
//                        "Program Options");
//         opts.addOption('a', "saveAllSolutions", "Saves the communities for all"+
//                        "possible partitionings", true, "FILE_PREFIX",
//                        "Program Options");
//         opts.addOption('n', "saveNthSolutions", "Saves only every nth solution"+
//                        " when -a is used", true, "INT", "Program Options");

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


        try {
            
            LOGGER.info("Loading graph file");
            Indexer<String> vertexLabels = new ObjectIndexer<String>();
            File f = new File(opts.getPositionalArg(0));
            Graph<Edge> graph = null;
            if (opts.hasOption('W'))
                graph = GraphIO.readUndirectedFromWeighted(
                    f, vertexLabels, opts.getDoubleOption('W'));
            else
                GraphIO.readUndirected(f, vertexLabels);

            LinkClustering lc = new LinkClustering();
            MultiMap<Integer,Integer> clusterToVertices = 
                lc.cluster(graph, System.getProperties());

            PrintWriter pw = new PrintWriter(
                new BufferedOutputStream(new FileOutputStream(
                                         opts.getPositionalArg(1))));

            for (Map.Entry<Integer,Set<Integer>> e : 
                     clusterToVertices.asMap().entrySet()) {
                Integer clusterId = e.getKey();
                Set<Integer> vertices = e.getValue();

                verbose(LOGGER, "Cluster %d had vertices: %s%n",
                        clusterId, vertices.size());
                
                Iterator<Integer> iter = vertices.iterator();
                StringBuilder sb = new StringBuilder();
                while (iter.hasNext()) {
                    int v = iter.next();
                    sb.append(vertexLabels.lookup(v));
                    if (iter.hasNext())
                        sb.append(' ');
                    else
                        pw.println(sb);
                }
            }
            pw.close();

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
            "   vertex1 vertex2 [weight]\n" +
            "where vertices may be named using any contiguous sequence of " +
            "characters or\n" +
            "numbers.  Weights may be any non-zero double value.  \n" +
            "Lines beginning\n" +
            "with '#' are treated as comments and skipped.\n\n"+
            OptionDescriptions.HELP_DESCRIPTION);
    }

}