import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.util.SerializableUtil

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Source

import java.io.PrintWriter
import java.util.HashSet


// Form the basis mapping by first creating a filtered bases of words to exclude
// during processing.
val excludeSet = new HashSet[String]()
Source.fromFile(args(0)).getLines.foreach { excludeSet.add(_) }
val basis = new FilteredStringBasisMapping(excludeSet)

// Reader in the clustering assignments for the given solution and add each row
// to the assigned cluster.
val assignmentFile = Source.fromFile(args(1)).getLines

// Get the number of clusters and create a centroid for each one.  The header
// for the assignments details the number of points and the number of clusters.
val Array(numPoints, k) = assignmentFile.next.split("\\s").map(_.toInt)
val clusters = Array.fill(k)(new CompactSparseVector())

// Create a mapping from each data point id to it's cluster assignment.
val assignments = new Array[Int](numPoints)
for ((line, cluster) <- assignmentFile.zipWithIndex; 
     if line != ""; id <- line.split("\\s+"))
    assignments(id.toInt) = cluster

// Create a parser for extracting dependency trees.
val parser = new CoNLLDependencyExtractor()
// Iterate through each dependency document.
val docIter = new DependencyFileDocumentIterator(args(2))
for ( (document, id) <- docIter.zipWithIndex ) {
    val reader = document.reader
    // read the header.
    val header = reader.readLine
    // Parse the document into a dependency tree
    val tree = parser.readNextTree(reader)
    // Get the centroid for the data point.
    val center = clusters(assignments(id))
    // Iterate for each word in the tree and add counts for those words to the
    // cluster vector.  Only ignore the focus word.
    for (node <- tree; if node.lemma != header) {
        val index = basis.getDimension(node.word)
        if (index >= 0)
            center.add(index, 1)
    }
}

// Extract the 10 most frequently occurring features for each cluster and print
// out their dimension descriptions.
val writer = new PrintWriter(args(3))
for (cluster <- clusters) {
    val top10 = (cluster.getNonZeroIndices.map { i =>
        (cluster.get(i), i)}).sorted.map(x=>basis.getDimensionDescription(x._2)).reverse.slice(0, 10)
    writer.println(top10.mkString(" "))
}
writer.close

MatrixIO.writeMatrix(Matrices.asSparseMatrix(clusters.toList, basis.numDimensions),
                     args(4), Format.SVDLIBC_SPARSE_TEXT)
SerializableUtil.save(basis, args(5))
