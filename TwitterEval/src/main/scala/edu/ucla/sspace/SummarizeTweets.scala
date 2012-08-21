package edu.ucla.sspace

import edu.ucla.sspace.similarity.CosineSimilarity

import scala.io.Source
import java.io.PrintWriter


object SummarizeTweets {
    val lambda = 0.5
    val beta = 100
    val w = (0.45, 0.45, 0.10)
    val sim = new CosineSimilarity()

    def main(args: Array[String]) {
        val featureModel = args(0)
        val tokenBasis = args(1)
        val neBasis = args(2)
        val converter = TweetModeler.load(featureModel, tokenBasis, neBasis)
        val tweetIterator = converter.tweetIterator(args(3))
        val tweets = tweetIterator.toList
        val assignments = Source.fromFile(args(4)).getLines
                                .map(_.split("\\s+")(1))
                                .toList
                                .tail
                                .map(_.toInt)

        val writer = new PrintWriter(args(5))
        val simFunc = (t1: Tweet, t2: Tweet) => Tweet.sim(t1, t2, lambda, beta, w, sim)
        val summaryMethod = args(6) match {
            case "median" => Tweet.medianSummary _
            case "mean" => Tweet.meanSummary _
            case "phrase" => Tweet.phraseGraphSummary _
        }

        val summaries = assignments.zip(tweets)
                                   .groupBy(_._1)
                                   .foreach{ case(groupId, group) => {
            val groupTweets = group.map(_._2)
            val startTime = groupTweets.map(_.timestamp).min
            val summaryTweet = summaryMethod(groupTweets, converter, simFunc)
            val meanTime = summaryTweet.timestamp
            val summary = converter.rawText(summaryTweet)
            writer.println("%d %d %d \"%s\"".format(startTime, meanTime, groupId, summary))
        }}
        writer.close
    }
}
