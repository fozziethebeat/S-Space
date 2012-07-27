package edu.ucla.sspace

import Util._

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.vector.CompactSparseVector

import scala.io.Source
import scala.xml.XML


trait TweetModeler {
    def emptyTweet() : Tweet

    def formTweet(timestamp: String, tweet: String) : Tweet

    def token(index: Int) : String

    def namedEntity(index: Int) : String

    def tweetIterator(tweetList: Iterator[String]) : Iterator[Tweet] = {
        tweetList.map(line => {
            val Array(timestamp, tweet) = line.split("\\s+", 2)
            try {
                formTweet(timestamp, tweet)
            } catch {
                case _ => new Tweet(-1, null, null, "")
            }
        }).filter(_.timestamp >= 0)
    }

    def tweetIterator(tweetFile: String) : Iterator[Tweet] = tweetIterator(Source.fromFile(tweetFile).getLines)

}

class JointNGramTweetModeler(ngramList: Iterator[String],
                             tokenList: Iterator[String],
                             ngramSize: Int) extends TweetModeler {

    val basis = TweetModeler.loadBasis(ngramList ++ tokenList)
    val ONE_VECTOR = new CompactSparseVector(1)

    def emptyTweet() = new Tweet(0,
                                 new CompactSparseVector(basis.numDimensions),
                                 ONE_VECTOR,
                                 "")

    def token(index: Int) = basis.getDimensionDescription(index)

    def namedEntity(index: Int) = "ONE"

    def formTweet(timestamp: String, tweet: String) = {
        val vector = new CompactSparseVector(basis.numDimensions)
        val text = XML.loadString(tweet).text
        val tokens = tokenize(text)
        for (ngram <- tokens.toList
                            .sliding(ngramSize)
                            .map(_.mkString(" ").trim)
                            .map(basis.getDimension)
                            .filter(_>= 0))
            vector.add(ngram, 1d)
        for (token <- tokens.map(basis.getDimension)
                            .filter(_>= 0))
            vector.add(token, 1d)
        new Tweet(timestamp.toDouble.toLong, vector, ONE_VECTOR, text)
    }
}

class SplitFeatureTweetModeler(tokenList: Iterator[String],
                               namedEntityList: Iterator[String]) extends TweetModeler {

    val tokenBasis = TweetModeler.loadBasis(tokenList)
    val neBasis = TweetModeler.loadBasis(namedEntityList)

    def emptyTweet() = new Tweet(0,
                                 new CompactSparseVector(tokenBasis.numDimensions),
                                 new CompactSparseVector(neBasis.numDimensions),
                                 "")

    def token(index: Int) = tokenBasis.getDimensionDescription(index)

    def namedEntity(index: Int) = neBasis.getDimensionDescription(index)

    def formTweet(timestamp: String, tweet: String) = {
        val tweetXml = XML.loadString(tweet)
        val tokenVector = new CompactSparseVector(tokenBasis.numDimensions)
        for (tokenId <- tweetXml.child.filter(_.label!="PERSON")
                                      .map(_.text)
                                      .mkString(" ")
                                      .split("\\s+")
                                      .map(_.toLowerCase)
                                      .map(tokenBasis.getDimension);
             if tokenId >= 0)
            tokenVector.add(tokenId, 1d)
        val neVector = new CompactSparseVector(neBasis.numDimensions)
        for (neId <- (tweetXml \ "PERSON").map(_.text).map(neBasis.getDimension);
             if neId >= 0)
            neVector.add(neId, 1d)
        new Tweet(timestamp.toDouble.toLong, tokenVector, neVector, tweetXml.text)
    }
}

object TweetModeler {
     def split(tokenFile: String, neFile: String) = new SplitFeatureTweetModeler(Source.fromFile(tokenFile).getLines,
                                                                                 Source.fromFile(neFile).getLines)
     def joint(tokenFile: String, neFile: String, nGramSize: Int) = new JointNGramTweetModeler(Source.fromFile(tokenFile).getLines,
                                                                                               Source.fromFile(neFile).getLines,
                                                                                               nGramSize)
    def loadBasis(items: Iterator[String]) = {
        val basis = new StringBasisMapping()
        for (item <- items)
            basis.getDimension(item)
        basis.setReadOnly(true)
        basis
    }
}
