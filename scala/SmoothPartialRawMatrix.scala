import edu.ucla.sspace.matrix.CellMaskedSparseMatrix
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix
import edu.ucla.sspace.matrix.LogEntropyTransform
import edu.ucla.sspace.matrix.Transform


object SmoothPartialRawMatrix {

    def main(args:Array[String]) {
        val baseMatrix = MatrixIO.readSparseMatrix(args(0), Format.SVDLIBC_SPARSE_BINARY)
        val skip = args(1).toInt
        val rowMask = 0 until baseMatrix.rows toArray
        val colMask = skip until baseMatrix.columns toArray
        val matrix = new CellMaskedSparseMatrix(baseMatrix, rowMask, colMask)
        val transformer = new LogEntropyTransform()
        val transformed = transformer.transform(matrix)
        MatrixIO.writeMatrix(transformed, args(2), Format.SVDLIBC_SPARSE_BINARY)
        MatrixIO.writeMatrix(transformed, args(3), Format.MATLAB_SPARSE)
    }
}
