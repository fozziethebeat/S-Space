import scala.io.Source
import scala.math.log

object ScoreTopicModel {
    def wikisize = 1322797760
    def empty = ""

    def uciPairGen(terms: Array[String]) = 
        for (m <- 1 to terms.length-1; l <- 1 to terms.length-1; if l != m)
            yield (terms(m), terms(l))
    
    def uciScore(termCounts: Map[(String, String), Int],
                 words: (String, String)) =
        log(wikisize*termCounts.get((words._1, words._2)).get) -
        log(termCounts.get((empty, words._1)).get*
            termCounts.get((empty, words._2)).get)

    def umassPairGen(terms: Array[String]) = 
        for (m <- 1 to terms.length-1; l <- 0 to m-1) yield (terms(m), terms(l))

    def umassScore(termCounts: Map[(String, String), Int],
                   words: (String, String)) =
        log(termCounts.get((words._1, words._2)).get + 1) -
        log(termCounts.get((empty, words._2)).get)

    def main(args: Array[String]) {
        // Read in the pair counts and single counts
        var termCounts = Map[(String, String), Int]()
        for (file <- args.slice(1, 3);
             line <- Source.fromFile(file).getLines)
            termCounts += (line.split("\\s+") match {
                case Array(w1, w2, c) => ((w1.intern, w2.intern) -> c.toInt)
                case Array(w1, c) => ((empty, w1.intern) -> c.toInt)
            })

        /*
        val pairGen = 
            if (args(0) == "uci")
                uciPairGen(_)
            else
                umassPairGen _
        val scorer = 
            if (args(0) == "uci")
                uciScore(termCounts, _:(String, String))
            else
                umassScore(termCounts, _:(String, String))
                */

        val(pairGen, scorer) =
            //if (args(0) == "uci")
            //    (uciPairGen(_), uciScore(termCounts, _:(String, String)))
            //else if (args(0) == "umass")
                (umassPairGen(_), umassScore(termCounts, _:(String, String)))

        // Process each topic file and print out the score for each topic.  
        // We need to generate a topic model id and name for each model.  The
        // following regex will extract this for us.
        val modelName = """.*_(\S+)_(\d+\)-ws.*""".r
        for (top10File <- args.slice(3, args.length)) {
            val modelName(model, numTopics) = top10File
            for (line <- Source.fromFile(top10File).getLines) {
                val score = (for (pair <- pairGen(line.split("\\s+")))
                                 yield scorer(pair)).sum
                printf("%s %s %.5f\n", model, numTopics, score)
            }
        }
    }
}
