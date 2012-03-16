package edu.ucla.sspace.clustering;

import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering;
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering.ClusterLink;
import edu.ucla.sspace.matrix.Matrix;

import java.util.Properties;


/**
 * A simple wrapper around {@code NeighborChainAgglomerativeClustering} that
 * treats the input matrix as an adjacency matrix connecting data points within
 * a graph.  This method defaults to using the {@link
 * NeighborChainAgglomerativeClustering#MEAN_LINK} {@link
 * NeighborChainAgglomerativeClustering#ClusterLink} method.
 *
 * </p>
 *
 * This method will run in O(N^2) time.
 *
 * @author Keith Stevens
 */
public class GraphHac implements Clustering {

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix adj, int k, Properties props) {
        return NeighborChainAgglomerativeClustering.clusterAdjacencyMatrix(
                adj, ClusterLink.MEAN_LINK, k);
    }

    /**
     * Unsupported operation.
     *
     * @throws new UnsupportedOperationException
     */
    public Assignments cluster(Matrix adj, Properties props) {
        throw new UnsupportedOperationException(
                "Cannot cluster without setting the number of clusters");
    }
}
