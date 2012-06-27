package edu.ucla.sspace.experiment

import scala.io.Source

object SelectTopWords {
    def main(args: Array[String]) {
        val stopWords = Source.fromFile(args(0)).getLines.toSet
        val numregex = "[-+]?[0-9,.-]+"
        def accept(entry: (String, Int)) = !stopWords.contains(entry._1) && !entry._1.matches(numregex)
        def toTuple(parts: Array[String]) = (parts(1), parts(0).toInt)

        val numTopWords = args(1).toInt
        Source.fromFile(args(2))
              .getLines
              .map(_.trim)
              .map(_.split("\\s+"))
              .map(toTuple)
              .filter(accept)
              .map(_.swap)
              .toList
              .sorted
              .takeRight(numTopWords)
              .map(_._2)
              .foreach(println)
    }
}
