/*
 * Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at
 * the Lawrence Livermore National Laboratory. Written by Keith Stevens,
 * kstevens@cs.ucla.edu OCEC-10-073 All rights reserved. 
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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixRank;
import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * An implementation of Hub Clustering, which first ranks each node in a
 * adjacency matrix using a {@link MatrixRank} function and then creates a
 * minimum spanning tree rooted from the top K ranked nodes in the matrix.
 * This approach is based on 
 *
 * <ul>
 *   <li>
 *    <li style="font-family:Garamond, Georgia, serif">Jean Veronis.  HyperLex:
 *    Lexical Cartography in Information Retrieval.  <i>Computer Speech and
 *    Language, Special Issue on Word Sense Dismabiguation.</i>Available <a
 *    href="http://www.sciencedirect.com/science/article/pii/S0885230804000142">here</a>.
 *    </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class HubClustering implements Clustering {

    /**
     * The {@link MatrixRank} function for ranking each vertex in a graph.
     */
    private final MatrixRank ranker;

    /**
     * Creates a new {@link HubClustering} instances using the given {@link
     * MatrixRank} method.
     */
    public HubClustering(MatrixRank ranker) {
        this.ranker = ranker;
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adj, Properties props) {
        return cluster(adj, (int) (adj.rows() * .10), props);
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adj, int numClusters, Properties props) {
        // Rank each node in the matrix and select the top ranked nodes.
        Set<Integer> hubs = selectHubs(ranker.rank(adj), numClusters);

        // Create a link an an initial minimum spanning tree.  Hub nodes have an
        // ifinite weight so that they get selected first and are at the top of
        // the tree, other nodes have a negaive weight and will later be
        // connected to the nearest hub node.
        Map<Integer, NodeLink> links = new HashMap<Integer, NodeLink>();
        int hubId = 0;
        for (int n = 0; n < adj.rows(); ++n)
            if (hubs.contains(n))
                links.put(n, new NodeLink(n, Double.MAX_VALUE, hubId++));
            else
                links.put(n, new NodeLink(-1, -1.0, -1));

        // Create a set of clusters focused on each hub node.
        List<Set<Integer>> clusters = new ArrayList<Set<Integer>>(numClusters);
        for (int c = 0; c < numClusters; ++c)
            clusters.add(new HashSet<Integer>());

        // While we haven't connected every node to some hub node, find the best
        // link.
        while (!links.isEmpty()) {
            // Find the best link that hasn't been used yet.
            NodeLink bestLink = null;
            int bestDest = -1;
            for (Map.Entry<Integer, NodeLink> e : links.entrySet()) {
                NodeLink link = e.getValue();
                if (bestLink == null || link.weight >= bestLink.weight) {
                    bestLink = link;
                    bestDest = e.getKey();
                }
            }

            links.remove(bestDest);

            // Update the weight from every node not assigned to a cluster based
            // on the newest node assigned to a cluster.
            for (Map.Entry<Integer, NodeLink> e : links.entrySet()) {
                int node = e.getKey();
                NodeLink link = e.getValue();
                double newWeight = adj.get(node, bestDest);
                if (newWeight >= link.weight) {
                    link.fromNode = bestDest;
                    link.weight = newWeight;
                    link.hub = bestLink.hub;
                }
            }

            // Connect the node not already assigned to a cluster to the cluster
            // corresponding to it's nearest hub node.
            clusters.get(bestLink.hub).add(bestDest);
        }

        // Transform the clusters to an Assignments object and return.
        Assignments assignments = new Assignments(clusters.size(), adj.rows());
        int c = 0;
        for (Set<Integer> cluster : clusters) {
            for (Integer point : cluster)
                assignments.set(point, c);
            c++;
        }
        return assignments;
    }

    /**
     * Returns the highest ranked {@code numHubs} in a matrix as desided by
     * {@code ranks}.
     */
    public static Set<Integer> selectHubs(double[] ranks, int numHubs) {
        MultiMap<Double, Integer> hubs =
            new BoundedSortedMultiMap<Double, Integer>(numHubs);
        for (int node = 0; node < ranks.length; ++node)
            hubs.put(ranks[node], node);
        return new HashSet<Integer>(hubs.values());
    }

    /**
     * A simple struct recording a link between two nodes and tracks the nearest
     * hub node.
     */
    private static class NodeLink {
        int fromNode;
        double weight;
        int hub;
        public NodeLink(int fromNode, double weight, int hub) {
            this.fromNode = fromNode;
            this.weight = weight;
            this.hub = hub;
        }
    }
}
