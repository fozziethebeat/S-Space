import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.matrix.SymmetricMatrix

import java.io.File
import java.io.PrintWriter

import scala.collection.mutable.HashSet
import scala.io.Source


object ExtractConsensusAssignments {

    def findClusters(assignments: Assignments, rowMap: Array[Int]) = {
        val clusters = Array.fill(assignments.numClusters)(HashSet[Int]())
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment(0)).add(if (rowMap == null) i else rowMap(i))
        clusters map { cluster => cluster toArray }
    }

    def printAssignments(assignments: Assignments, rows: Int, fileName: String) { 
        val writer = new PrintWriter(fileName)
        writer.println("%d %d".format(rows, assignments.numClusters))
        for (points <- findClusters(assignments, null)) {
            for (point <- points)
                writer.print("%d ".format(point))
            writer.println
        }
        writer.close
    }

    def readConsensusMatrix(fileName: String, scale: Double) = {
        val matrixLines = Source.fromFile(fileName).getLines
        val Array(rows, cols) = matrixLines.next.split("\\s+")
        val consensus = new SymmetricMatrix(rows.toInt, cols.toInt)
        for ((line, row) <- matrixLines zipWithIndex;
             (value, col) <- line.trim.split("\\s+") zipWithIndex;
             if value != "")
                consensus.set(row.toInt, col.toInt, value.toDouble/scale)
        consensus
    }

    def updateConsensusMatrix(fileName: String, cm: Matrix, scale: Double) {
        val matrixLines = Source.fromFile(fileName).getLines
        matrixLines.next
        for ((line, row) <- matrixLines zipWithIndex;
             (value, col) <- line.trim.split("\\s+") zipWithIndex;
            if value != "")
                cm.add(row.toInt, col.toInt, value.toDouble/scale)
    }

    def main(args:Array[String]) {
        val k = args(1).toInt
        val numModels = (args.size - 2).toDouble
        val cma = readConsensusMatrix(args(2), numModels)
        for (cmFile <- args.view(3,args.length))
            updateConsensusMatrix(cmFile, cma, numModels)

        val assignments = HierarchicalAgglomerativeClustering.toAssignments(
            HierarchicalAgglomerativeClustering.clusterSimilarityMatrix(
                cma, -1, ClusterLinkage.MEAN_LINKAGE, k),
            null, k)
        printAssignments(assignments, cma.rows, args(0))
    }
}
