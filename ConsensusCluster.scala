import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.matrix.SymmetricIntMatrix
import edu.ucla.sspace.util.Counter
import edu.ucla.sspace.util.ReflectionUtil

import java.io.File
import java.io.PrintWriter

import scala.collection.mutable.HashSet
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.parallel.immutable.ParRange
import scala.math._

import util.Random


object ConsensusCluster {

    /**
     * Returns a masked matrix that uses only some subset of the feature space.
     * @param matrix The matrix of data points that should be sampled
     * @param fraction The fraction of dimensions that should be sampled
     *
     * @return A {@link SparseMatrix} with all the data points from {@code
     *         matrix} but a randomly sampled subset of features from {@code
     *         matrix}, without replacement.
     */
    def subsample(matrix: Matrix, fraction: Double): SparseMatrix = {
        matrix match {
            case sm:SparseMatrix => {
                val indices = List.range(0, matrix.columns)
                val dims = (fraction * matrix.columns).toInt
                val columnMap:Array[Int] = (Random.shuffle(indices) take dims) toArray
                val rowMap:Array[Int] = 0 until matrix.rows toArray
                val m = new CellMaskedSparseMatrix(sm, rowMap, columnMap)
                m
            }
            case _ => throw new IllegalArgumentException(
                        "Cannot use dense matrix")
        }
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
    def findClusters(assignments: Assignments) = {
        val clusters = 0 until assignments.numClusters map {
            _ => HashSet[Int]() }
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment.assignments()(0)).add(i)
        clusters map { cluster => cluster toArray }
    }

    /**
     * Computes the summed pairwise distances between data points assigned to
     * the same cluster for all clusters.
     *
     * @param assignments The set of cluster assignments from a single run
     *
     * @return The summed distances between all intra-cluster points.
     */
    def computeGapObjective(assignments: Assignments) = {
        // Now compute the objective score for the cluster result.
        val centroids = assignments.getSparseCentroids
        val counts = assignments.clusterSizes
        val score = (centroids zip counts).foldLeft(0.0) (
            (sum, vs) => sum + vs._1.magnitude * vs._2)
        log(score)
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
        if (args.length != 6) {
            println("usage: ConsensusCluster <ClusterAlg> <InputMatrix> <maxK> <numSamples> <numDims> <datFileBase>")
            return
        }

        // Create the clustering algorithm.
        val clustering:Clustering = ReflectionUtil.getObjectInstance(args(0))
        // Get the properties object for clustering.
        val props = System.getProperties

        // Get the maximum value of K and the number of samples desired.
        val maxK = args(2).toInt
        val numSamples = args(3).toInt
        val dimFraction = args(4).toDouble
        val samples = numSamples.toDouble

        // Read in the data matrix.
        val dataset = MatrixIO.readMatrix(
            new File(args(1)), Format.SVDLIBC_SPARSE_TEXT)
        val numPairs = dataset.rows * (dataset.rows -1) / 2.0
        val sampleError = sqrt(1 + 1.0 / samples)

        val results = 2 to maxK map { k =>
            // Create a matrix that counts the number of times data points are
            // assigned to the same cluster.  We ignore the indicator matrix
            // since our subsampling method uses all data points, and instead
            // discards features.
            val adjacency = Matrices.synchronizedMatrix(
                new SymmetricIntMatrix(dataset.rows, dataset.rows))

            // Take a subsample of the dataset and cluster it.  For the
            // resulting clusters, increment the adjacency count each time two
            // data points are in the same cluster.  Note the magic of ParRange,
            // this ParallelSeq takes a standard Range and runs map on each
            // element in parallel, with the map calls potentially being out of
            // order but the returned value being in order.  It then waits
            // until all map calls are finished before returning. MAGIC!
            val gapScores = new ParRange(1 to numSamples) map { h =>
                printf("Running clustering k: %d, h: %d\n", k, h)
                val sampledMatrix = subsample(dataset, dimFraction)
                val assignments = clustering.cluster(sampledMatrix, k, props)
                for (points <- findClusters(assignments))
                    for ((x, i) <- points zipWithIndex)
                        for (y <- points.view(0, i))
                                adjacency.set(x, y, adjacency.get(x, y) + 1)

                // Compute and return the object score for this sampled run.
                computeGapObjective(assignments)
            }

            // Compute the clustering result for the full data set, un-modified,
            // and store just the computed objective score.
            val realGap = computeGapObjective(
                clustering.cluster(dataset, k, props))

            // Compute the expectation of the objective score for the sampled
            // runs.  Also compute the standard deviation of those runs.
            val sampleGapExpectation = gapScores.sum / numSamples
            val sampleGapStd = sampleError * sqrt( (gapScores map { 
                s => pow(s - sampleGapExpectation, 2) } sum) / numSamples )

            // Build the sorted list of scaled adjacency values.
            val scoreCounter = new Counter[Double]()
            for (x <- 0 until adjacency.rows; y <- 0 until x)
                scoreCounter.count(adjacency.get(x, y)/numSamples)

            /*
            val valueList = (for (x <- 0 until adjacency.rows; y <- 0 until x)
                               yield adjacency.get(x, y)/samples).sorted
                               */
            val valueList = (scoreCounter map { 
                entry => (entry.getKey, entry.getValue) }).toList.sorted

            // Compute the CDF value for each data point.  This requires
            // counting the number of tuples for which the second value is true
            // and storing that count for each new tuple generated.
            var sum = 0
            val cdf_k = valueList map { 
                ic => sum += ic._2; (ic._1, sum/numPairs) }

            // Now accumulate the differences between each pair of adjacency
            // count files, starting from the left, and multiply this distance
            // by the CDF, giving us the area under the curve.
            val a_k = (for (Seq((y1, _), (y2, cdf)) <- cdf_k.sliding(2))
                           yield (y2 - y1) * cdf) reduceLeft(_+_)

            // Finally, return the CDF results and the accumlation score for
            // this value of k.
            (cdf_k, a_k, sampleGapExpectation - realGap, sampleGapStd)
        }

        println("Printing CDF")
        // Now figure out how to plot everything.
        // First print out the values for the CDF
        val cdfWriter = new PrintWriter(args(5) + ".cdf.dat")
        cdfWriter.println("#k adjacencyCount CDF")
        for (((cdf, _, _, _), k) <- results zipWithIndex; (c, s) <- cdf)
            cdfWriter.println("%d %f %f".format(k+2, c, s))
        cdfWriter.close()

        println("Printing Gap score and CDF Delta")
        // Compute the differential of the area under the CDF curve.
        val deltaWriter = new PrintWriter(args(5) + ".delta.dat")
        val gapWriter = new PrintWriter(args(5) + ".gap.dat")
        deltaWriter.println("#k Delta(k)")
        gapWriter.println("#k gap gap-stdev")
        for (((_, a_k, gap, std), k) <- results zipWithIndex) {
            deltaWriter.println("%d %f".format(k+2, 
                if (k == 0) a_k else (a_k - results(k-1)._2) / a_k))
            gapWriter.println("%d %f %f".format(k+2, gap, gap-std))
        }
        deltaWriter.close
        gapWriter.close
        println("Done!")
    }
}
