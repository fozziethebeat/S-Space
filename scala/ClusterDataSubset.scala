import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.util.ReflectionUtil
import edu.ucla.sspace.util.SerializableUtil

import java.io.File
import java.io.PrintWriter

import scala.collection.mutable.HashSet
import scala.io.Source

import util.Random


object ClusterDataSubset {

    def findClusters(assignments: Assignments, rowMap: Array[Int]) = {
        val clusters = 0 until assignments.numClusters map {
            _ => HashSet[Int]() }
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment(0)).add(
              if (rowMap == null) i else rowMap(i))
        clusters map { cluster => cluster toArray }
    }

    def main(args:Array[String]) {
        val alg:Clustering = ReflectionUtil.getObjectInstance(args(0))
        val data = MatrixIO.readSparseMatrix(args(1), Format.SVDLIBC_SPARSE_TEXT)
        val basis:BasisMapping[String, String] = SerializableUtil.load(args(2))
        val fraction = args(3).toDouble
        val numClusters = args(4).toInt
        val outputBase = args(5)

        val columnMap = (0 until data.columns).toArray
        val rowMap = Source.fromFile(args(6)).getLines.toList.map(_.toInt)

        val props = System.getProperties
        
        val rowMask = (rowMap take (fraction*data.rows).toInt) toArray
        val dataSubset = new CellMaskedSparseMatrix(
            data, rowMask, columnMap)
        val assignments = alg.cluster(dataSubset, numClusters, props)

        // Write out the cluster assignments.
        val clustWriter = new PrintWriter("%s.sol".format(outputBase))
        clustWriter.println("%d %d".format(dataSubset.rows, numClusters))
        for (points <- findClusters(assignments, null))
            clustWriter.println(points.mkString(" "))
        clustWriter.close

        // Now compute the top 10words per cluster and print them out.
        val topWriter = new PrintWriter("%s.top10".format(outputBase))
        for (centroid <- assignments.getSparseCentroids()) {
            val selected = (centroid.getNonZeroIndices map {
                i => (centroid.get(i), i) }).sorted.reverse.take(10)
            for ((_, index) <- selected)
                topWriter.printf(
                    "%s ", basis.getDimensionDescription(index))
            topWriter.println()
        }
        topWriter.close
    }
}
