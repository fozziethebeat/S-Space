package edu.ucla.sspace.clustering

import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.io.Source

import java.io.File
import java.util.Properties


class PageRankClustering extends AbstractGraphClustering {

    val d = .85

    def findMinSpanningTree(adj:Matrix, hubs:Set[Int]) = {
        val disconnectedVertices = HashMap[Int, (Int,Double, Int)]()
        // To start, prime the distance from the root node as the maximum value
        // except to the selected hubs.  Hubs have a negative distance so that
        // we pick those first every time.
        for (v <- 0 until adj.rows)
            if (hubs.contains(v))
                disconnectedVertices(v) = (-1, java.lang.Double.MAX_VALUE, -1)
            else
                disconnectedVertices(v) = (-1, -1.0, -1)

        val clusters = HashMap[Int, HashSet[Int]]()
        while (!disconnectedVertices.isEmpty) {
            // Find the node that has the shortest link to some node already
            // explored.
            var bestSim = -1.0
            var bestDest = -1 
            var bestFrom = -1 
            var bestParent = -1
            for ( (v, (from, w, p)) <- disconnectedVertices)
                if (w >= bestSim) {
                    bestSim = w
                    bestDest = v
                    bestFrom = from
                    bestParent = p
                }

            // Remove the chosen node.
            disconnectedVertices.remove(bestDest)

            // If this node is closer to unselected nodes, update their weights
            // so that they may be chosen later on.
            for ( (v, (from, w, p)) <- disconnectedVertices) {
                val newW = adj.get(v, bestDest)
                if (newW >= w) {
                    val newP = if (bestParent == -1) bestFrom
                               else bestParent
                    disconnectedVertices(v) = (bestDest, newW, newP)
                }
            }

            if (bestFrom == -1)
                clusters(bestDest) = HashSet[Int](bestDest)
            else if (bestParent == -1)
                clusters.get(bestFrom).get.add(bestDest)
            else
                clusters.get(bestParent).get.add(bestDest)
        }

        val assignments = new Assignments(clusters.size, adj.rows)
        for ((cluster, id) <- clusters.values.zipWithIndex; point <- cluster)
            assignments.set(point, id)
        assignments
    }

    def cluster(adj:Matrix, props:Properties) =
        cluster(adj, 100, props)

    def cluster(adj:Matrix, numClusters:Int, props:Properties) = {
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
                            newRanks(r) += m.get(r, c) / columnSums(c)*ranks(r)
            }
            for (r <- 0 until adj.columns)
                newRanks(r) = newRanks(r) * d + (1-d) * initialRanks(r)
            ranks = newRanks
        }

        val orderedRanks = ranks.zipWithIndex.toList.sorted.reverse.map ( _._2)
        findMinSpanningTree(adj, orderedRanks take numClusters toSet)
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
