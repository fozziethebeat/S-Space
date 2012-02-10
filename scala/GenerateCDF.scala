import edu.ucla.sspace.matrix.SymmetricMatrix
import edu.ucla.sspace.util.Counter

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.io.Source

import java.io.PrintWriter


object GenerateCDF {
    def readConsensusMatrix(fileName: String) = {
        val matrixLines = Source.fromFile(fileName).getLines
        val Array(rows, cols) = matrixLines.next.split("\\s+")
        val consensus = new SymmetricMatrix(rows.toInt, cols.toInt)
        for ((line, row) <- matrixLines zipWithIndex;
             (value, col) <- line.trim.split("\\s+") zipWithIndex)
            if (value != "")
                consensus.set(row.toInt, col.toInt, value.toDouble)
        consensus
    }

    def main(args:Array[String]) {
        val results = args.slice(1, args.length) map { cmFile =>
            val cm = readConsensusMatrix(cmFile)
            val scoreCounter = new Counter[Double]()
            for (x <- 0 until cm.rows; y <- 0 until x)
                scoreCounter.count(cm.get(x,y))
            val valueList = (scoreCounter map {
                entry => (entry.getKey, entry.getValue) }).toList.sorted
            val numPairs = cm.rows * (cm.rows -1) / 2.0
            var sum = 0
            val cdf_k = ((0.0, 0.0) :: (valueList map {
                ic => sum += ic._2; (ic._1, sum/numPairs) })) :+ (1.0, 1.0)
            val a_k = (for (Seq((y1, _), (y2, cdf)) <- cdf_k.sliding(2))
               yield (y2 - y1) * cdf) reduceLeft(_+_)

            (cdf_k, a_k)
        }

        val cdfWriter = new PrintWriter(args(0) + ".cdf.dat")
        cdfWriter.println("k indexValue cdf")
        for (((cdf, _), k) <- results zipWithIndex; (c, s) <- cdf)
            cdfWriter.println("%d %f %f".format(k+2, c, s))
        cdfWriter.close()

        val deltaWriter = new PrintWriter(args(0) + ".delta.dat")
        deltaWriter.println("k delta")
        for (((_, a_k), k) <- results zipWithIndex)
            deltaWriter.println("%d %f".format(k+2, 
                if (k == 0) a_k else (a_k - results(k-1)._2) / a_k))
        deltaWriter.close
    }
}
