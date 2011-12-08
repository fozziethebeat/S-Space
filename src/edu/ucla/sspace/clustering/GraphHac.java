package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.matrix.Matrix;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class GraphHac extends AbstractGraphClustering {

    public Assignments cluster(Matrix adj, int k, Properties props) {
        int[] assignments = HierarchicalAgglomerativeClustering.clusterSimilarityMatrix(
                adj, -1, ClusterLinkage.MEAN_LINKAGE, k);
        return new Assignments(k, assignments, null);
    }

    public Assignments cluster(Matrix adj, Properties props) {
        return null;
    }
}
