package edu.ucla.sspace

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.vector.CompactSparseVector

import scala.io.Source
import scala.xml.{Elem,Node,XML}


trait TweetModeler {
    val entityLabels = Set("ORGANIZATION", "PERSON", "LOCATION")

    def emptyTweet() : Tweet

    def formTweet(timestamp: String, tweet: String) : Tweet

    def token(index: Int) : String

    def namedEntity(index: Int) : String

    def uniformTweet(time: Long) : Tweet

    def tokenizeXml(xml: Elem) : List[String] 

    def tokenize(text: String) : List[String] =
        tokenizeXml(XML.loadString(text))

    def rawText(tweet: Tweet) =
        XML.loadString(tweet.text).text

    def hashTagSet(tweets: List[Tweet]) = 
        tweets.map(_.text).map(text =>
            XML.loadString(text).text
               .toLowerCase
               .split("\\s+")
               .filter(_.startsWith("#"))
               .filter(Util.validTag)
               .map(Util.normalize)
               .toList
        ).reduce(_++_).toSet

    def namedEntitySet(tweets: List[Tweet]) =
        tweets.map(_.text).map(text => 
            XML.loadString(text).child
               .filter(node => entityLabels.contains(node.label))
               .map(_.text)
               .toList
        ).reduce(_++_).toSet

    def tweetIterator(tweetList: Iterator[String]) : Iterator[Tweet] = {
        tweetList.next
        tweetList.map(line => {
            val Array(timestamp, tweet) = line.split("\\s+", 2)
            try {
                formTweet(timestamp, tweet)
            } catch {
                case e => System.err.println("Error on : ", line)
                          System.exit(1)
                          e.printStackTrace
                          new Tweet(0, null, null, "")
            }
        })
    }

    def tweetIterator(tweetFile: String) : Iterator[Tweet] =
        tweetIterator(Source.fromFile(tweetFile).getLines)

}

class JointTokenTweetModeler(tokenList: Iterator[String],
                             namedEntityList: Iterator[String]) extends TweetModeler {
    val basis = TweetModeler.loadBasis(tokenList ++ namedEntityList)
    val ONE_VECTOR = new CompactSparseVector(1)
    val uniformVector = new CompactSparseVector(Array.fill(basis.numDimensions)(1d))

    def emptyTweet() = new Tweet(0,
                                 new CompactSparseVector(basis.numDimensions),
                                 ONE_VECTOR,
                                 "")

    def uniformTweet(time: Long) = new Tweet(time, uniformVector, ONE_VECTOR, "")

    def token(index: Int) = basis.getDimensionDescription(index)

    def namedEntity(index: Int) = "ONE"

    def tokenizeXml(xml: Elem) = 
        xml.child.map(extractTokens).reduce(_++_)

    def formTweet(timestamp: String, tweet: String) = {
        val vector = new CompactSparseVector(basis.numDimensions)
        val tweetXml = XML.loadString(tweet)
        for (tokenId <- tokenizeXml(tweetXml).map(basis.getDimension); if tokenId >= 0)
            vector.add(tokenId, 1d)
        new Tweet(timestamp.toDouble.toLong, vector, ONE_VECTOR, tweet)
    }

    def extractTokens(node: Node) = 
        node.label match {
            case "ORGANIZATION" => List(node.text)
            case "PERSON" => List(node.text)
            case "LOCATION" => List(node.text)
            case _ => Util.tokenize(node.text).toList
        }
}

class JointNGramTweetModeler(ngramList: Iterator[String],
                             tokenList: Iterator[String],
                             ngramSize: Int) extends TweetModeler {

    val basis = TweetModeler.loadBasis(ngramList ++ tokenList)
    val ONE_VECTOR = new CompactSparseVector(1)
    val uniformVector = new CompactSparseVector(Array.fill(basis.numDimensions)(1d))

    def emptyTweet() = new Tweet(0,
                                 new CompactSparseVector(basis.numDimensions),
                                 ONE_VECTOR,
                                 "")

    def uniformTweet(time: Long) = new Tweet(time, uniformVector, ONE_VECTOR, "")

    def token(index: Int) = basis.getDimensionDescription(index)

    def namedEntity(index: Int) = "ONE"

    def tokenizeXml(xml: Elem) = Util.tokenize(xml.text).toList

    def formTweet(timestamp: String, tweet: String) = {
        val vector = new CompactSparseVector(basis.numDimensions)
        val tweetXml = XML.loadString(tweet)
        val tokens = tokenizeXml(tweetXml)
        for (ngram <- tokens.sliding(ngramSize)
                            .map(_.mkString(" ").trim)
                            .map(basis.getDimension)
                            .filter(_>= 0))
            vector.add(ngram, 1d)
        for (token <- tokens.map(basis.getDimension)
                            .filter(_>= 0))
            vector.add(token, 1d)
        new Tweet(timestamp.toDouble.toLong, vector, ONE_VECTOR, tweet)
    }
}

class SplitFeatureTweetModeler(tokenList: Iterator[String],
                               namedEntityList: Iterator[String]) extends TweetModeler {

    val tokenBasis = TweetModeler.loadBasis(tokenList)
    val uniformTokenVector = new CompactSparseVector(Array.fill(tokenBasis.numDimensions)(1d))
    val neBasis = TweetModeler.loadBasis(namedEntityList)
    val uniformNeVector = new CompactSparseVector(Array.fill(neBasis.numDimensions)(1d))

    def emptyTweet() = new Tweet(0,
                                 new CompactSparseVector(tokenBasis.numDimensions),
                                 new CompactSparseVector(neBasis.numDimensions),
                                 "")

    def token(index: Int) = tokenBasis.getDimensionDescription(index)

    def namedEntity(index: Int) = neBasis.getDimensionDescription(index)

    def uniformTweet(time: Long) = new Tweet(time, uniformTokenVector, uniformNeVector, "")

    def tokenizeXml(xml: Elem) = xml.child
                                    .filter(_.label!="PERSON")
                                    .map(_.text)
                                    .mkString(" ")
                                    .toLowerCase
                                    .split("\\s+")
                                    .toList

    def formTweet(timestamp: String, tweet: String) = {
        val tokenVector = new CompactSparseVector(tokenBasis.numDimensions)
        val tweetXml = XML.loadString(tweet)
        for (tokenId <- tokenizeXml(tweetXml).map(tokenBasis.getDimension);
             if tokenId >= 0)
            tokenVector.add(tokenId, 1d)
        val neVector = new CompactSparseVector(neBasis.numDimensions)
        for (neId <- (tweetXml \ "PERSON").map(_.text).map(neBasis.getDimension);
             if neId >= 0)
            neVector.add(neId, 1d)
        new Tweet(timestamp.toDouble.toLong, tokenVector, neVector, tweet)
    }
}

object TweetModeler {
    def split(tokenFile: String, neFile: String) = new SplitFeatureTweetModeler(Source.fromFile(tokenFile).getLines,
                                                                                 Source.fromFile(neFile).getLines)
    def joint(tokenFile: String, neFile: String) = new JointTokenTweetModeler(Source.fromFile(tokenFile).getLines,
                                                                               Source.fromFile(neFile).getLines)
    def load(featureModel: String, tokenFile: String, neFile: String) =
        featureModel match {
            case "split" => split(tokenFile, neFile)
            case "joint" => joint(tokenFile, neFile)
        }

    def loadBasis(items: Iterator[String]) = {
        val basis = new StringBasisMapping()
        for (item <- items)
            basis.getDimension(item)
        basis.setReadOnly(true)
        basis
    }
}
