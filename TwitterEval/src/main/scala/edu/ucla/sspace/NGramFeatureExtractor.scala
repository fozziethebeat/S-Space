package edu.ucla.sspace

import Util._

import edu.ucla.sspace.util.Counter
import edu.ucla.sspace.util.ObjectCounter

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.mapAsScalaMap
import scala.io.Source

import java.io.PrintWriter


object NGramFeatureExtractor {
    def main(args: Array[String]) {
        val modeler = TweetModeler.split(args(1), args(2))
        val tweets = modeler.tweetIterator(args(0)).toList
        val tweetGroups = Source.fromFile(args(3)).getLines.map(_.split("\\s+"))
        val nGramSize = args(4).toInt
        val tokenizedTweets = tweets.map(_.text).map(tokenize)

        write(filterCounter(count(tokenizedTweets, 
                                  x => x.toList.sliding(nGramSize).map(_.mkString(" ").trim)), 
                            5),
              args(5))

        write(filterCounter(count(tokenizedTweets, 
                                  x => x.filter(validToken)),
                            2),
              args(6))
    }

    def write(items: TraversableOnce[(Int, String)], outFile: String) {
        val p = new PrintWriter(outFile)
        for ( (count, ngram) <- items )
            p.println(ngram) //"%s".format(ngram, count))
        p.close
    }

    def count(tokenizedTweets: List[Array[String]],
              itemGenerator: (Array[String]) => TraversableOnce[String]) = {
        val counter = new ObjectCounter[String]()
        for (tweetTokens <- tokenizedTweets;
             item <- itemGenerator(tweetTokens))
            counter.count(item)
        counter
    }

    def filterCounter(counter: Counter[String], limit: Int) =
        counter.iterator
               .map(e=>(e.getValue.toInt, e.getKey))
               .toList
               .sorted
               .reverse
               .takeWhile(_._1 > limit)
}
