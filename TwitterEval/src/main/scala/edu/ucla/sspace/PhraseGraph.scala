package edu.ucla.sspace

import edu.ucla.sspace.util.ObjectCounter

import scala.io.Source
import scala.math.log


class PhraseGraph {

    val tokenCounts = new ObjectCounter[String]()

    def train(tweets: Seq[Seq[String]]) {
        for (tweet <- tweets ; List(word1, word2) <- ("$START" :: tweet.toList).sliding(2) )
            tokenCounts.count(word2)
    }

    def score(tweets: Seq[Seq[String]]) =
        for (tweet <- tweets) yield 
            (tweet, tweet.map(tokenCounts.getCount).sum)
}

class PhraseGraph2 {

    var leftGraphs = Map[String, PhraseNode]()
    var rightGraphs = Map[String, PhraseNode]()
    def train(tweets: Seq[List[String]], keyPhrases: Set[String]) {
        // Create a left and right root node for each key phrase.
        keyPhrases.foreach(phrase => {
            leftGraphs += (phrase -> new PhraseNode())
            rightGraphs += (phrase -> new PhraseNode())
        })

        // Iterate over each tweet and check to see if a key phrase appears in the tweet.  If it does, add the tokens in that tweet to the
        // graphs for the key phrase.
        for (tweet <- tweets; keyPhrase <- keyPhrases) {
            val (prev, next) = tweet.span(_ != keyPhrase)
            // Check to see if we actually found a sentence with the key phrase.  If we did, add the words to the phrase graph for that key
            // phrase.
            if (next.size > 0 && next.head == keyPhrase) {
                addLinks(prev.reverse, leftGraphs(keyPhrase))
                addLinks(next.tail, rightGraphs(keyPhrase))
            }
        }

        leftGraphs.values.foreach(weightNodes(_, Util.rejectSet, 0))
    }

    def score(tweets: Seq[List[String]], keyPhrases: Set[String]) =
        keyPhrases.map(keyPhrase => {
            (keyPhrase,
             tweets.zipWithIndex.map{ case(tweet, ti) => {
                val (prev, next) = tweet.span(_ == keyPhrase)
                // Check to see if we actually found a sentence with the key phrase.  If we did, add the words to the phrase graph for that key
                // phrase.
                if (next.size > 0 && next.head == keyPhrase)
                    (scoreGraph(prev.reverse, leftGraphs(keyPhrase)) + scoreGraph(next.tail, rightGraphs(keyPhrase)), ti)
                else 
                    (0d, ti)
              }}
            )
        })

    def scoreGraph(tokens: List[String], node: PhraseNode) : Double =
        tokens match {
            case head :: tail => node.linkMap.get(head) match {
                case Some(child) => scoreGraph(tail, child) + node.inCount
                case None => node.inCount
            }
            case _ => node.inCount
        }

    def weightNodes(node: PhraseNode, rejectSet: Set[String], depth: Int) {
        for ( (word, child) <- node.linkMap ) {
            child.inCount = if (rejectSet.contains(word)) 0
                            else child.inCount - (depth * log(child.inCount))
            weightNodes(child, rejectSet, depth)
        }
    }

    def addLinks(tokens: Seq[String], root: PhraseNode) {
        tokens.foldLeft(root)( (oldGraph, token) => {
            val newGraph = oldGraph.neighbor(token)
            newGraph.inCount += 1
            newGraph
        })
    }

    override def toString() : String = {
        val indent = ""
        leftGraphs.map{ case (term, child) =>
            indent + term + "\n" + child.toString(indent + "  ")
        }.mkString("") + "\n" + 
        rightGraphs.map{ case (term, child) =>
            indent + term + "\n" + child.toString(indent + "  ")
        }.mkString("")
    }
}

class PhraseNode {
    var inCount = 0d
    var linkMap = Map[String,PhraseNode]()

    def neighbor(term: String) =
        linkMap.get(term) match {
            case Some(node) => node
            case None => { val node = new PhraseNode()
                           linkMap += (term -> node)
                           node
                         }
        }

    def toString(indent: String) : String  =
        linkMap.map{ case (term, child) =>
            indent + term + "\n" + child.toString(indent + "  ")
        }.mkString("")
}

object PhraseGraph {
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

        assignments.zip(tweets)
                   .groupBy(_._1)
                   .foreach{ case(groupId, group) => {
            val groupTweets = group.map(_._2)
            val namedEntitySet = converter.namedEntitySet(groupTweets)
            val hashTagSet = converter.hashTagSet(groupTweets)
            val keySet = hashTagSet ++ namedEntitySet
            val phraseGraph = new PhraseGraph2()
            val tokenizedTweets = groupTweets.map(_.text).map(converter.tokenize)
            phraseGraph.train(tokenizedTweets, keySet)
            val scoredLists = phraseGraph.score(tokenizedTweets, keySet)
            for ( (keyPhrase, scoredList) <- scoredLists)
                printf("%d: %s -> %s\n",
                       groupId,
                       keyPhrase,
                       converter.rawText(groupTweets(scoredList.max._2)))
            }}
            /*
        val phraseGraph = new PhraseGraph2()
        val sentences = List("the cat in the hat", 
                             "the cat and the dog",
                             "a cat and the chicken")
        val tokenizedSentences = sentences.map(_.split("\\s+").toList)
        phraseGraph.train(tokenizedSentences, Set("cat"))
        println(phraseGraph)
        */
    }
}
