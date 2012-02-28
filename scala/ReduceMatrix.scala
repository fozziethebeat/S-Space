import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.factorization.NonNegativeMatrixFactorizationMultiplicative
import edu.ucla.sspace.matrix.factorization.SingularValueDecompositionLibC


/**
 * Reduces a matrix using either NMF or SVD.
 */
object ReduceMatrix {
    def main(args:Array[String]) {
        val reduction = args(2) match {
            case "nmf" => new NonNegativeMatrixFactorizationMultiplicative();
            case "svd" => new SingularValueDecompositionLibC();
        }
        val numDim = args(3).toInt

        val matrixFile = new MatrixFile(
            args(0), Format.valueOf(args(1).toUpperCase))
        reduction.factorize(matrixFile, numDim)

        MatrixIO.writeMatrix(reduction.dataClasses, args(4), Format.DENSE_TEXT)
    }
}
