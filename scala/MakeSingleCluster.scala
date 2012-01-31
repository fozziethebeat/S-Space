import scala.io.Source

import java.io.File
import java.io.PrintWriter


object MakeSingleCluster {
    def printSolution(fileName: String, numRows: Int) {
        val writer = new PrintWriter(fileName)
        writer.println("%d %d".format(numRows, 1))
        for (point <- 0 until numRows)
            writer.printf("%d ".format(point))
        writer.println
        writer.close
    }

    def main(args:Array[String]) {
        val numRows = Source.fromFile(args(0)).getLines.toList.size
        for (m <- args.slice(1, args.length)) {
            printSolution("%s.rca01".format(m), numRows)
            printSolution("%s.cca01".format(m), numRows)
        }
    }
}
