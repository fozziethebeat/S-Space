import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.vector.SparseDoubleVector

import scala.collection.JavaConversions.bufferAsJavaList
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import java.io.PrintWriter


val fullData = MatrixIO.readSparseMatrix(args(0), Format.SVDLIBC_SPARSE_TEXT)
val allHeaders = Source.fromFile(args(1)).getLines.toList
val testHeaders = Source.fromFile(args(2)).getLines.map(_.split("\\s+")(1)).toSet

val testVectors = new ArrayBuffer[SparseDoubleVector]()
val orderedTestHeaders = new PrintWriter(args(3))
val trainVectors = new ArrayBuffer[SparseDoubleVector]()

for (r <- 0 until fullData.rows) {
    if (testHeaders.contains(allHeaders(r))) {
        orderedTestHeaders.println(allHeaders(r))
        testVectors.add(fullData.getRowVector(r))
    } else {
        trainVectors.add(fullData.getRowVector(r))
    }
}
orderedTestHeaders.close

MatrixIO.writeMatrix(Matrices.asSparseMatrix(testVectors),
                     args(4), Format.SVDLIBC_SPARSE_TEXT)
MatrixIO.writeMatrix(Matrices.asSparseMatrix(trainVectors),
                     args(5), Format.SVDLIBC_SPARSE_TEXT)
