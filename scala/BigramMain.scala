import edu.ucla.sspace.basis.FilteredStringBasisMapping
import edu.ucla.sspace.bigram.BigramSpace
import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.text.StringDocument
import edu.ucla.sspace.util.SerializableUtil

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.asScalaSet
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.io.Source

import java.io.File
import java.util.HashSet


val excludeSet = new HashSet[String]()
Source.fromFile(args(0)).getLines.foreach { excludeSet.add(_) }
val basis = new FilteredStringBasisMapping(excludeSet)
val bigramSpace = new BigramSpace(basis, 8)
val parser = new CoNLLDependencyExtractor()
for (semevalFile <- args.slice(3, args.length);
     document <- new DependencyFileDocumentIterator(semevalFile)) {
    document.reader.readLine
    val tree = parser.readNextTree(document.reader)
    val wordDoc = tree.map(_.word).mkString(" ")
    bigramSpace.processDocument(new StringDocument(wordDoc).reader)
}

bigramSpace.processSpace(System.getProperties)

val matrix = Matrices.asMatrix(
    (for (w <- bigramSpace.getWords) yield bigramSpace.getVector(w)).toList)
MatrixIO.writeMatrix(matrix, new File(args(1)), Format.SVDLIBC_SPARSE_TEXT)
SerializableUtil.save(basis, args(2))
