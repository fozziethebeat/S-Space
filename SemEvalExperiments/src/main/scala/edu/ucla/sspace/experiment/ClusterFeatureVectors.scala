package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.CKVWSpectralClustering06
import edu.ucla.sspace.clustering.DirectClustering
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering
import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorIO

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.util.Random

import java.io.FileReader


object ClusterFeatureVectors {
    def main(args: Array[String]) {
        val alg = args(0) match {
            case "kmeans" => new DirectClustering()
            case "hac" => new NeighborChainAgglomerativeClustering()
            case "eigen" => new CKVWSpectralClustering06()
        }

        val numClusters = args(1).toInt

        val data = Matrices.asSparseMatrix(VectorIO.readSparseVectors(
            new FileReader(args(2))))

        val assignments = alg.cluster(data, numClusters, System.getProperties())
        val partition = Partition.fromAssignments(assignments)
        PartitionUtil.write(partition, args(3))
    }
}
