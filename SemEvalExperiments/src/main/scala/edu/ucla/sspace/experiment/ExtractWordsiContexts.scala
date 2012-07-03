package edu.ucla.sspace.experiment 

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.dependency.FlatPathWeight
import edu.ucla.sspace.dependency.UniversalPathAcceptor
import edu.ucla.sspace.dv.RelationBasedBasisMapping
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.Vectors
import edu.ucla.sspace.vector.VectorIO
import edu.ucla.sspace.wordsi.Wordsi
import edu.ucla.sspace.wordsi.OccurrenceDependencyContextGenerator
import edu.ucla.sspace.wordsi.OrderingDependencyContextGenerator
import edu.ucla.sspace.wordsi.PartOfSpeechDependencyContextGenerator
import edu.ucla.sspace.wordsi.WordOccrrenceDependencyContextGenerator
import edu.ucla.sspace.wordsi.semeval.SemEvalDependencyContextExtractor

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet

import java.io.File
import java.util.HashSet

import scala.io.Source


object ExtractWordsiContexts extends Wordsi {

    def main(vargs:Array[String]) {
        val (args, loadBasis) = if (vargs(0) == "-l") (vargs.slice(1, vargs.size), true) 
                                else (vargs, false)

        var basis:BasisMapping[String, String] = null
        var depBasis:RelationBasedBasisMapping = null
        if (loadBasis) {
            if (args(0) == "dep") {
                depBasis = SerializableUtil.load(args(5))
                depBasis.setReadOnly(true)
            }
            else {
                basis = SerializableUtil.load(args(5))
                basis.setReadOnly(true)
            }
        } else {
            val excludeSet = new HashSet[String]()
            Source.fromFile(args(2)).getLines.foreach { excludeSet.add(_) }
            if (args(0) == "dep")
                depBasis = new RelationBasedBasisMapping(excludeSet)
            else
                basis = new FilteredStringBasisMapping(excludeSet)
        }

        val windowSize = args(1).toInt
        val generator = args(0) match {
            case "woc" => new OccurrenceDependencyContextGenerator(basis, windowSize)
            case "pos" => new PartOfSpeechDependencyContextGenerator(basis, windowSize) 
            case "ord" => new OrderingDependencyContextGenerator(basis, windowSize)
            case "dep" => {
                val acceptor = new UniversalPathAcceptor()
                val weightor = new FlatPathWeight()
                new WordOccrrenceDependencyContextGenerator(
                    depBasis, weightor, acceptor, windowSize)
            }
        }

        val parser = new CoNLLDependencyExtractor()
        val extractor = new SemEvalDependencyContextExtractor(parser, generator)

        println("Processing Documents")
        for (document <- new DependencyFileDocumentIterator(args(3)))
            extractor.processDocument(document.reader, this)

        println("Printing vectors")
        printVectors(new File(args(4)), generator.getVectorLength)

        println("Saving basis mapping")
        if (!loadBasis) {
            args(0) match {
                case "dep" => SerializableUtil.save(depBasis, args(5))
                case _ => SerializableUtil.save(basis, args(5))
            }
        }
    }

    var vectors = List[SparseDoubleVector]()

    def acceptWord(word:String) = true

    def handleContextVector(primary: String,
                            secondary: String, 
                            vector: SparseDoubleVector) {
        vectors :+= vector
    }

    def printVectors(outFile: File, numDim: Int) {
        val sizedData = vectors.map(Vectors.subview(_, 0, numDim))
        VectorIO.writeVectors(sizedData, outFile)
    }
}
