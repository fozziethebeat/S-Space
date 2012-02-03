import edu.ucla.sspace.common.ArgOptions
import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage
import edu.ucla.sspace.matrix.CellMaskedMatrix
import edu.ucla.sspace.matrix.CellMaskedSparseMatrix
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
//import edu.ucla.sspace.matrix.ScalarMatrix
import edu.ucla.sspace.matrix.SymmetricIntMatrix
import edu.ucla.sspace.util.Counter
import edu.ucla.sspace.util.ReflectionUtil

import java.io.File
import java.io.PrintWriter

import scala.collection.mutable.HashSet
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
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
    def rowSubSample(matrix: Matrix, indicators: Matrix, 
                     fraction: Double, useGraphs: Boolean) = {
        val indices = 0 until matrix.rows
        val dims = (fraction * matrix.rows).toInt
        val rowMap:Array[Int] = (Random.shuffle(indices) take dims) toArray
        val columnMap:Array[Int] = if (useGraphs) rowMap 
                                   else 0 until matrix.columns toArray

        for (Array(r1, r2) <- rowMap.combinations(2))
            indicators.set(r1, r2, indicators.get(r1, r2) + 1)
        matrix match {
            case sm:SparseMatrix =>
                (new CellMaskedSparseMatrix(sm, rowMap, columnMap), rowMap)
            case _ => 
                (new CellMaskedMatrix(matrix, rowMap, columnMap), rowMap)
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
    def findClusters(assignments: Assignments, rowMap: Array[Int]) = {
        val clusters = 0 until assignments.numClusters map {
            _ => HashSet[Int]() }
        for ((assignment, i) <- assignments.assignments zipWithIndex)
            clusters(assignment(0)).add(
              if (rowMap == null) i else rowMap(i))
        clusters map { cluster => cluster toArray }
    }

    def consensus(matrix: Matrix, indicators:Matrix, row: Int, col: Int) = {
        val indicator = indicators.get(row, col)
        if (indicator == 0d) 0d else matrix.get(row, col) / indicator
    }

    def printConsensusMatrix(matrix: Matrix, indicators: Matrix,
                             k: Int, fileName:String) {
        val writer = new PrintWriter("%s.cm%02d".format(fileName, k))
        writer.print("%d %d\n".format(matrix.rows, matrix.columns))
        for (row <- 0 until matrix.rows) {
            for (col <- 0 until row) {
                val score = consensus(matrix, indicators, row, col)
                writer.print("%f ".format(score))
            }
            writer.println
        }
        writer.close
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

    def printSingleCluster(numRows: Int, outputBase: String) {
        val rca = new PrintWriter("%s.rca01".format(outputBase))
        val cca = new PrintWriter("%s.cca01".format(outputBase))

        rca.println("%d %d".format(numRows, 1))
        cca.println("%d %d".format(numRows, 1))

        rca.println((0 until numRows).mkString(" "))
        cca.println((0 until numRows).mkString(" "))

        rca.close
        cca.close

        val cm = new PrintWriter("%s.cm01".format(outputBase))
        cm.print("%d %d\n".format(numRows, numRows))
        for (row <- 0 until numRows)
            cm.println((0 until row).mkString(" "))
        cm.close
    }

    def main(args:Array[String]) {
        val options = new ArgOptions()
        options.addOption('g', "graphCluster",
                          "If true, ConsensusClustering will assume a graph " +
                          "is being clustered",
                          false, null, "Optional")
        options.addOption('p', "numPartitions",
                          "This overrides the number of partitions each " +
                          "ensemble tries to find.  By defaul each ensemble " +
                          "finds the same number of partitions as clusters",
                          true, "INT", "Optional")
        val vargs = options.parseOptions(args)

        if (vargs.length != 6) {
            println("usage: ConsensusCluster [OPTIONS] <ClusterAlg> <InputMatrix> " +
                    "<K> <numSamples> <samplePercent> <datFileBase>\n" +
                    options.prettyPrint())
            return
        }

        println("Creating the clustering method")
        val useGraphs = options.hasOption('g')
        // Create the clustering algorithm.
        val clustering:Clustering = ReflectionUtil.getObjectInstance(vargs(0))
        // Get the properties object for clustering.
        val props = System.getProperties

        // Get the maximum value of K and the number of samples desired.
        val k = vargs(2).toInt
        val numPartitions = options.getIntOption('p', k)
        val numSamples = vargs(3).toInt
        val dimFraction = vargs(4).toDouble

        println("Reading the data matrix")
        // Read in the data matrix.
        val dataset = if (vargs(1).contains("nmf") ||
                          vargs(1).contains("lda") ||
                          vargs(1).contains("svd"))
                MatrixIO.readMatrix(new File(vargs(1)), Format.DENSE_TEXT)
            else 
                MatrixIO.readSparseMatrix(vargs(1), Format.SVDLIBC_SPARSE_TEXT)

        val numPairs = dataset.rows * (dataset.rows -1) / 2.0
        val sampleError = sqrt(1 + 1.0 / numSamples)

        if (k == 1) {
            printSingleCluster(dataset.rows, vargs(5))
            return
        }

        System.err.println("reporter:status:Reference Clustering")
        System.err.println("reporter:counter:reference,%d,1".format(k))
        val assignments = clustering.cluster(dataset, k, props)
        printAssignments(assignments, dataset.rows,
                         "%s.rca%02d".format(vargs(5), k))
            
        println("Computing the consensus clusters")

        // Create a matrix that counts the number of times data points are
        // assigned to the same cluster.  We ignore the indicator matrix
        // since our subsampling method uses all data points, and instead
        // discards features.
        val rows = dataset.rows
        val adjacency = Matrices.synchronizedMatrix(
            new SymmetricIntMatrix(rows, rows))
        val indicators = Matrices.synchronizedMatrix(
            new SymmetricIntMatrix(rows, rows))
        //val indicators = new ScalarMatrix(rows, rows, numSamples)

        System.err.println("reporter:status:Consensus Clustering")
        // Take a subsample of the dataset and cluster it.  For the
        // resulting clusters, increment the adjacency count each time two
        // data points are in the same cluster.  Note the magic of ParRange,
        // this ParallelSeq takes a standard Range and runs map on each
        // element in parallel, with the map calls potentially being out of
        // order but the returned value being in order.  It then waits
        // until all map calls are finished before returning. MAGIC!
        (1 to numSamples).par foreach { h =>
            var failed = true
            while (failed) {
            val (sampledMatrix, rowMap) = rowSubSample(
                dataset, indicators, dimFraction, useGraphs)
            printf("Running consensus clustering k: %d, h: %d\n", k, h)
            try {
            val assignments = clustering.cluster(
                sampledMatrix, numPartitions, props)
            for (points <- findClusters(assignments, rowMap);
                 Array(x, y) <- points.combinations(2))
                adjacency.add(x,y,1)
            failed = false
            } catch {
                case _ => failed = true
            }
            }
            System.err.println("reporter:counter:consensus,%d,1".format(k))
        }

        printConsensusMatrix(adjacency, indicators, k, vargs(5))

        System.err.println("reporter:status:final hac")
        val hacSol = HierarchicalAgglomerativeClustering.toAssignments(
            HierarchicalAgglomerativeClustering.clusterSimilarityMatrix(
                adjacency, -1, ClusterLinkage.MEAN_LINKAGE, k),
            null, k)
        printAssignments(hacSol, adjacency.rows, 
                         "%s.cca%02d".format(vargs(5), k))

        println("Done!")
    }
}
