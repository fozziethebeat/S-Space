import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath

import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Source

import java.io.File
import java.io.PrintWriter

// Read in the data matrix.
val dataMatrix = MatrixIO.readSparseMatrix(args(0), Format.SVDLIBC_SPARSE_TEXT)

// Load the basis mapping.
val basis:BasisMapping[String, String] = SerializableUtil.load(args(1))

// Reader in the clustering assignments for the given solution and add each row
// to the assigned cluster.
val assignments = Source.fromFile(args(2)).getLines

// Get the number of clusters and create a centroid for each one.  The header
// for the assignments details the number of points and the number of clusters.
val Array(numPoints, k) = assignments.next.split("\\s").map(_.toInt)
val clusters = Array.fill(k)(new CompactSparseVector(dataMatrix.columns))

// Read each header, get the cluster number, and add the vector to the cluster.
for ((line, cluster) <- assignments.zipWithIndex; id <- line.split("\\s+"))
    VectorMath.add(clusters(cluster), dataMatrix.getRowVector(id.toInt))

// Extract the 10 most frequently occurring features for each cluster and print
// out their dimension descriptions.
val writer = new PrintWriter(args(3))
for (cluster <- clusters) {
    val top10 = (cluster.getNonZeroIndices.map { i =>
        (cluster.get(i), i)}).sorted.map(x=>basis.getDimensionDescription(x._2)).reverse.slice(0, 10)
    writer.println(top10.mkString(" "))
}
writer.close

MatrixIO.writeMatrix(Matrices.asSparseMatrix(clusters.toList),
                     args(4), Format.SVDLIBC_SPARSE_TEXT)
