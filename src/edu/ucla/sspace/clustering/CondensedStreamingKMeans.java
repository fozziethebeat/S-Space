package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class CondensedStreamingKMeans implements Clustering {

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        OnlineClustering<DoubleVector> results = computeStreamingCluster(
                matrix);
        int[] assignments = HierarchicalAgglomerativeClustering.clusterRows(
                Matrices.asMatrix(collectCentroids(results)), 
                .15, ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);

        List<Cluster<DoubleVector>> newClusters = mergeClusters(
                assignments, results.getClusters());
        return computeAssignments(newClusters, matrix.rows());
    }
    
    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, 
                               int numClusters,
                               Properties props) {
        OnlineClustering<DoubleVector> results = computeStreamingCluster(
                matrix);
        int[] assignments = HierarchicalAgglomerativeClustering.partitionRows(
                Matrices.asMatrix(collectCentroids(results)), 
                numClusters, ClusterLinkage.MEAN_LINKAGE, SimType.COSINE);

        List<Cluster<DoubleVector>> newClusters = mergeClusters(
                assignments, results.getClusters());
        return computeAssignments(newClusters, matrix.rows());
    }

    @SuppressWarnings("unchecked")
    public static <T extends DoubleVector> OnlineClustering<T> computeStreamingCluster(
            Matrix matrix) {
        StreamingKMeans<T> generator = new StreamingKMeans<T>();

        OnlineClustering<T> onlineClustering = generator.generate();
        for (int r = 0; r < matrix.rows(); ++r)
            onlineClustering.addVector((T) matrix.getRowVector(r));

        return onlineClustering;
    }

    public static <T extends DoubleVector> List<T> collectCentroids(
            OnlineClustering<T> onlineClustering) {
        List<T> centroids = new ArrayList<T>();
        for (Cluster<T> cluster : onlineClustering.getClusters())
            centroids.add(cluster.centroid());
        return centroids;
    }

    public static <T extends DoubleVector> List<Cluster<T>> mergeClusters(
            int[] assignments, 
            List<Cluster<T>> clusters) {
        // Combine clusters determined to be merged by HAC.
        List<Cluster<T>> newClusters = new ArrayList<Cluster<T>>();

        // When a new assignment index is encountered, create an empty
        // cluster.  The first cluster assigned to that index becomes the
        // initial value.  Later clusters assigned to that index are merged
        // into the primary cluster.
        for (int i = 0; i < assignments.length; ++i) {
            int assignment = assignments[i];
            while (assignment >= newClusters.size())
                newClusters.add(null);

            Cluster<T> cluster = newClusters.get(assignment);
            if (cluster == null)
                newClusters.set(assignment, clusters.get(i));
            else
                cluster.merge(clusters.get(i));
        }
        return newClusters;
    }

    public static <T extends DoubleVector> Assignments computeAssignments(
            List<Cluster<T>> clusters,
            int numDataPoints) {
        Assignments assignments = new Assignments(clusters.size(), numDataPoints);
        int clusterId = 0;
        for (Cluster<T> cluster : clusters) {
            BitSet ids = cluster.dataPointIds();
            for (int id = ids.nextSetBit(0); id >= 0; id=ids.nextSetBit(id+1))
                assignments.set(id, clusterId);
            clusterId++;
        }
        return assignments;
    }
}
