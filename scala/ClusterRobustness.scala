import edu.ucla.sspace.matrix.SymmetricMatrix

import java.io.PrintWriter

import scala.io.Source


object ClusterRobustness {

    def readClusters(clusterFile: String) = {
        val clusterData = Source.fromFile(clusterFile).getLines 
        val numDataPoints = clusterData.next.split("\\s+")(0).toDouble
        val clusters = (clusterData map { line => line.split("\\s+") map { x =>
            x.toInt } }).toSet
        (clusters, numDataPoints)
    }

    def readConsensusMatrix(fileName: String) = {
        val matrixLines = Source.fromFile(fileName).getLines
        val Array(rows, cols) = matrixLines.next.split("\\s+")
        val consensus = new SymmetricMatrix(rows.toInt, cols.toInt)
        for ((line, row) <- matrixLines zipWithIndex;
             (value, col) <- line.trim.split("\\s+") zipWithIndex)
            if (value != "")
                consensus.set(row.toInt, col.toInt, value.toDouble)
        consensus
    }

    /**
     * Returns a list of each item and it's number of duplicates in {@code ls}.
     * This method assumes that {@code ls} is sorted.
     *
     * @param ls A sequence of sorted items.
     *
     * @return A list of (item, count) tuples, with count holding the number of
     *         times item occurred in a row.
     */
    def countItems[T](ls: Seq[T]): List[(T, Int)] = {
        if (ls.isEmpty) List[Nothing]()
        else {
            val (packed, next) = ls span { _ == ls.head }
            if (next == Nil) List((ls.head, packed.length))
            else (ls.head, packed.length)::countItems(next)
        }
    }

    def main(args:Array[String]) {
        val dataFile = args(0)
        val minK = args(1).toInt
        val maxK = args(2).toInt

        val clusterWriter = new PrintWriter(args(3))
        clusterWriter.println("K Model Confidence")

        val pointWriter = new PrintWriter(args(4))
        pointWriter.println("K Model Confidence CDF")

        for (model <- args.view(5, args.length);
             k <- minK to maxK) {
            printf("Processing %s %d\n", model, k)
            val consensus = readConsensusMatrix(
                "%s.%s.cm%02d".format(dataFile, model, k))
            val (clusters, numDataPoints) = readClusters(
                "%s.%s.rca%02d".format(dataFile, model, k))

            // Print the confidence score for each cluster.
            val clusterConfidences = clusters map { cluster => 
                if (cluster.size < 2)
                    (0.0, 0.0)
                else {
                    val total = (cluster.combinations(2) map { index =>
                        consensus.get(index(0), index(1)) }).sum
                    val numPairs = .5 * cluster.size * (cluster.size - 1)
                    (total / numPairs, cluster.size / numDataPoints)
                }
            }
            val averageConfidence = clusterConfidences.foldLeft(0.0) (
                (sum, x) => sum + x._1*x._2)
            clusterWriter.println("%d %s %f".format(
                k, model, averageConfidence))
            
            // Print the confidence score for each individual data point.
            val pointConfidences = countItems((clusters map { cluster => 
                cluster map { i => 
                    (cluster map { j =>
                        if (i == j) 0 else consensus.get(i, j)}
                    ).sum/cluster.size}}).reduce(_++_).sorted)

            var sum = 0
            val cdf_k = pointConfidences foreach {
                ic => sum += ic._2; 
                pointWriter.println("%d %s %f %f".format(
                    k, model, ic._1, sum/numDataPoints))
            }
        }
        pointWriter.close
        clusterWriter.close

        println("done")
    }
}
