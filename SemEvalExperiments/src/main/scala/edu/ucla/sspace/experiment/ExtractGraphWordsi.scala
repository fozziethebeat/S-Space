package edu.ucla.sspace.experiment

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.wordsi.GraphWordsi

import scala.collection.JavaConversions.asScalaIterator
import scala.io.Source

import java.util.HashMap
import java.util.HashSet


object ExtractGraphWordsi {
    def main(args:Array[String]) {
        val excludeSet = new HashSet[String]();
        Source.fromFile(args(0)).getLines.foreach(l => excludeSet.add(l.trim))

        val referenceLikilhood = new HashMap[String, java.lang.Double]()
        Source.fromFile(args(1), "ISO-8859-1").getLines.foreach(l => {
            val parts = l.split("\\s+")
            if (parts.size == 2)
                referenceLikilhood.put(parts(0), parts(1).toDouble)
        })

        val basis:BasisMapping[String, String] = new FilteredStringBasisMapping(excludeSet)

        val extractor = new CoNLLDependencyExtractor()
        val wordsi = new GraphWordsi(basis, extractor, referenceLikilhood, args(3))

        for (doc <- new DependencyFileDocumentIterator(args(2)))
            wordsi.processDocument(doc.reader)
        wordsi.processSpace(System.getProperties)
        println("Processing complete")
    }
}
