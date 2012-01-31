import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.SymmetricIntMatrix
import edu.ucla.sspace.matrix.SymmetricMatrix
import edu.ucla.sspace.matrix.TransformStatistics

import scala.io.Source
import scala.math.log


object DisagreementAverage {

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

    def readClusters(clusterFile: String) = {
        var numDataPoints = 0
        val clusters = (Source.fromFile(clusterFile).getLines map { line =>
            line.split("\\s+") map { x => numDataPoints += 1; x.toInt } }).toSet
        (clusters, numDataPoints)
    }

    def readReferenceMatrix(fileName: String) = {
        val (clusters, numDataPoints) = readClusters(fileName)
        val consensus = new SymmetricIntMatrix(numDataPoints, numDataPoints)
        for (cluster <- clusters;
             Array(x,y) <- cluster.combinations(2))
            consensus.set(x, y, 1)
        consensus
    }

    def computeOverlap(clusterFile: String, classFile:String) = {
        val (computedClusters, n) = readClusters(clusterFile)
        val (expectedClusters, _) = readClusters(classFile)
        val overlap = new ArrayMatrix(
            expectedClusters.size, computedClusters.size)

        for ((expected, i) <- expectedClusters zipWithIndex;
             dataPoint <- expected;
             (computed, j) <- computedClusters zipWithIndex)
            if (computed contains dataPoint)
                overlap.set(i, j, 1 + overlap.get(i, j))
        (overlap, n.toDouble)
    }

    def computeStatistics(matrix: Matrix) = {
        val matrixStats = TransformStatistics.extractStatistics(matrix)
        (matrixStats.rowSums, matrixStats.columnSums)
    }

    def getClustEntropy(m: Matrix, i: Int, N: Double, sum: Double) =
         (for (j <- 0 until m.columns) yield {
             val v = m.get(i,j)
             if (v == 0d) 0 else v/N * log(v/sum)
         }).sum

    def getClassEntropy(m: Matrix, j: Int, N: Double, sum: Double) =
         (for (i <- 0 until m.rows) yield {
             val v = m.get(i,j)
             if (v == 0d) 0 else v/N * log(v/sum)
         }).sum

    def computeVMeasure(clusterFile: String, classFile: String) = {
        val (overlap, n) = computeOverlap(clusterFile, classFile)
        val (classSums, clusterSums) = computeStatistics(overlap)

        val (classEntropy, condClusterEntropy) = {
            val x = (for (i <- 0 until overlap.rows) yield
              ((classSums(i)/n) * log(classSums(i)/n),
               getClustEntropy(overlap, i, n, classSums(i)))).unzip
            (x._1.sum, x._2.sum)
        }

        val (clusterEntropy, condClassEntropy) = {
            val x = (for (j <- 0 until overlap.columns) yield
              ((clusterSums(j)/n) * log(clusterSums(j)/n),
               getClassEntropy(overlap, j, n, clusterSums(j)))).unzip
            (x._1.sum, x._2.sum)
        }

        val homogeniety = 
            if (classEntropy == 0d) 1 else 1 - condClassEntropy/classEntropy
        val completeness = 
            if (clusterEntropy == 0d) 1 else 1 - condClusterEntropy/clusterEntropy
        val vMeasure = 2*homogeniety*completeness/(homogeniety+completeness)

        (vMeasure, homogeniety, completeness)
    }

    def disagreementAverage(consensus1: Matrix, consensus2: Matrix) = {
        var sum = 0.0
        var numDiferences = 0.0
        var numAgreements = 0.0
        var numEmptyAgreements = 0.0
        var c1Sum = 0.0
        var c1Count = 0.0
        var c2Sum = 0.0
        var c2Count = 0.0

        // Iterate through every unique item pair in the two consensus matrices
        // and compute a lot of summary statistics that are used in the Kappa
        // score, F-Score, and average disagreement.
        for (r <- 0 until consensus1.rows; c <- 0 until r) {
            val c1Value = consensus1.get(r,c)
            if (c1Value != 0d) {
                c1Sum += c1Value
                c1Count += 1.0
            }

            val c2Value = consensus2.get(r,c)
            if (c2Value != 0d) {
                c2Sum += c2Value
                c2Count += 1.0
            }

            val difference = c1Value - c2Value

            if (difference != 0d) {
                sum += difference
                numDiferences += 1.0
            } else if (c2Value != 0d && c1Value != 0d)
                numAgreements += c2Value * c1Value 
            else
                numEmptyAgreements += 1.0
        }

        // Compute the kappa score.
        val numPairs = consensus1.rows * (consensus1.rows - 1) * .5
        var c1EmptyCount = numPairs - c1Count
        var c2EmptyCount = numPairs - c2Count
        var p0 = (1/numPairs) * (numAgreements + numEmptyAgreements)
        var pe = (1/(numPairs*numPairs)) * 
                 (c1Sum*c2Sum + c1EmptyCount*c2EmptyCount)
        var kappaScore = (p0 - pe) / (1 - pe)

        // Compute the average disagreement, although this is less than useful.
        val averageDisagreement = sum / numDiferences
        val averageC1 = c1Sum / c1Count
        val averageC2 = c2Sum / c2Count

        // Compute the paired F-Score.
        val precision = numAgreements / c1Sum
        val recall = numAgreements / c2Sum 
        val pairedFScore = 2 * precision * recall / (precision + recall)

        (averageDisagreement, pairedFScore, kappaScore)
    }

    def main(args: Array[String]) {
        val (matrixReader, first, rest) =
            if (args(0) == "-r") {
                println("K fscore kappa vmeasure homogeneity completeness")
                (readReferenceMatrix _, args(1), args.slice(2, args.length))
            } else {
                println("K fscore kappa")
                (readConsensusMatrix _, args(0), args.slice(1, args.length))
            }

        var prev = matrixReader(first)
        var prevFile = first

        for ((file, i) <- rest zipWithIndex) {
            val curr = matrixReader(file)
            val (davg, fScore, kappa) = disagreementAverage(prev, curr)

            if (args(0) == "-r") {
                val (v, h, c) = computeVMeasure(prevFile, file)
                printf("%d %f %f %f %f %f\n",
                       i+3, fScore, kappa, v, h, c)
                prevFile = file
            } else
                printf("%d %f %f\n", i+3, fScore, kappa)

            prev = curr
        }
    }
}
