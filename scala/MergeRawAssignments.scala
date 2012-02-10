import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.SymmetricMatrix

import java.io.PrintWriter

import scala.collection.mutable.HashSet
import scala.io.Source


object ExtractConsensusAssignments {

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

    def readClusters(solution:String, consensus:Matrix, scale:Double):Matrix = {
        val lines = Source.fromFile(solution).getLines
        val Array(rows, _) = lines.next.split("\\s").map(_.toInt)
        val m = if (consensus == null) new SymmetricMatrix(rows, rows)
                else consensus
        for (line <- lines; 
             Array(x,y) <- line.split("\\s+").map(_.toInt).combinations(2))
            m.add(x,y, 1.0/scale)
        m
    }

    def main(args:Array[String]) {
        val k = args(1).toInt
        val numModels = (args.size - 2).toDouble
        val cma = args.slice(3, args.length).foldLeft(null:Matrix){
            (cm, s) => readClusters(s, cm, numModels)}
        val assignments = HierarchicalAgglomerativeClustering.toAssignments(
            HierarchicalAgglomerativeClustering.clusterSimilarityMatrix(
                cma, -1, ClusterLinkage.MEAN_LINKAGE, k),
            null, k)
        printAssignments(assignments, cma.rows, args(0))
    }
}
