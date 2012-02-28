import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.common.Similarity
import edu.ucla.sspace.matrix.YaleSparseMatrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.CompactSparseVector

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.mutable.HashSet
import scala.io.Source

import java.io.File
import java.io.PrintWriter

// Arguments:
// (1) solution file
// (2) result file 
// (3) basis file
// (4) context file

/**
 * Extracts the cluster labels for each data point using the distributed graph
 * representation.
 */
object ConvertGraphSolution {

    def main(args: Array[String]) {
        val clusterMatrix = readSolution(args(0))
        val basis = readBasis(args(2))
        basis.setReadOnly(true)

        var numPoints = 0
        val finalClusters = Array.fill(clusterMatrix.rows)(new HashSet[Int]())
        for ( (instanceId, rowVec) <- readContextMatrix(args(3), basis)) {
            val (clusterId, _) = rowVectors(clusterMatrix).foldLeft((-1,-1.0))(
                (best, cluster) => { 
                    val sim = Similarity.jaccardIndex(rowVec, cluster._1)
                    if (sim >= best._2) (cluster._2, sim) else best
            })
            finalClusters(clusterId).add(instanceId)
            numPoints += 1
        }

        val writer = new PrintWriter(args(1))
        writer.println("%d %d".format(numPoints, clusterMatrix.rows))
        for ( cluster <- finalClusters )
            writer.println(cluster.mkString(" "))
        writer.close
    }

    def rowVectors(m:SparseMatrix) =
        for (r <- 0 until m.rows) yield (m.getRowVector(r), r)

    def readBasis(basisFile: String):BasisMapping[String,String] = 
        SerializableUtil.load(basisFile)

    def readContextMatrix(contextFile: String, basis:BasisMapping[String, String]) = {
        val docIter = new DependencyFileDocumentIterator(contextFile)
        for (doc <- docIter) yield {
            val reader = doc.reader
            val l = reader.readLine
            val instanceId = l.split("\\.")(2).toInt
            val rowVector = new CompactSparseVector()
            var line = reader.readLine
            while (line != null) {
                val d = basis.getDimension(line.split("\\s+")(1))
                if (d >= 0)
                    rowVector.set(d, 1)
                line = reader.readLine
            }
            (instanceId-1, rowVector)
        }
    }

    def readSolution(solution: String) = {
        val lines = Source.fromFile(solution).getLines
        val Array(numPoints, numClusters) = lines.next.split("\\s+").map(_.toInt)
        val clusterMatrix = new YaleSparseMatrix(numClusters, numPoints)
        for ( (line, clusterIndex) <- lines.zipWithIndex;
              point <- line.split("\\s+") if point != "" )
            clusterMatrix.set(clusterIndex, point.toInt, 1)
        clusterMatrix
    }
}
