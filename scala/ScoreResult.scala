import scala.io.Source
import scala.math._

object ScoreScala {
    def main(args:Array[String]) {
        val goldScores = (Source.fromFile(args(0)).getLines.map { line => 
            val Array(word, count) = line.trim.split("\\s+")
            (word, count.toDouble)
        }).toMap
        val mse = (Source.fromFile(args(1)).getLines.map { line =>
            val Array(word, count) = line.split("\\s+")
            pow(count.toDouble- goldScores.getOrElse(word, 0.0), 2)
        }).sum/goldScores.size
        println(mse)
    }
}
