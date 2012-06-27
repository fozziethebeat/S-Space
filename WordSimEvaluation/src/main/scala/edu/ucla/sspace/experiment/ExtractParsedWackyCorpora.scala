package edu.ucla.sspace.experiment

import edu.ucla.sspace.text.corpora.PukWacCorpusReader

import scala.collection.JavaConversions.asScalaIterator

import java.io.File


object ExtractParsedWackyCopora {
    def main(args: Array[String]) {
        // Read each document as parsed by the corpus reader and print out  the
        // text.
        val reader = new PukWacCorpusReader()
        for (doc <- reader.read(new File(args(0))) )
            println(doc.reader.readLine.toLowerCase)
    }
}
