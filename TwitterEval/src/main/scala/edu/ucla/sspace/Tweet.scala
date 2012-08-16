package edu.ucla.sspace

import edu.ucla.sspace.similarity.SimilarityFunction
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorMath

import scala.math.abs
import scala.math.pow


class Tweet(val timestamp: Long,
            val tokenVector: SparseDoubleVector,
            val neVector: SparseDoubleVector,
            val text: String) {

    def +(t: Tweet) = {
        VectorMath.add(tokenVector, t.tokenVector)
        VectorMath.add(neVector, t.neVector)
        Tweet(timestamp + t.timestamp, tokenVector, neVector, text)
    }

    def +?(t: Tweet) = {
        VectorMath.add(tokenVector, t.tokenVector)
        VectorMath.add(neVector, t.neVector)
        Tweet(timestamp, tokenVector, neVector, text)
    }

    def avgTime(n: Int) =
        Tweet(timestamp / n, tokenVector, neVector, text)
}

object Tweet {

    def apply() = new Tweet(0, new CompactSparseVector(), new CompactSparseVector(), "")

    def apply(ts: Long, tVec: SparseDoubleVector, neVec: SparseDoubleVector, text: String) =
        new Tweet(ts, tVec, neVec, text)

    def apply(t: Tweet) = {
        val tv = new CompactSparseVector(t.tokenVector.length)
        VectorMath.add(tv, t.tokenVector)
        val nv = new CompactSparseVector(t.neVector.length)
        VectorMath.add(nv, t.neVector)
        new Tweet(t.timestamp, tv, nv, t.text)
    }

    def sumsim(t1: Tweet, t2: Tweet,
               lambda: Double, beta: Double, weights: (Double, Double, Double),
               simFunc: SimilarityFunction) =
        weights._1*pow(lambda, abs(t1.timestamp - t2.timestamp)/beta) + // Time
        (if (weights._2 != 0d) weights._2*simFunc.sim(t1.tokenVector, t2.tokenVector) else 0d) + // Topic
        (if (weights._3 != 0d) weights._3*simFunc.sim(t1.neVector, t2.neVector) else 0d) // Named Entities

    def prodSim(t1: Tweet, t2: Tweet,
                lambda: Double, beta: Double, weights: (Double, Double, Double),
                simFunc: SimilarityFunction) =
        pow(lambda, abs(t1.timestamp - t2.timestamp)/beta) *
        simFunc.sim(t1.tokenVector, t2.tokenVector)

    def sim(t1: Tweet, t2: Tweet,
            lambda: Double, beta: Double, weights: (Double, Double, Double),
            simFunc: SimilarityFunction) =
        sumsim(t1, t2, lambda, beta, weights, simFunc)

    def medianSummary(tweets: List[Tweet], converter: TweetModeler, simFunc: (Tweet, Tweet) => Double) = 
        tweets.map(proposedMedian => (tweets.map(simFunc(_, proposedMedian)).sum, proposedMedian))
              .maxBy(_._1)._2

    def meanSummary(tweets: List[Tweet], converter: TweetModeler, simFunc: (Tweet, Tweet) => Double) = {
        val mean = tweets.foldLeft( converter.emptyTweet )( _+_ ).avgTime(tweets.size)
        tweets.map(proposedSummary => (simFunc(proposedSummary, mean), proposedSummary))
              .maxBy(_._1)._2
    }

    def phraseGraphSummary(tweets: List[Tweet], converter: TweetModeler, simFunc: (Tweet, Tweet) => Double) = {
        val phraseGraph = new PhraseGraph2()
        val tokenizedTweets = tweets.map(_.text).map(converter.tokenize)
        phraseGraph.train(tokenizedTweets, Set[String]())
        val scored = phraseGraph.score(tokenizedTweets, Set[String]())
        tweets( scored.maxBy(_._1)._2 )

        /*
        meanSummary(tweets, converter, simFunc)
        val tokenizedTweets = tweets.map(_.text).map(converter.tokenize)
        val phraseGraph = new PhraseGraph()
        phraseGraph.train(tokenizedTweets)
        tweets(phraseGraph.score(tokenizedTweets).zipWithIndex.maxBy(_._1._2)._2)
        */
    }

}
