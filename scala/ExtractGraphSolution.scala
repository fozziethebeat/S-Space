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
import scala.io.Source

import java.io.File

// Arguments:
// (1) solution file
// (2) header file
// (3) basis file
// (4) context file

object ExtractGraphSolution {

    def main(args: Array[String]) {
        val clusterMatrix = readSolution(args(0))
        val headers = readHeaders(args(1))
        val basis = readBasis(args(2))
        basis.setReadOnly(true)
        val term = headers(0).replaceAll(".[0-9]+", "")

        for ( (headerId, rowVec) <- readContextMatrix(args(3), basis)) {
            val header = headers(headerId)
            val (clusterId, _) = rowVectors(clusterMatrix).foldLeft((-1,-1.0))(
                (best, cluster) => { 
                    val sim = Similarity.jaccardIndex(rowVec, cluster._1)
                    if (sim >= best._2) (cluster._2, sim) else best
            })
            printf("%s %s %s.%d\n", term, header, term, clusterId)
        }
    }

    def rowVectors(m:SparseMatrix) =
        for (r <- 0 until m.rows) yield (m.getRowVector(r), r)

    def readHeaders(headerFile: String) = 
        Source.fromFile(headerFile).getLines.toList

    def readBasis(basisFile: String):BasisMapping[String,String] = 
        SerializableUtil.load(basisFile)

    def readContextMatrix(contextFile: String, basis:BasisMapping[String, String]) = {
        val docIter = new DependencyFileDocumentIterator(contextFile)
        for (doc <- docIter) yield {
            val reader = doc.reader
            val l = reader.readLine
            val headerId = l.split("\\.")(2).toInt
            val rowVector = new CompactSparseVector()
            var line = reader.readLine
            while (line != null) {
                val d = basis.getDimension(line.split("\\s+")(1))
                if (d >= 0)
                    rowVector.set(d, 1)
                line = reader.readLine
            }
            (headerId-1, rowVector)
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
