import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.common.SemanticSpace
import edu.ucla.sspace.common.Similarity.SimType
import edu.ucla.sspace.evaluation.FinkelsteinEtAl353WordSimilarityEvaluation
import edu.ucla.sspace.evaluation.RubensteinGoodenoughWordSimilarityEvaluation
import edu.ucla.sspace.evaluation.WordSimilarityEvaluationRunner
import edu.ucla.sspace.matrix.Matrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.Vectors

import java.io.BufferedReader
import java.io.File
import java.util.Properties

import scala.io.Source


object TopicSpaceSemanticEvaluation {

    def main(args:Array[String]) {
        val basis:BasisMapping[String, String] = SerializableUtil.load(args(0))
        val numDim = args(2).toInt
        basis.setReadOnly(true)
        val wordSpace = new BasisWordSpace(
            basis,
            MatrixIO.readMatrix(new File(args(1)), Format.DENSE_TEXT),
            numDim)

        val test = new RubensteinGoodenoughWordSimilarityEvaluation(args(4))
        //val test = new FinkelsteinEtAl353WordSimilarityEvaluation(args(4))
        val report = WordSimilarityEvaluationRunner.evaluate(
            wordSpace, test, SimType.COSINE)
        printf("%d %s %f\n", numDim, args(3), report.correlation)
    }

    class BasisWordSpace(basis: BasisMapping[String, String],
                         wordSpace: Matrix,
                         numDim: Int) extends SemanticSpace {

        def getWords() = basis.keySet

        def processDocument(reader:BufferedReader) { }

        def processSpace(props:Properties) { }

        def getVector(word: String) = basis.getDimension(word) match {
            case -1 => null
            case x:Int => Vectors.subview(wordSpace.getRowVector(x),
                                          0, numDim)
        }

        def getSpaceName() = "blah" 

        def getVectorLength() = basis.numDimensions
    }
}
