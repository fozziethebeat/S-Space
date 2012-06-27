package edu.ucla.sspace.experiment

import scala.io.Source

object ExtractWordSimKeyWords {
    def main(args: Array[String]) {
        val lines = Source.fromFile(args(0)).getLines
        // Throw away the first line, we know it to alway contain header
        // information.
        lines.next

        // Next filter any lines that start with # (representing a comment).
        // Then split the valid lines and reveal the key words and turn it into
        // a set of words and print each word to stdout.
        lines.filter(!_.startsWith("#"))
             .flatMap(_.split("\\s+").slice(0,2))
             .toSet
             .foreach(println)
    }
}
