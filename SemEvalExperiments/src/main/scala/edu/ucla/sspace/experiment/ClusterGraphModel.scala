package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.ChineseWhispers
import edu.ucla.sspace.clustering.GraphHac
import edu.ucla.sspace.clustering.NormalizedSpectralClustering
import edu.ucla.sspace.clustering.PageRankClustering
import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.vector.SparseDoubleVector

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.util.Random

import java.io.FileReader


object ClusterGraphModel {
    def main(args: Array[String]) {
        val alg = args(0) match {
            case "cw" => new ChineseWhispers()
            case "ghac" => new GraphHac()
            case "spec" => new NormalizedSpectralClustering()
            case "prc" => new PageRankClustering()
        }

        val numClusters = args(1).toInt

        val data = MatrixIO.readMatrix(args(2), Format.SVDLIBC_SPARSE_TEXT)

        val assignments = alg.cluster(data, numClusters, System.getProperties())
        val partition = Partition.fromAssignments(assignments)
        PartitionUtil.write(partition, args(3))
    }
}
