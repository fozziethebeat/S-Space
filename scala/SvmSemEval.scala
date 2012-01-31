import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.vector.DoubleVector
import edu.ucla.sspace.vector.SparseDoubleVector

import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import scala.sys.process._

import java.io.File
import java.io.PrintWriter


object SvmSemEval {

    def makeInstance(rowVector:DoubleVector, sense: String, id: String) = {
        val senseId = sense.split("\\.")(2)
        def printValue(x: Int) = "%d:%f".format(x,rowVector.get(x))
        val values = rowVector match {
            case sv:SparseDoubleVector => 
                sv.getNonZeroIndices.map(printValue(_)).mkString(" ")
            case dv:DoubleVector =>
                (0 until dv.length).map(printValue(_)).mkString(" ")
        }
        senseId + " " + values
    }

    def main(args:Array[String]) {
        System.err.println(args(1))
        val m = if (args(1).contains("lda") ||
                    args(1).contains("svd") ||
                    args(1).contains("nmf"))
                MatrixIO.readMatrix(new File(args(1)), Format.DENSE_TEXT)
            else
                MatrixIO.readSparseMatrix(args(1), Format.SVDLIBC_SPARSE_TEXT)

        val headers = Source.fromFile(args(2)).getLines.toList
        val labels = Source.fromFile(args(3)).getLines.toList
        val testLabels = Source.fromFile(args(4)).getLines.map(_.split("\\s+")(1)).toSet

        val writer = new PrintWriter("semeval.data.train")
        for ( r <- 0 until m.rows; if !testLabels.contains(headers(r)) )
            writer.println(makeInstance(m.getRowVector(r), labels(r), headers(r)))
        writer.close
        ("svm-scale -l 0 -u 1 -s range semeval.data.train" #> new File("semeval.train.scale")).!
        ("svm-train semeval.train.scale semeval.model" #> new File("/dev/null")).!

        val term = headers(0).replaceAll(".[0-9]+", "")
        val testWriter = new PrintWriter("semeval.data.test")
        for ( r <- 0 until m.rows; if testLabels.contains(headers(r)))
            testWriter.println(makeInstance(m.getRowVector(r), labels(r), headers(r)))
        testWriter.close
        val testHeaders = (0 until m.rows).filter(r => testLabels.contains(headers(r))).map(headers(_))
        ("svm-scale -l 0 -u 1 -r range semeval.data.test" #> new File("semeval.test.scale")).!
        ("svm-predict semeval.test.scale semeval.model semeval.solution" #> new File("/dev/null")).!
        for ( (label, i) <- Source.fromFile("semeval.solution").getLines.zipWithIndex)
            printf("%s %s %s.%s\n", term, testHeaders(i), term, label)
    }
}
