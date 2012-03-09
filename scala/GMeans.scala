import edu.ucla.sspace.clustering.criterion.CriterionFunction
import edu.ucla.sspace.clustering.seeding.KMeansSeed
import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.clustering.DirectClustering
import edu.ucla.sspace.common.Similarity
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.RowMaskedMatrix
import edu.ucla.sspace.matrix.SparseRowMaskedMatrix
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.vector.Vectors
import edu.ucla.sspace.vector.VectorMath

import org.apache.commons.math.distribution.NormalDistributionImpl

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.PriorityQueue
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.math._

import java.util.Properties


class GMeans(val seed: KMeansSeed, 
             val criterion: CriterionFunction) extends Clustering {

    /**
     * The A^2_* threshold.  We reject the null hypothesis of a single cluster
     * whenever a split clustering solution has a separation statistics larger
     * than this threshold.
     */
    val threshold = 1.8692

    /**
     * Create a Normal Distribution with mean 0 and standard deviation of 1 so
     * we can easily determine the normal cumulative distribution function for
     * any value.
     */
    val norm = new NormalDistributionImpl(0, 1)

    /**
     * {@inheritDoc}
     */
    def cluster(matrix: Matrix, props: Properties) =
        cluster(matrix, Integer.MAX_VALUE, props)

    /**
     * {@inheritDoc}
     */
    def cluster(matrix: Matrix, maxClusters:Int, props: Properties) = {
        // Return one cluster in the odd case that only one is desired.
        if (maxClusters == 1)
            new Assignments(1, Array.fill(matrix.rows)(0), matrix)

        // The clusterQueue will store the set of sub-matrices that are
        // available for splitting.  Once this is empty, we have split every
        // cluster as much as possible according to gaussian statistics.  As a
        // best guess, we order each candidate cluster by it's size so that
        // larger clusters get tested for splitting first.
        val clusterQueue = new PriorityQueue()(new MatrixOrdering())

        // The clusterList stores the set of completed clusters in the form of
        // matrices.  If the list is of size one, then the assignments is
        // simple.  If the size is larger than one, then all matrices will be
        // masked matrices and we can consider the matrix to be a cluster of
        // points with a list of the original point's indices in matrix. 
        val clusterList = new ArrayBuffer[Matrix]()

        // Seed the clustering process with the first matrix.
        clusterQueue.enqueue(matrix)

        // Cluster until we can't split anything further according to the A^*_2
        // statistic.
        while (!clusterQueue.isEmpty) {
            // Get the cluster and attempt to split it using K-Means.
            val m = clusterQueue.dequeue
            val splitResult = splitCluster(m)

            // If we couldn't split, then add this cluster to the list of final
            // clusters.
            if (splitResult.numClusters == 1)
                clusterList.append(m)
            else
                // Otherwise split the two clusters into their own masked
                // matrices and add them to the cluster queue.
                for (cluster <- splitResult.clusters)
                    clusterQueue.enqueue(m match {
                        case sm:SparseMatrix => 
                                new SparseRowMaskedMatrix(sm, cluster) 
                        case _ => 
                                new RowMaskedMatrix(m, cluster) 
                     })

            // Check for the number of clusters found so far.  If we've hit the
            // limit, then add all candiate clusters to the finalized list and
            // the next iteration of the loop will stop processing.
            if (clusterList.size + clusterQueue.size == maxClusters)
                while (!clusterQueue.isEmpty)
                    clusterList.append(clusterQueue.dequeue)
        }

        if (clusterList.size == 1)
            new Assignments(1, Array.fill(matrix.rows)(0), matrix)
        else {
            val assignments = Array.fill(matrix.rows)(0)
            for ( (m, c) <- clusterList.zipWithIndex )
                m match {
                    case rm: RowMaskedMatrix =>
                        rm.reordering.foreach(assignments(_) = c)
                    case _ => throw new IllegalArgumentException(
                        "Invalid Matrix Found!")
                }
             new Assignments(clusterList.size, assignments, matrix)
        }
    }

    /**
     * Returns either a 2-clustering or a 1-cluster of the {@code matrix} based
     * on the splitting statistic.  If the 2-cluster solution appears to have
     * split a single gaussian distribution, this will reject the 2-cluster
     * hypothesis and retain the single cluster, otherwise it will return the
     * 2-cluster assignments.
     */
    def splitCluster(matrix: Matrix) = {
        // Split the cluster into two using K-Means with the desired Seeding
        // method and Criterion function.
        val twoClusters = DirectClustering.cluster(
            matrix, 2, 1, seed, criterion)
        // Extract the centroids.
        val centroids = twoClusters.getCentroids

        // Compute the vector that best splits the two centroids.
        val median = Vectors.copyOf(centroids(0))
        VectorMath.subtract(median, centroids(1))
        
        // Project each data point onto the splitting vector as a single scala
        // varlue.
        val projection = (0 until matrix.rows).map(r =>
            VectorMath.dotProduct(matrix.getRowVector(r), median) /
            pow(median.magnitude, 2))

        // Compute the mean and standard deviation of the projected values.
        val mean = projection.sum / projection.size
        val stdev = sqrt(projection.map(x => pow(x-mean, 2)).sum)
        val n = matrix.rows

        // Compute the A^2_* statistic based on the CDF score for the projected
        // values.  While doing this, we normalize the projected values to have
        // a mean of 0 and stdev of 1, and then find the cdf of those scaled
        // values, this is z.  Then the statistic evalutes z in a complicated
        // manner using the Anderson-Darling statistic with a correction for
        // having mean and stdev estimated from data, as opposed to known
        // values.
        val z = projection.map(x => 
            norm.cumulativeProbability((x - mean) / stdev))
        val a = -1/n * z.zipWithIndex.map(xi => 
            (2*xi._2 - 1) * (log(xi._1) + log(1 - z(n+1-xi._2)) ) - n).sum
        // This is the correction based on estimated mean and stdev.
        val a2 = a * ((1+4) / (n-25) / (n*n))

        // Check whether or not the statistic surpasses our threshold.  If it
        // does, retain the two cluster solution other wise return just a single
        // cluster solution.
        if (a2 <= threshold)
            new Assignments(1, Array.fill(matrix.rows)(0), matrix)
        else 
            twoClusters
    }

    /**
     * A simple ordering for matrices based on the number of rows.
     */
    class MatrixOrdering extends Ordering[Matrix] {
        def compare(x:Matrix, y:Matrix) =
            y.rows - x.rows
    }
}
