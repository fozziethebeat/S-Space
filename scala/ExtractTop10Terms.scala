import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.vector.CompactSparseVector

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

for (word <- Source.fromFile(args(1)).getLines)
    basis.getDimension(word)
basis.setReadOnly(true)

// Reader in the clustering assignments for the given solution and add each row
// to the assigned cluster.
val partition = Partition.read(args(2))
val clusters = Array.fill(partition.numClusters)(new CompactSparseVector())

// Create a parser for extracting dependency trees.
val parser = new CoNLLDependencyExtractor()
// Iterate through each dependency document.
val docIter = new DependencyFileDocumentIterator(args(3))
for ( (document, id) <- docIter.zipWithIndex ) {
    val reader = document.reader
    // read the header.
    val header = reader.readLine
    // Parse the document into a dependency tree
    val tree = parser.readNextTree(reader)
    // Get the centroid for the data point.
    val center = clusters(partition.assignments()(id))
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
val writer = new PrintWriter(args(4))
for (cluster <- clusters) {
    val top10 = (cluster.getNonZeroIndices.map { i =>
        (cluster.get(i), i)}).sorted.map(x=>basis.getDimensionDescription(x._2)).reverse.slice(0, 10)
    writer.println(top10.mkString(" "))
}
writer.close

MatrixIO.writeMatrix(Matrices.asSparseMatrix(clusters.toList, basis.numDimensions),
                     args(5), Format.SVDLIBC_SPARSE_TEXT)
