import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.SparseMatrix

import scala.collection.mutable.HashMap

import java.util.Properties


class PageRankClustering {

    val d = .85

    def findMinSpanningTree(adj:Matrix, hubs:Set[Int]) {
        val disconnectedVertices = HashMap[Int, (Int,Double)]()
        // To start, prime the distance from the root node as the maximum value
        // except to the selected hubs.  Hubs have a negative distance so that
        // we pick those first every time.
        for (v <- 0 until adj.rows)
            if (hubs.contains(v))
                disconnectedVertices(v) = (-1, -1)
            else
                disconnectedVertices(v) = (-1, java.lang.Double.MAX_VALUE)

        while (!disconnectedVertices.isEmpty) {
            // Find the node that has the shortest link to some node already
            // explored.
            var bestDistance = java.lang.Double.MAX_VALUE
            var bestNode = -1 
            var bestLink = -1 
            for ( (v, (from, w)) <- disconnectedVertices)
                if (w <= bestDistance) {
                    bestDistance = w
                    bestNode = v
                    bestLink = from
                }

            // Remove the chosen node.
            disconnectedVertices.remove(bestNode)

            // If this node is closer to unselected nodes, update their weights
            // so that they may be chosen later on.
            for ( (v, (from, w)) <- disconnectedVertices) {
                val newW = adj.get(v, bestNode)
                if (newW < w)
                    disconnectedVertices(v) = (bestNode, newW)
            }

            // Return to, from link.
            (bestNode, bestLink)
        }
    }

    def cluster(adj:Matrix, props:Properties) = {
        val n = adj.rows.toDouble
        val initialRanks = ((0 until adj.rows).map { _ => 1.0/n }).toArray
        val columnSums = sumColumns(adj)

        var ranks = initialRanks
        for (i <- 0 until 50) {
            var newRanks = new Array[Double](adj.rows)
            adj match {
                case sm:SparseMatrix =>
                    for (r <- 0 until adj.rows) {
                        val sv = sm.getRowVector(r)
                        for (c <- sv.getNonZeroIndices)
                            newRanks(r) += sv.get(c) / columnSums(c) * ranks(r)
                    }
                case m:Matrix =>
                    for (r <- 0 until adj.rows)
                        for (c <- 0 until adj.columns)
                            newRanks(r) += m.get(r, c) / columnSums(c) * ranks(r)
            }
            for (r <- 0 until adj.columns)
                newRanks(r) = newRanks(r) * d + (1-d) * initialRanks(r)
            ranks = newRanks
        }

        // Now somehow magically select some number of top hubs
        val something = Set(0, 1, 2)
        findMinSpanningTree(adj, something)
        something
    }

    def sumColumns(adj:Matrix) = {
        val colSums = new Array[Double](adj.columns)
        adj match {
            case sm:SparseMatrix => 
                for (r <- 0 until adj.rows) {
                    val sv = sm.getRowVector(r)
                    for (c <- sv.getNonZeroIndices)
                        colSums(c) += sm.get(r,c)
                }
            case m:Matrix =>
                for (r <- 0 until adj.rows)
                    for (c <- 0 until adj.columns)
                        colSums(c) += m.get(r,c)
        }
        colSums
    }
}
