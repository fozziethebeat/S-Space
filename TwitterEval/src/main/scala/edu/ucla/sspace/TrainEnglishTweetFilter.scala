package edu.ucla.sspace

import scala.io.Source


object TrainEnglishTweetFilter {
    def main(args: Array[String]) {
        System.err.println("Loading Tweets")
        val tweetIter = Source.fromFile(args(0)).getLines
        tweetIter.next
        val fullTweets = tweetIter.toList.map(_.split("\\s+", 2)).toArray
        System.err.println("Cleaning Tweets")
        val tokenizedTweets = fullTweets.map(_(1))
                                        .map(_.replaceAll("@\\w+", ""))
                                        .map(_.replaceAll("#\\w+", ""))
                                        .map(_.toLowerCase)
                                        .map(_.replaceAll("\\s+", " "))
                                        .map(_.trim)
                                        .map(_.sliding(3, 3).toSet.toList)
        System.err.println("Training on Tweets")
        val trainer = new NaiveBayesClassifier(2, 0.0001, Array(.01,0.99))
        val classifier = trainer.trainEM(tokenizedTweets, 30)
        NaiveBayesClassifier.save(classifier, args(1))
    }
}
