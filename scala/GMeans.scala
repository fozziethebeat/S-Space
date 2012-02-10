import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.clustering.DirectClustering
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.vectors.Vectors
import edu.ucla.sspace.vectors.VectorMath

import org.apache.commons.math.distribution.NormalDistributionImpl

import scala.math._

import java.util.Properties


class GMeans(val alpha: Double,
             val seed: KMeansSeed, 
             val criterion: CriterionFunction) implements Clustering {

    def cluster(matrix: Matrix, props: Properties) = {
        val norm = new NormalDistributionImpl(0, 1)
        val twoClusters = DirectClustering.cluster(
            matrix, 2, 1, seed, criterion)
        val centroids = twoClusters.getCentroids
        val median = Vectors.copyOf(centroids(0))
        VectorMath.subtract(median, centroids(1))
        val projection = (0 until matrix.size).map(r =>
            Similarity.dotProduct(matrix.getRowVector(r), median) /
            pow(median.magnitude, 2))
        val mean = projection.sum / projection.size
        val stdev = sqrt(projection.map(x => Math.pow(x-mean)).sum)
        val n = matrix.rows
        val z = projection.map(x => norm.cumulativeProbability((x - mean) / sdev))
        val a = -1/n * z.zipWithIndex.map(xi => (2*xi._2 - 1) * (log(x._1) + log(1 - z(n+1-xi._2)) ) - n).sum
        val a2 = a * ((1+4) / (n-25) / (n*n))
        // Do something magical with a2 to see if it's in the range of
        // non-critical values with respect to alpha.
    }
}
