import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.Matrix

import scala.collection.mutable.HashSet
import scala.io.Source
import scala.math.Ordering
import scala.util.Random

import java.io.PrintWriter


val numClusters = args(0).toInt
val mergeType = args(1)
val outFile = args(2)
System.err.println("reporter:status:Loading partitions")
val partitions = args.slice(3, args.size).map(Partition(_)).toArray

System.err.println("reporter:status:Finding best of K")
var bestPartition = partitions(0)
var bestScore = partitions.map(bestPartition.sdd(_)).sum
for (p1 <- partitions.slice(1, partitions.size)) {
    val sddSum = partitions.map(p1.sdd(_)).sum
    if (sddSum < bestScore) {
        bestScore = sddSum
        bestPartition = p1
    }
}

System.err.println("reporter:status:Finding BOEM")
val best = mergeType match {
    case "filtered" => filteredBoem(bestPartition, partitions, numClusters)
    case "complete" => boem(bestPartition, partitions, numClusters)
}
System.err.println("reporter:status:Done")
val writer = new PrintWriter(outFile)
writer.println(best)
writer.close
    
def boem(bestPartition: Partition,
         partitions:Array[Partition],
         numClusters:Int) = {
    var best = Partition(bestPartition)
    val numPoints = bestPartition.assignments.size
    var improved = true
    var bestMove = (-1, -1)
    var bestScore = totalCost(best, partitions)
    var count = 0
    while (improved) {
        System.err.println("reporter:counter:boem,move,1")
        improved = false
        // Consider moving each data point.
        for (r <- 0 until numPoints) {
            // Get the points current cluster.
            val elemCluster = best.assignments(r)

            // Define a helper function for computing the cost of moving the point
            // without actually changing the points assignment.
            def move(c:Int) = {
                best.move(r, c)
                val cost = totalCost(best, partitions)
                best.move(r, elemCluster)
                cost
            }

            // Check the improvement for moving the point to each alternate
            // cluster and update the best score for it.
            for (c <- 0 until numClusters) {
                val score = if (c == elemCluster) bestScore
                            else move(c)
                if (score < bestScore) {
                    bestScore = score
                    bestMove = (r, c)
                    improved = true
                }
            }
        }

        // If we found an improvement, make the move.
        if (improved)
            best.move(bestMove._1, bestMove._2)

        // Set a hard counter so that the moving does not get stuck in a local
        // minimum of swaps.
        count += 1
        if (count == 200)
            improved = false
    }
    best
}

def totalCost(partition: Partition, partitions: Array[Partition]) =
    partitions.par.map(partition.sdd(_)).sum

def filteredBoem(bestPartition: Partition,
                 partitions:Array[Partition],
                 numClusters:Int) = {
    var best = Partition(bestPartition)
    val beta = .99
    val numIterations = 75
    val numPoints = bestPartition.assignments.size
    val stocasticCost = new ArrayMatrix(numPoints, numClusters)

    for (t <- 0 until numIterations) {
        System.err.println("reporter:counter:filtered boem,move,1")
        printf("Iteration %d\n", t)
        val pIndex = Random.nextInt(partitions.size)
        val selected = partitions(pIndex)
        val (point, newCluster) = findBestMove(stocasticCost, beta, 
                                               best, selected)
        best.move(point, newCluster)
    }
    best
}

def findBestMove(stocasticCost: Matrix,
                 beta: Double,
                 best: Partition,
                 selected: Partition) = {
    // Compute the cost from the best partition to the selected partition.  This
    // cost will be re-used whenever we try to move a point to it's currently
    // assigned cluster.
    val fixedCost = best.sdd(selected)

    // Initialize the best move and the best move's score with default values
    // signifying that no choice has been made.
    var bestMove = (-1, -1)
    var bestCost = Double.MaxValue

    // Iterate through each point and update the stocastic cost matrix by
    // considering moving each point to a different cluster.
    for (r <- 0 until stocasticCost.rows) {
        // Get the points current cluster.
        val elemCluster = best.assignments(r)

        // Define a helper function for computing the cost of moving the point
        // without actually changing the points assignment.
        def move(c:Int) = {
            best.move(r, c)
            val cost = best.sdd(selected)
            best.move(r, elemCluster)
            cost
        }

        // Consider each alternate cluster for the data point.  Update the
        // stocastic cost while also tracking the move with the lowest cost.
        for (c <- 0 until stocasticCost.columns) {
            // Decay the cost by some fraction.
            val decayedCost = beta * stocasticCost.get(r,c)
            // Compute the cost of moving the point to the new cluster (or just
            // leaving it where it is).
            val moveCost =
                if (c == elemCluster) fixedCost
                else move(c)
            // Update the new cost.
            val newCost = decayedCost + moveCost
            stocasticCost.set(r,c, newCost)
            // Determine if the new cost is lower than the best observed move
            // and update if so.
            if (newCost <= bestCost) {
                bestCost = newCost
                bestMove = (r, c)
            }
        }
    }
    bestMove
}

object Partition {
    /**
     * Create a new Partition from a cluster assignment file.
     */
    def apply(clusterSolution: String) = {
        val lines = Source.fromFile(clusterSolution).getLines
        val Array(n, c) = lines.next.split("\\s+").map(_.toInt)
        val assignments = new Array[Int](n)
        val clusters = (0 until c).toArray.map(_ => new HashSet[Int]())
        for ((line, ci) <- lines zipWithIndex;
             elem <- line.split("\\s+").map(_.toInt)) {
            assignments(elem) = ci
            clusters(ci) += (elem)
        }
        new Partition(clusters, assignments)
    }

    def apply(other: Partition) =
        new Partition(other.clusters.map(_.clone),
                      other.assignments.clone)
}

class Partition(val clusters: Array[HashSet[Int]], val assignments: Array[Int]) {

    /**
     * Returns the value of n choose 2.
     */
    def chooseTwo(n : Int) = n * (n - 1) / 2

    /**
     * Returns the number of pairs co-clusters in this partition.
     */
    def numPairs = clusters.foldLeft(0)( (s,c) => s + chooseTwo(c.size))

    /**
     * Moves an element to a new cluster.  Returns true if the point was moved
     * to a new cluster and false if the point was not moved at all.
     */
    def move(element: Int, newCluster:Int): Boolean = {
        val oldCluster = assignments(element)
        if (oldCluster == newCluster) return false
        assignments(element) = newCluster
        clusters(oldCluster).remove(element)
        clusters(newCluster).add(element)
        true
    }

    def sdd(sp: Partition):Int = {
        if (assignments.size != sp.assignments.size)
            return 0

        val b = numPairs
        val c = sp.numPairs

        def ordering(x:Int, y:Int) = 
            if (assignments(x) == assignments(y))
                sp.assignments(x) < sp.assignments(y)
            else
                assignments(x) < assignments(y)

        val orderedOverlap = (0 until assignments.size).toList.sortWith(ordering)

        var a = 0
        var count = 1
        for (i <- 1 until assignments.size) {
            val prev = orderedOverlap(i-1)
            val curr = orderedOverlap(i)
            if (assignments(prev) == assignments(curr) &&
                sp.assignments(prev) == sp.assignments(curr)) {
                count += 1
            } else {
                a += chooseTwo(count)
                count = 1
            }
        }

        return b + c - 2 * a
    }

    override def toString =
        "%d %d\n".format(assignments.size, clusters.size) +
        clusters.map(_.mkString(" ")).mkString("\n")
}
