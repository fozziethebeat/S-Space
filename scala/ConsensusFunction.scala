import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering
import edu.ucla.sspace.clustering.NeighborChainAgglomerativeClustering.ClusterLink
import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.SymmetricMatrix
import edu.ucla.sspace.matrix.SymmetricIntMatrix

import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.iterableAsScalaIterable
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

System.err.println("reporter:status:Finding BOEM")
val best = mergeType match {
    case "fsboem" => filteredBoem(partitions, numClusters)
    case "boem" => boem(partitions, numClusters)
    case "agglo" => agglo(partitions, numClusters)
    case "bok" => bestOfK(partitions, numClusters)
}
System.err.println("reporter:status:Done")
val writer = new PrintWriter(outFile)
writer.println(best)
writer.close
    
def addPairsToMatrix(points: Set[Int], matrix: Matrix) {
    for ( Array(x,y) <- points.subsets(2).map(_.toArray))
        matrix.add(x,y,1)
}

def agglo(partitions:Array[Partition], numClusters:Int) = {
    System.err.println("reporter:status:Agglomerative Merge")
    val rows = partitions(0).assignments.size
    val adjacency = new SymmetricMatrix(rows, rows)
    val indicators = new SymmetricMatrix(rows, rows)
    for (partition <- partitions) {
        addPairsToMatrix(partition.clusters.reduce(_++_).toSet, indicators)
        for ( cluster <- partition.clusters )
            addPairsToMatrix(cluster.toSet, adjacency)
    }
    for (r <- 0 until rows; c <- (r+1) until rows)
        adjacency.set(r,c, adjacency.get(r,c) / indicators.get(r,c))

    val sol = NeighborChainAgglomerativeClustering.clusterAdjacencyMatrix(
            adjacency, ClusterLink.MEAN_LINK, numClusters)
    Partition(sol)
}

def bestOfK(partitions:Array[Partition], numClusters:Int) = {
    System.err.println("reporter:status:Best of K")
    var bestPartition = partitions(0)
    var bestScore = partitions.map(bestPartition.sdd(_)).sum
    for (p1 <- partitions.slice(1, partitions.size)) {
        val sddSum = partitions.map(p1.sdd(_)).sum
        if (sddSum < bestScore) {
            bestScore = sddSum
            bestPartition = p1
        }
    }
    bestPartition
}

def printBest(best:Partition) {
    println("""alignl %pi^"*" = \{ \{""")
    println(best.clusters.map(_.mkString(", ")).mkString("""\} newline alignl phantom {%pi^"*" = \{}  \{"""))
    println("""\}""")
}

def boem(partitions:Array[Partition], numClusters:Int) : Partition = {
    //var best = Partition(bestOfK(partitions, numClusters))
    var best = Partition(agglo(partitions, numClusters))
    val numPoints = best.assignments.size
    val numPartitions = partitions.size

    System.err.println("reporter:status:Best One Element Move")

    // Compute the cost of co-clustered points.  
    val coClusterCost = new SymmetricIntMatrix(numPoints, numPoints)
    for (r <- 0 until numPoints; c <- r+1 until numPoints) {
        coClusterCost.set(r,c, numPartitions)
        for ( partition <- partitions; if partition.coClustered(r, c))
            coClusterCost.add(r, c, -2)
    }

    // Create the array that tracks the best one element move for each data
    // point.  The tuple stores 1) the cost and 2) the move.
    val bestMoves = Array.fill(numPoints)((0.0, 0))

    // Compute the cost of moving each data point to a new cluster.
    val moveCost = new ArrayMatrix(numPoints, numClusters)
    for (r <- 0 until numPoints) {
        var bestMove = (Double.MaxValue, 0)
        for (c <- 0 until numClusters) {
            val cost = best.clusters(c).filter(_!=r).map(coClusterCost.get(r,_)).sum
            if (cost < bestMove._1)
                bestMove = (cost, c)
            moveCost.set(r, c, cost)
        }
        bestMoves(r) = bestMove
    }

    def getDelta(i: Int) =
        (moveCost.get(i,best.assignments(i))-bestMoves(i)._1, i)

    for (i <- 0 until 200) {
        // Get the best one element move by finding the element with the largest
        // difference from it's current cluster to it's best next cluster.
        var bestMove = getDelta(0)
        for (p <- 1 until numPoints) {
            val newMove = getDelta(p)
            if (newMove._1 > bestMove._1)
                bestMove = newMove
        }

        // Extract the difference and the point for convenience.
        val (delta, point) = bestMove

        // Get the cluster id's for the previous cluster and the new cluster for
        // the point.
        val oldCluster = best.assignments(point)
        val newCluster = bestMoves(point)._2

        // Check that this move makes a difference.  If it doesn't, we can exit
        // early.
        if (delta <= 0 || oldCluster == newCluster)
            return best

        // For each element, modify the cost of the move and update the best
        // move costs.
        for (r <- 0 until numPoints) {
            // Update the cost for the old cluster.
            moveCost.add(r, oldCluster, -coClusterCost.get(r, point))
            // Update the cost for the new cluster.
            moveCost.add(r, newCluster, coClusterCost.get(r, point))

            // Update the best move cost for this element if needed.
            if (moveCost.get(r, oldCluster) < bestMoves(r)._1)
                bestMoves(r) = (moveCost.get(r, oldCluster), oldCluster)
            if (moveCost.get(r, newCluster) < bestMoves(r)._1)
                bestMoves(r) = (moveCost.get(r, newCluster), newCluster)
        }
        
        // Finally, make the move for this data point to it's new cluster.
        best.move(point, newCluster)
    }
    best
}

def totalCost(partition: Partition, partitions: Array[Partition]) =
    partitions.par.map(partition.sdd(_)).sum

def filteredBoem(partitions:Array[Partition], numClusters:Int) = {
    var best = Partition(bestOfK(partitions, numClusters))
    System.err.println("reporter:status:Filtered Stochastic Best One Element Move")
    val beta = .99
    val numIterations = 75
    val numPoints = best.assignments.size
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
    def apply(solution: Assignments) = {
        val assignments = new Array[Int](solution.size)
        for (p <- 0 until solution.size)
            assignments(p) = solution.get(p)
        val clusters = solution.clusters.map( c => {
                val s = new HashSet[Int]()
                for (p <- c) s.add(p)
                s
        }).toArray
        new Partition(clusters, assignments)
    }

    /**
     * Create a new Partition from a cluster assignment file.
     */
    def apply(clusterSolution: String) = {
        val lines = Source.fromFile(clusterSolution).getLines
        val Array(n, c) = lines.next.split("\\s+").map(_.toInt)
        val assignments = new Array[Int](n)
        val clusters = Array.fill(c)(new HashSet[Int]())
        for ((line, ci) <- lines.filter(_!= "").zipWithIndex; 
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
     * Returns true if two data points are assigned to the same cluster.
     */
    def coClustered(i: Int, j:Int) = assignments(i) == assignments(j)

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

        val orderedOverlap = 
            (0 until assignments.size).toList.sortWith(ordering)

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
        a+= chooseTwo(count)

        return b + c - 2 * a
    }

    override def toString =
        "%d %d\n".format(assignments.size, clusters.size) +
        clusters.filter(_.size > 0).map(_.mkString(" ")).mkString("\n")
}
