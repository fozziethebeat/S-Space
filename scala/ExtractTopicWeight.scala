import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format

import java.io.PrintWriter


object ExtractTopicWeight {
    def main(args:Array[String]) {
        val matrix = MatrixIO.readMatrix(args(0), Format.DENSE_TEXT)
        val weights = (0 until matrix.columns) map { c =>
            ((0 until matrix.rows) map { r => matrix.get(r,c) }).sum
        }
        val total = weights.sum
        val writer = new PrintWriter(args(1))
        for (weight <- weights)
            writer.println("%f".format(weight / total))
        writer.close
    }
}
