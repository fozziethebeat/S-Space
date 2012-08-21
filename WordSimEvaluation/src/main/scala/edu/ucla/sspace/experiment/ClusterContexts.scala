package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.CKVWSpectralClustering06
import edu.ucla.sspace.clustering.DirectClustering
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering.ClusterLink
import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorIO

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.util.Random

import java.io.FileReader


object ClusterContexts {
    def main(args: Array[String]) {
        val alg = args(0) match {
            case "kmeans" => new DirectClustering()
            case "hac-avg" => new NeighborChainAgglomerativeClustering(ClusterLink.MEAN_LINK, new CosineSimilarity())
            case "hac-single" => new NeighborChainAgglomerativeClustering(ClusterLink.SINGLE_LINK, new CosineSimilarity())
            case "hac-complete" => new NeighborChainAgglomerativeClustering(ClusterLink.COMPLETE_LINK, new CosineSimilarity())
            case "hac-median" => new NeighborChainAgglomerativeClustering(ClusterLink.MEDIAN_LINK, new CosineSimilarity())
            case "eigen" => new CKVWSpectralClustering06()
        }

        val numClusters = args(1).toInt
        val maxVectors = 20000

        val vectors = VectorIO.readSparseVectors(new FileReader(args(2))).take(maxVectors).toList
        val data = Matrices.asSparseMatrix(vectors)

        val assignments = alg.cluster(data, numClusters, System.getProperties())
        val partition = Partition.fromAssignments(assignments)
        PartitionUtil.write(partition, args(3))
    }
}
