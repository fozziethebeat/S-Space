import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.mutable.ArrayBuffer

import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.dependency.FlatPathWeight
import edu.ucla.sspace.dependency.UniversalPathAcceptor
import edu.ucla.sspace.dv.RelationBasedBasisMapping
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.GeneratorMap
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.TernaryVector
import edu.ucla.sspace.vector.Vectors
import edu.ucla.sspace.wordsi.Wordsi
import edu.ucla.sspace.wordsi.OccurrenceDependencyContextGenerator
import edu.ucla.sspace.wordsi.OrderingDependencyContextGenerator
import edu.ucla.sspace.wordsi.PartOfSpeechDependencyContextGenerator
import edu.ucla.sspace.wordsi.WordOccrrenceDependencyContextGenerator
import edu.ucla.sspace.wordsi.semeval.SemEvalDependencyContextExtractor

import java.io.File
import java.util.HashSet

import scala.io.Source


object PrintWordsi extends Wordsi {

    def main(args:Array[String]) {
        val excludeSet = new HashSet[String]()
        Source.fromFile(args(2)).getLines.foreach { excludeSet.add(_) }

        val windowSize = args(1).toInt
        val basis = new FilteredStringBasisMapping(excludeSet)
        var depBasis:RelationBasedBasisMapping = null
        val generator = args(0) match {
            case "woc" => new OccurrenceDependencyContextGenerator(basis, windowSize)
            case "pos" => new PartOfSpeechDependencyContextGenerator(basis, windowSize) 
            case "ord" => new OrderingDependencyContextGenerator(basis, windowSize)
            case "dep" => {
                val acceptor = new UniversalPathAcceptor()
                val weightor = new FlatPathWeight()
                depBasis = new RelationBasedBasisMapping(excludeSet)
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
        SerializableUtil.save(depBasis, args(5))
    }

    val vectors = new ArrayBuffer[SparseDoubleVector]

    def acceptWord(word:String) = true

    def handleContextVector(primary: String,
                            secondary: String, 
                            vector: SparseDoubleVector) {
        vectors += vector
    }

    def printVectors(outFile: File, numDim: Int) {
        val matrix = Matrices.asMatrix(
            for (v <- vectors) yield Vectors.subview(v, 0, numDim) )
        MatrixIO.writeMatrix(matrix, outFile, Format.SVDLIBC_SPARSE_TEXT)
    }
}
