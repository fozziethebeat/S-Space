package edu.ucla.sspace

import scala.io.Source


object FilterEnglishTweets {
    def main(args: Array[String]) {
        val classifier = NaiveBayesClassifier.load(args(0))
        val tweetIter = Source.fromFile(args(1)).getLines
        tweetIter.next
        val fullTweets = tweetIter.toList.map(_.split("\\s+", 2)).toArray
        val tokenizedTweets = fullTweets.map(_(1))
                                        .map(_.replaceAll("@\\w+", ""))
                                        .map(_.replaceAll("#\\w+", ""))
                                        .map(_.toLowerCase)
                                        .map(_.replaceAll("\\s+", " "))
                                        .map(_.trim)
                                        .map(_.split("\\s+").toList) //sliding(3, 3).toSet.toList)
        val majorityLabel = classifier.categoryCounts.zipWithIndex.max._2
        for ( (tweet, ti) <- tokenizedTweets.zipWithIndex;
             if classifier.classify(tweet) == majorityLabel)
            printf("%s %s\n", fullTweets(ti)(0), fullTweets(ti)(1))
    }
}
