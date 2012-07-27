package edu.ucla.sspace.experiment

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.common.Similarity.jaccardIndex
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.CompactSparseVector

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.mutable.HashSet
import scala.io.Source

import java.io.File
import java.io.PrintWriter


// Arguments:
// (1) solution file
// (2) result file 
// (3) basis file
// (4) context file

/**
 * Extracts the cluster labels for each data point using the distributed graph
 * representation.
 */
object ConvertGraphSolution {
    def main(args: Array[String]) {
        // Read in the cluster solution and create a vector recording which words were
        // assigned to each cluster.
        val partition = Partition.read(args(0))
        val numPoints = partition.numPoints
        val numClusters = partition.numClusters
        val clusters = Array.fill(numClusters)(new CompactSparseVector(numPoints))
        for ( (group, cid) <- partition.clusters.zipWithIndex; point <- group )
            clusters(cid).add(point, 1.0)

        // Read in the basis mapping and set it to read only.
        val basis:BasisMapping[String, String] = SerializableUtil.load(args(2))
        basis.setReadOnly(true)

        // Iterate through each document in the corpus and determine which cluster has
        // the highset jaccard similarity to it.
        val finalClusters = Array.fill(numClusters)(new HashSet[Int]())
        for ( (id, v) <- readContextMatrix(args(3), basis)) {
            val label = clusters.zipWithIndex.map(x=>(jaccardIndex(x._1, v), x._2)).max._2
            finalClusters(label).add(id)
        }

        val numInstances = finalClusters.map(_.size).sum
        // Write out the cluster assignments in terms of the contexts.
        val writer = new PrintWriter(args(1))
        writer.println("%d %d".format(numInstances, numClusters))
        for ( cluster <- finalClusters )
            writer.println(cluster.mkString(" "))
        writer.close
    }

    def readContextMatrix(contextFile: String, basis:BasisMapping[String, String]) = {
        val docIter = new DependencyFileDocumentIterator(contextFile)
        for ( (doc, id) <- docIter.zipWithIndex) yield {
            val reader = doc.reader
            reader.readLine
            val rowVector = new CompactSparseVector()
            var line = reader.readLine
            while (line != null) {
                val d = basis.getDimension(line.split("\\s+")(1))
                if (d >= 0)
                    rowVector.set(d, 1)
                line = reader.readLine
            }
            (id, rowVector)
        }
    }
}
