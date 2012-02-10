import edu.ucla.sspace.common.Similarity
import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.matrix.SymmetricMatrix
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath
import edu.ucla.sspace.util.ReflectionUtil

import scala.collection.mutable.HashSet
import scala.io.Source
import scala.util.Random

import java.io.File
import java.io.PrintWriter


object ClusterMostConfident {

    def readClusters(clusterFile: String) = {
        val clusterData = Source.fromFile(clusterFile).getLines 
        val numDataPoints = clusterData.next.split("\\s+")(0).toDouble
        val clusters = (clusterData map { line => line.split("\\s+") map { x =>
            x.toInt } }).toSet
        clusters
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
     * Returns a sequence of data point partitions.  Each returned sequence will
     * be the data point indices assigned to that cluster.  Note that the {@link
     * Assignments} class provides a method just like this, but scala does not
     * behave well with the type casting.
     *
     * @param assignments The set of cluster assignments from a single run
     *
     * @return A sequence of sets, with each set holding the datapoint ids for
     *         each cluster.
     */
    def findClusters(assignments: Assignments, rowMap: Array[Int]) = {
        val clusters = 0 until assignments.numClusters map {
            _ => HashSet[Int]() }
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment(0)).add(
              if (rowMap == null) i else rowMap(i))
        clusters map { cluster => cluster toArray }
    }

    def computeConfidence(x: Int, cluster:Array[Int], consensus:Matrix) =
        (for (y <- cluster; if x != y) yield consensus.get(x, y)).sum

    def computeNonConfidence(x: Int, i: Int, 
                             clusters: Set[Array[Int]], consensus:Matrix) =
        (for ((cluster, j) <- clusters zipWithIndex; if i != j; y <- cluster)
            yield 1.0-consensus.get(x,y)).sum

    def printSingleCluster(ordering:Array[Int], nameBase: String) {
        for (fraction <- .50 to 1.00 by .10) {
            val numRows = (fraction * ordering.size).toInt
            val writer = new PrintWriter("%s.%.02f".format(
                nameBase, fraction))
            writer.println("%d %d".format(numRows, 1))
            for (point <- ordering take numRows)
                writer.print("%d ".format(point))
            writer.println
            writer.close
        }
    }

    def confidenceOrder(clusters: Set[Array[Int]], consensus: Matrix) =
        (for ((cluster, i) <- clusters zipWithIndex; x <- cluster) yield {
            val interSim = computeConfidence(x, cluster, consensus)
            val intraSim = computeNonConfidence(x, i, clusters, consensus) 
            ((interSim+intraSim)/(consensus.rows-1.0), x)
        }).toArray.sorted.reverse.map(_._2)

    def randomOrder(numRows:Int) = 
        Random.shuffle(0 until numRows).toArray

    def similarityOrder(dataset: SparseMatrix) = {
        val centroid = new CompactSparseVector(dataset.columns)
        for (r <- 0 until dataset.rows)
            VectorMath.add(centroid, dataset.getRowVector(r))

        (((0 until dataset.rows) map { r => 
            (Similarity.cosineSimilarity(centroid, dataset.getRowVector(r)), r)
        }).sorted.reverse).toArray.map { _._2 }
    }

    def main(args:Array[String]) {
        val dataFile = MatrixIO.readSparseMatrix(args(1), Format.SVDLIBC_SPARSE_TEXT)
        val numClusters = args(3).toInt

        val ordering = args(0) match {
            case "-c" => if (numClusters == 1) 0 until dataFile.rows toArray 
                         else confidenceOrder(readClusters(args(4)), readConsensusMatrix(args(5)))
            case "-r" => randomOrder(dataFile.rows)
            case "-s" => similarityOrder(dataFile)
            case "-n" => 0 until dataFile.rows toArray
        }

        if (numClusters == 1) {
            printSingleCluster(ordering, args(6))
            return
        }

        val alg:Clustering = ReflectionUtil.getObjectInstance(args(2))
        val props = System.getProperties


        (.50 to 1.00 by .10).par.foreach { fraction =>
            System.err.println("reporter:counter:fraction_start,%f,1".format(fraction))
            val numRows = (fraction * dataFile.rows).ceil.toInt
            printf("Clustering best %d rows with %s\n", numRows, alg.toString) 

            val savedIndices = ordering take numRows toSet
            val rowMask:Array[Int] = (0 until dataFile.rows).filter(savedIndices.contains(_)).toArray
            val columnMask = 0 until dataFile.columns toArray
            val maskedData = new CellMaskedSparseMatrix(
                dataFile, rowMask, columnMask)

            val assignments = alg.cluster(maskedData, numClusters, props)
            val writer = new PrintWriter("%s.%.02f".format(
                args(6), fraction))
            writer.println("%d %d".format(maskedData.rows, numClusters))
            for (points <- findClusters(assignments, null))
                writer.println(points.map(rowMask(_)).mkString(" "))
            writer.close
            System.err.println("reporter:counter:fraction_stop,%f,1".format(fraction))
        }
        println("done")
    }
}
