import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format

import scala.collection.mutable.HashSet

import java.io.File
import java.io.PrintWriter


object CluterMultiModel {

    def findClusters(assignments: Assignments, rowMap: Array[Int]) = {
        val clusters = 0 until assignments.numClusters map {
            _ => HashSet[Int]() }
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment(0)).add(
              if (rowMap == null) i else rowMap(i))
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

    def main(args:Array[String]) {
        val dataset = MatrixIO.readMatrix(new File(args(0)), Format.SVDLIBC_DENSE_TEXT)
        val k = args(1).toInt
        val hac = new HierarchicalAgglomerativeClustering()
        val assignments = hac.cluster(dataset, k, System.getProperties)
        printAssignments(assignments, dataset.rows, args(2))
    }
}
