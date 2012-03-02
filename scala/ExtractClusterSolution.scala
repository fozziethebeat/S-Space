import edu.ucla.sspace.common.Similarity
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath

import scala.io.Source


/** 
 * Extracts the cluster solutions and reports them in the SemEval format.  This
 * works in two modes: (1) treating the cluster assignments as the final
 * solution and (2) building a classifier using the cluster assignments and
 * labeling each context with the best cluster label.
 */
object ExtractClusterSolution {

    def readCentroids(clusterFile: String, data:SparseMatrix) = {
        val clusterData = Source.fromFile(clusterFile).getLines 
        if (!clusterData.hasNext)
            System.exit(0)

        val numDataPoints = clusterData.next.split("\\s+")(0).toDouble
        (clusterData map { line => 
            val centroid = new CompactSparseVector(data.columns)
            for ( x <- line.split("\\s+"))
                VectorMath.add(centroid, data.getRowVector(x.toInt))
            centroid
        }).toArray
    }

    def main(args:Array[String]) {
        if (args(0) == "-c") {
            val data = MatrixIO.readSparseMatrix(args(1), Format.SVDLIBC_SPARSE_TEXT)
            val headers = Source.fromFile(args(2)).getLines.toList
            val centroids = readCentroids(args(3), data)

            val term = headers(0).replaceAll(".[0-9]+", "")

            for ((row, header) <- (0 until data.rows) zip headers) {
                val rowVector = data.getRowVector(row)
                var bestScore = 0.0
                var bestIndex = 0
                for ((centroid, i) <- centroids zipWithIndex) {
                    val score = Similarity.cosineSimilarity(centroid, rowVector)
                    if (score >= bestScore) {
                        bestScore = score
                        bestIndex = i
                    }
                }
                printf("%s %s %s.%d\n", term, header, term, bestIndex)
            }
        } else {
            val headers = Source.fromFile(args(1)).getLines.toList
            val term = headers(0).replaceAll(".[0-9]+", "")
            val clusterData = Source.fromFile(args(2)).getLines
            clusterData.next
            for ((line, clusterId) <- clusterData zipWithIndex ;
                 x <- line.split("\\s+") if x != "" )
                printf("%s %s %s.%d\n", term, headers(x.toInt), term, clusterId)
        }
    }
}
