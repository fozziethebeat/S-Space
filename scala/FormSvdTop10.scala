
import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.SerializableUtil

import scala.math._

import java.io.File


val basis:BasisMapping[String, String] = SerializableUtil.load(args(0))
val ws = MatrixIO.readMatrix(new File(args(1)), Format.DENSE_TEXT)

for ( c <- 0 until ws.columns) {
    val ordering = ((0 until ws.rows).map { r =>
        (ws.get(r,c), r) } ).sorted.reverse.map(_._2).take(10)
        //(pow(ws.get(r,c), 2), r) } ).sorted.reverse.map(_._2).take(10)
    println( ordering.map(basis.getDimensionDescription(_)).mkString(" "))
}
