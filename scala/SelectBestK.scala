import scala.io.Source


/**
 * Heuristics for selecting the best number of clusters using the change in the
 * area of the CDF of a consensus matrix.
 */
object SelectBestK {
    def pickFirstPositiveSlope(deltaPoints: List[(Int, Double)]):Int = {
        for (Seq((k, y1), (_, y2)) <- deltaPoints.sliding(2))
            if (y2-y1 > 0)
                return k
        return 1
    }

    def pickFirstSlowSlope(deltaPoints: List[(Int, Double)]):Int = {
        for (Seq((k, y1), (_, y2)) <- deltaPoints.sliding(2))
            if ((y2-y1)/y1 > -.50)
                return k
        return 1
    }

    def main(args:Array[String]) {
        val deltaLines = Source.fromFile(args(0)).getLines
        deltaLines.next
        val deltaPoints = (deltaLines map { line =>
            val Array(k, delta) = line.split("\\s+")
            (k.toInt, delta.toDouble)
        }).toList

        printf("%s %02d\n", args(2), args(1) match {
            case "fps" => pickFirstPositiveSlope(deltaPoints)
            case "fss" => pickFirstSlowSlope(deltaPoints)
            case _ => throw new IllegalArgumentException("No Such Metric")
        })
    }
}
