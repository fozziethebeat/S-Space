import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.matrix.SparseMatrix

import scala.Math._


object ProjectDocuments {
    def main(args: Array[String]) {
        val fullDocSpace = MatrixIO.readMatrix(args(0), Format.DENSE_TEXT)
        val maskedDocSpace = new ArrayMatrix(fullDocSpace.rows, fullDocSpace.columns)
        val retainSize = (args(1).toDouble * fullDocSpace.columns).toInt
        val normType = args(3)
        for (r <- 0 until fullDocSpace.rows) {
            val retained = (0 until fullDocSpace.columns map { c =>
                val value = normType match {
                    case "n" => fullDocSpace.get(r,c)
                    case "s" => pow(fullDocSpace.get(r,c), 2)
                    case "a" => abs(fullDocSpace.get(r,c))
                    case "p" => fullDocSpace.get(r,c) + 1
                }
                (value, c) 
            }).sorted.reverse.slice(0, retainSize)
            val total = retained.foldLeft(0.0)( (s,r) => s + r._1)
            for ( (score, column) <- retained)
                maskedDocSpace.set(r, column, score / total)
        }
        MatrixIO.writeMatrix(maskedDocSpace, args(2), Format.DENSE_TEXT)
    }
}
