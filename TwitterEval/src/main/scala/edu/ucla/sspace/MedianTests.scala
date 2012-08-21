package edu.ucla.sspace

import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.vector.VectorMath

import scala.io.Source

import java.io.PrintWriter


object MedianTests {
    val lambda = 0.5
    val beta = 100
    val w = (0.45, 0.45, 0.10)
    val simFunc = new CosineSimilarity()

    def main(args: Array[String]) {
        val config = Config(args(0))
        val preGroupIter = Source.fromFile(args(1)).getLines
        preGroupIter.next
        val groupIter = preGroupIter.map(_.split("\\s+")(1).toInt)
        val converter = TweetModeler.load(config.featureModel.get,
                                          config.tokenBasis.get,
                                          config.neBasis.get)
        val tweetIter = converter.tweetIterator(config.taggedFile.get)
        def sim(t1: Tweet, t2: Tweet) = Tweet.sim(t1, t2, lambda, beta, w, simFunc)

        val p = new PrintWriter(args(2))
        groupIter.zip(tweetIter)
                 .toList
                 .groupBy(_._1)
                 .foreach{ case(key, group) => {
            val tweets = group.map(_._2).toArray
            val median = tweets(tweets.map(pmedian => tweets.map(point => sim(pmedian, point)).sum)
                                      .zipWithIndex.max._2)
            val mean = tweets.foldLeft( converter.emptyTweet )( _+_ )
            val altMedian = tweets(tweets.map(sim(_, mean)).zipWithIndex.max._2)

            // This looks to be just as good as the mean and median methods.  So we can actually do it in a streaming fashion.  Why does
            // this work?  I don't know.  I need maths to prove it and the maths are ugly.
            val rollingMedian = tweets.foldLeft( (converter.emptyTweet, tweets.head) ){ 
                case ( (meanTweet, medianTweet), tweet ) => {
                    val newMean = meanTweet + tweet
                    val v1 = sim(newMean, medianTweet)
                    val v2 = sim(newMean, tweet)
                    if (v1 > v2)
                        (newMean, medianTweet)
                    else
                        (newMean, tweet)
                }}._2
            p.println("Summary comparison at group: " + key)
            p.println(median.text)
            p.println(altMedian.text)
            p.println(rollingMedian.text)
        }}
        p.close
    }
}
