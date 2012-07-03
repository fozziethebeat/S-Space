package edu.ucla.sspace.experiment

import edu.ucla.sspace.text.DependencyFileDocumentIterator

import scala.collection.JavaConversions.asScalaIterator
import scala.io.Source


object ExtractTrainContexts {
    def main(args: Array[String]) {
        val testHeaders = Source.fromFile(args(1)).getLines
                                                  .map(_.split("\\s+")(1))
                                                  .toSet
        for (document <- new DependencyFileDocumentIterator(args(0))) {
            val reader = document.reader
            var line = reader.readLine
            if (testHeaders.contains(line)) {
                while (line != null) {
                    println(line)
                    line = reader.readLine
                }
                println
            }
        }
    }
}
