import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.hal.EvenWeighting
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.Vectors
import edu.ucla.sspace.wordsi.Wordsi
import edu.ucla.sspace.wordsi.WordOccrrenceContextGenerator
import edu.ucla.sspace.wordsi.semeval.SemEvalContextExtractor

import org.apertium.lttoolbox.process.FSTProcessor
import org.apertium.utils.IOUtils._

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.matching.Regex

import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.util.HashSet


/**
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

    def main(args:Array[String]) {
        setup(args(0))

        val excludeSet = new HashSet[String]()
        Source.fromFile(args(2)).getLines.foreach { excludeSet.add(_) }

        val windowSize = args(1).toInt
        val basis = new FilteredStringBasisMapping(excludeSet)
        val weight = new EvenWeighting()
        val generator = new WordOccrrenceContextGenerator(
            basis, weight, windowSize)

        val parser = new CoNLLDependencyExtractor()
        val extractor = new SemEvalContextExtractor(generator, windowSize)

        for (document <- new DependencyFileDocumentIterator(args(3))) {
            val header = document.reader.readLine
            val tree = parser.readNextTree(document.reader)
            val expandedTokens = header + " " + tree.map( node => 
                if (node.lemma == header)
                    "|||| " + node.word
                else if (!excludeSet.contains(node.word))
                    analyze(node.word)
                else
                    ""
            ).mkString(" ")
            extractor.processDocument(
                new BufferedReader(new StringReader(expandedTokens)), this)
        }

        println("Printing vectors")
        printVectors(new File(args(4)), generator.getVectorLength)

        println("Saving basis mapping")
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
        val matrix = Matrices.asMatrix(
            for (v <- vectors) yield Vectors.subview(v, 0, numDim) )
        MatrixIO.writeMatrix(matrix, outFile, Format.SVDLIBC_SPARSE_TEXT)
    }

    val fstp = new FSTProcessor()

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

    // 3: Recognize a word that could not be recognized.  The transducer prepends
    // "*" to unrecognized tokens, so we match and eliminate it.
    val unknownRegex = """\^(.+)/\*(.+?)\$""".r

    // 4: A regular expression for matching morphological tags.  This is simpler
    // than writing a splitting rule.
    val featureRegex = """<.*?>""".r

    def setup(dixFile: String) {
        fstp.load(openInFileStream(dixFile))
        fstp.initAnalysis
    }

    def analyze(token: String) : String = {
        if (token.contains("$") || token.contains("@") ||
            token.contains("[") || token.contains("]") ||
            token.contains("*"))
            return token

        println(token)
        val out = new StringWriter()
        val inToken = token+"\n"
        fstp.analysis(new StringReader(inToken), out)
        val analyzed = out.toString
        if (analyzed == inToken)
            return token

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
        tokens.mkString(" ")
    }
}
