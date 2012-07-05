package edu.ucla.sspace.experiment

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.dependency.DependencyTreeNode
import edu.ucla.sspace.hal.EvenWeighting
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.Vectors
import edu.ucla.sspace.vector.VectorIO
import edu.ucla.sspace.wordsi.DependencyContextGenerator
import edu.ucla.sspace.wordsi.Wordsi
import edu.ucla.sspace.wordsi.WordOccrrenceContextGenerator
import edu.ucla.sspace.wordsi.semeval.SemEvalDependencyContextExtractor

import org.apertium.lttoolbox.process.FSTProcessor
import org.apertium.utils.IOUtils._

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.matching.Regex

import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.HashSet
import java.util.LinkedList


/**
 * Creates feature vectors for SemEval using morphological analysis prior to
 * computing word co-occurrence.  Some analysis features are discarded, namely
 * part of speech features.  Tense and quantity features are retained.
 *
 * </p>
 *
 * args:
 * <ol>
 *  <li>lttool-box morphological dictionary</li>
 *  <li>windowSize</li>
 *  <li>stop-word list</li>
 *  <li>corpus</li>
 *  <li>output.mat</li>
 *  <li>output.basis</li>
 * </ol>
 */
object MorphAnalysisWordsi extends Wordsi {

    def main(vargs:Array[String]) {
        val (args, loadBasis) = if (vargs(0) == "-l") (vargs.slice(1, vargs.size), true) 
                                else (vargs, false)

        val windowSize = args(1).toInt

        val basis = if (!loadBasis) {
            val excludeSet = new HashSet[String]()
            Source.fromFile(args(2)).getLines.foreach { excludeSet.add(_) }
            new FilteredStringBasisMapping(excludeSet)
        } else {
            val b:BasisMapping[String, String] = SerializableUtil.load(args(5))
            b.setReadOnly(true)
            b
        }

        val generator = new MorphDependencyContextGenerator(basis, windowSize, args(0))

        val parser = new CoNLLDependencyExtractor()
        val extractor = new SemEvalDependencyContextExtractor(parser, generator)

        for (document <- new DependencyFileDocumentIterator(args(3)))
            extractor.processDocument(document.reader, this)

        println("Printing vectors")
        printVectors(new File(args(4)), generator.getVectorLength)

        println("Saving basis mapping")
        if (!loadBasis)
            SerializableUtil.save(basis, args(5))
    }

    val vectors = new ArrayBuffer[SparseDoubleVector]

    def acceptWord(word:String) = true

    def handleContextVector(primary: String,
                            secondary: String, 
                            vector: SparseDoubleVector) {
        vectors += vector
    }

    def printVectors(outFile: File, numDim: Int) {
        val sizedData = vectors.map(Vectors.subview(_, 0, numDim))
        VectorIO.writeVectors(sizedData, outFile)
    }

    class MorphDependencyContextGenerator(val basis:BasisMapping[String, String],
                                          windowSize: Int,
                                          dixFile: String)
                                          extends DependencyContextGenerator {

        val extractor = new WordOccrrenceContextGenerator(
            basis, new EvenWeighting(), windowSize)

        val fstp = { 
            val f = new FSTProcessor(); 
            f.load(openInFileStream(dixFile)); 
            f.initAnalysis; 
            f 
        }

        def generateContext(tree: Array[DependencyTreeNode], focus: Int) = {
            val prevWords = new LinkedList[String]()
            val nextWords = new LinkedList[String]()
            for (node <- tree.view(0, focus))
                if (basis.getDimension(node.word) >= 0)
                    prevWords.addAll(analyze(node.word))
            for (node <- tree.view(focus+1, tree.size))
                if (basis.getDimension(node.word) >= 0)
                    nextWords.addAll(analyze(node.word))
            extractor.generateContext(prevWords, nextWords)
        }

        def getVectorLength = extractor.getVectorLength
        def setReadOnly(readOnly:Boolean) { extractor.setReadOnly(readOnly) }

        val rejectFeatures = Set("<n>", "<cnjcoo>", "<cm>", "<prn>", "<det>", 
                                 "<def>", "<sg>", "<vbser>", "<abbr>", "<adj>", 
                                 "<apos>", "<cnjadv>", "<comp>", "<sent>", 
                                 "<qnt>", "<mf>", "<vblex>", "<adv>", "<sp>")

        // 1: Recognize a fully analyzed word so that they can be tokenized.  In the
        // above test case, "cats," will not be separated by white space so we require
        // this more complicated splitting method.
        val parseRegex = """\^.*?\$""".r

        // 2: Recognize a word with morphological tags.
        val morphredRegex = """\^(.+?)/(.+?)(<[0-9a-z<>]+>).*\$""".r
        // 3: Recognize a word that could not be recognized.  The transducer
        // prepends "*" to unrecognized tokens, so we match and eliminate it.
        val unknownRegex = """\^(.+)/\*(.+?)\$""".r

        // 4: A regular expression for matching morphological tags.  This is simpler
        // than writing a splitting rule.
        val featureRegex = """<.*?>""".r


        def analyze(token: String) : Seq[String] = {
            // Special cases for characters that the analyzer can't handle.
            if (token.contains("$") || token.contains("@") ||
                token.contains("[") || token.contains("]") ||
                token.contains("\\") || token.contains("/") ||
                token.contains("*") || token.contains("^"))
                return List(token)

            val out = new StringWriter()
            val inToken = token+"\n"
            fstp.analysis(new StringReader(inToken), out)
            val analyzed = out.toString
            if (analyzed == inToken)
                return List(token)

            // Iterate through the analyzed words and return a list of the tokens we care
            // about.
            val tokens = parseRegex.findAllIn(analyzed).map(parseMatch => {
                // Match the current analyzed word as being morphed or unknown.  For morphed
                // words, create a list of the lemma and the tags.  For unknown words just
                // create a list of the lemma.
                val p = parseMatch.toString match {
                    case morphredRegex(surface, lemma, tags) =>
                        lemma :: featureRegex.findAllIn(tags).toList
                    case unknownRegex(surface, lemma) =>
                        List(lemma) 
                }
                p
            }).reduceLeft(_++_).filter(!rejectFeatures.contains(_))

            // Print out the features after being fully split.  Each token and tag should be
            // separated by white space.
            tokens
        }
    }
}
