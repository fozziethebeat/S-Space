import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.sim.CosineSimilarity
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath

import scala.io.Source

// Read in the feature representation of the contexts.
val contexts = MatrixIO.readMatrix(args(2), Format.SVDLIBC_SPARSE_TEXT)

// Read in the clustering solutions and form the cluster centers using the rows
// from the training context matrix.
val trainContexts = MatrixIO.readMatrix(args(0), Format.SVDLIBC_SPARSE_TEXT)
val solutionFile = Source.fromFile(args(1)).getLines
val Array(numPoints, numClusters) = solutionFile.next.split("\\s+").map(_.toInt)
val clusters = Array.fill(numClusters)(new CompactSparseVector(contexts.columns))
for ( (clusterLine, id) <- solutionFile.zipWithIndex; 
      if clusterLine != "";
      point <- clusterLine.split("\\s+") )
    VectorMath.add(clusters(id), trainContexts.getRowVector(point.toInt))

// Read in the headers.  These must be in the same order as the rows in
// contexts.
val headers = Source.fromFile(args(3)).getLines.toList
// Get the main term for this evaluation.
val term = headers(0).replaceAll(".[0-9]+", "")

// Iterate through each row in the full context matrix and label it with the
// cluster that has the highest similarity.
val sim = new CosineSimilarity()
for ( (r, header) <- (0 until contexts.rows) zip headers ) {
    val v = contexts.getRowVector(r)
    val label = clusters.zipWithIndex.map( x => (sim.sim(x._1, v), x._2)).max._2
    printf("%s %s %s.%d\n", term, header, term, label)
}
