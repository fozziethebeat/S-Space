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
 * @author Keith Stevens
 */
public class HubClustering implements Clustering {

    private final MatrixRank ranker;

    public HubClustering(MatrixRank ranker) {
        this.ranker = ranker;
    }

    public Assignments cluster(Matrix adj, Properties props) {
        return cluster(adj, (int) (adj.rows() * .10), props);
    }

    public Assignments cluster(Matrix adj, int numClusters, Properties props) {
        Set<Integer> hubs = selectHubs(ranker.rank(adj), numClusters);
        Map<Integer, NodeLink> links = new HashMap<Integer, NodeLink>();
        int hubId = 0;
        for (int n = 0; n < adj.rows(); ++n)
            if (hubs.contains(n))
                links.put(n, new NodeLink(n, Double.MAX_VALUE, hubId++));
            else
                links.put(n, new NodeLink(-1, -1.0, -1));

        List<Set<Integer>> clusters = new ArrayList<Set<Integer>>(numClusters);
        for (int c = 0; c < numClusters; ++c)
            clusters.add(new HashSet<Integer>());

        while (!links.isEmpty()) {
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

            clusters.get(bestLink.hub).add(bestDest);
        }

        Assignments assignments = new Assignments(clusters.size(), adj.rows());
        int c = 0;
        for (Set<Integer> cluster : clusters) {
            for (Integer point : cluster)
                assignments.set(point, c);
            c++;
        }
        return assignments;
    }

    public static Set<Integer> selectHubs(double[] ranks, int numHubs) {
        MultiMap<Double, Integer> hubs =
            new BoundedSortedMultiMap<Double, Integer>(numHubs);
        for (int node = 0; node < ranks.length; ++node)
            hubs.put(ranks[node], node);
        return new HashSet<Integer>(hubs.values());
    }

    public class NodeLink {
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
