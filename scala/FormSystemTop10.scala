import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.mutable.HashMap
import scala.io.Source

import java.io.File
import java.io.PrintWriter

// Read in the data matrix.
val dataMatrix = MatrixIO.readSparseMatrix(args(0), Format.SVDLIBC_SPARSE_TEXT)

// Get the number of clusters and create a centroid for each one.
val k = args(1).toInt
val clusters = Array.fill(k)(new CompactSparseVector(dataMatrix.columns))

// Load the basis mapping.
val basis:BasisMapping[String, String] = SerializableUtil.load(args(2))

// Reader in each header for this word and build up the centroid vectors using
// only the assignments corresponing to items in the headers.
val indexer = new HashMap[String, Int] with Indexer[String]
for ((line, id) <- Source.fromFile(args(3)).getLines.zipWithIndex) {
    val cluster = indexer.getId(line)
    printf("%s %d\n", line, cluster)
    VectorMath.add(clusters(cluster), dataMatrix.getRowVector(id))
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

MatrixIO.writeMatrix(Matrices.asSparseMatrix(clusters.toList),
                     new File(args(5)),
                     Format.SVDLIBC_SPARSE_TEXT)

trait Indexer[T] extends HashMap[T, Int] {
    def getId(key:T):Int = {
        get(key) match {
            case Some(v:Int) => v
            case None => val v:Int = size
                         put(key, v)
                         v
        }
    }
}
