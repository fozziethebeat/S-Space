package edu.ucla.sspace


import edu.ucla.sspace.util.HashMultiMap
import edu.ucla.sspace.util.Indexer
import edu.ucla.sspace.util.ObjectCounter
import edu.ucla.sspace.util.ObjectIndexer

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashSet
import scala.io.Source
import scala.math.log

import java.io.PrintWriter


class PhraseGraph2 {

/*
    var leftGraphs = Map[String, CondensedTrie]()
    var rightGraphs = Map[String, CondensedTrie]()
    */
    val graph = new CondensedTrie(true)

    def train(tweets: Seq[List[String]], keyPhrases: Set[String]) {
        graph.train(tweets)
        // Create a left and right root node for each key phrase.
        /*
        keyPhrases.foreach(phrase => {
            leftGraphs += (phrase -> new CondensedTrie(true))
            rightGraphs += (phrase -> new CondensedTrie(true))
        })

        // Iterate over each tweet and check to see if a key phrase appears in the tweet.  If it does, add the tokens in that tweet to the
        // graphs for the key phrase.
        for (keyPhrase <- keyPhrases) {
            val phraseMatches = tweets.map(_.span(_ != keyPhrase))
                                      .filter{ case(prev, next) => next.size > 0 && next.head == keyPhrase }
            leftGraphs(keyPhrase).train(phraseMatches.map(_._1).map(_.reverse))
            rightGraphs(keyPhrase).train(phraseMatches.map(_._2).map(_.tail))
        }
        */
    }

    def score(tweets: Seq[List[String]], keyPhrases: Set[String]) =
        tweets.zipWithIndex.map{ case(tweet, ti) => {
            (scoreGraph(tweet, graph.root), ti)
        }}
    /*
        keyPhrases.map(keyPhrase => {
            (keyPhrase,
             tweets.zipWithIndex.map{ case(tweet, ti) => {
                val (prev, next) = tweet.span(_ == keyPhrase)
                // Check to see if we actually found a sentence with the key phrase.  If we did, add the words to the phrase graph for that key
                // phrase.
                if (next.size > 0 && next.head == keyPhrase)
                    (scoreGraph(prev.reverse, leftGraphs(keyPhrase).root) + scoreGraph(next.tail, rightGraphs(keyPhrase).root), ti)
                else 
                    (0d, ti)
              }}
            )
        })
        */

    def scoreGraph(tokens: List[String], node: PhraseNode) : Double =
        tokens match {
            case head :: tail => node.linkMap.get(head) match {
                case Some(child) => scoreGraph(tail, child) + node.inCount
                case None => node.inCount
            }
            case _ => node.inCount
        }
}

/**
 * The {@link CondensedTrie} represents a single phrase graph centered around a single key phrase.  Lists of tokens, representing sentences,
 * can be added to the {@link CondensedTrie} to create a minimal finite state automata which counts the number of times sequences of tokens
 * appear.  Lists must be added in fully sorted order, otherwise the behavior is undefined.  Once the {@link CondensedTrie} has been
 * completed, a sequence of tokens can be used to walk through the {@link CondensedTrie} and count the weight of that particular sequence.
 */
class CondensedTrie(useSubsumingMatches: Boolean = false) {

    /**
     * The filtering method for determining which candidate node from the register will replace existing children nodes during the
     * compaction phase.
     */
    val matchMethod = if (useSubsumingMatches) subsumeMatch _ 
                      else exactMatch _

    /**
     * The {@code register} records all nodes that have been previously added to the {@link CondensedTrie} so that after an entry has been
     * made into the trie, we can check to see if newly created nodes are duplicates of existing nodes and can thus merge them.  The
     * appraoch we're currently using models an exact minimum finite state automoton which only merges nodes when they match exactly. 
     *
     * </p> 
     *
     * An alternative approach that we can do by using a {@link HashMultiMap} would be to also merge nodes when the newly created node has
     * links which are a pure subset of an existing node in the registry, and also has the same label.  This would create an automata that
     * accepts more strings than were used to build it, but for some purposes, such as for weighting the likelihood of phrases, this is
     * acceptable.
     */
    var register = new HashMultiMap[String, PhraseNode]()

    /**
     * The root node in the {@link CondensedTrie}.  This always has an emtpy label.
     */
    val root = new PhraseNode("")

    /**
     * Trains the {@link CondensedTrie} on a list of token sequences.  This list does not have to be sorted and will instead be sorted
     * before any sentences are added.
     */
    def train(tokenizedSentences: Seq[List[String]]) {
        for ( tokenizedSentence <- tokenizedSentences.sortWith(Util.tokenListComparator) )
            add(tokenizedSentence)
        finish
    }

    /**
     * Adds the list of tokens to this {@link CondensedTrie}.
     */
    def add(tweet: List[String]) {
        val (lastSharedNode, remainingSuffix) = computeDeepestCommonNodeAndSuffix(root, tweet)
        if (lastSharedNode.linkMap.size != 0)
            replaceOrRegister(lastSharedNode)
        addSuffix(lastSharedNode, remainingSuffix)
    }

    /**
     * This must be called after all sentences have been added in order to do a final round of condensing.  This will only inspect the
     * tokens in the last sentence added.
     */
    def finish {
        if (root.linkMap.size != 0)
            replaceOrRegister(root)
    }

    /**
     * Returns the deepest {@link PhraseNode} in the {@link CondensedTrie} matching the tokens in {@code tweet}.  When a {@link PhraseNode}
     * no longer has an arc matching the first element in {@code tweet}, this returns that {@link PhraseNode} and the remaining tokens in
     * {@code tweet} that cold not be matched.
     */
    def computeDeepestCommonNodeAndSuffix(node: PhraseNode, tweet: List[String]) : (PhraseNode, List[String]) =
        tweet match {
            case head::tail => node.linkMap.get(head) match {
                case Some(child) => {
                    child.addCount()
                    computeDeepestCommonNodeAndSuffix(child, tail)
                }
                case None => (node, tweet)
            }
            case _ => (node, tweet)
        }

    /**
     * Adds all tokens in {@code tweet} as a branch stemming from {@code node}
     */
    def addSuffix(node: PhraseNode, tweet: List[String]) {
        tweet.foldLeft(node)( (n, t) => n.neighbor(t).addCount() )
    }

    /**
     * Returns true if {@code child} and {@code candidate} are exact matches.
     */
    def exactMatch(candidate: PhraseNode, child: PhraseNode) = 
        candidate == child

    /**
     * Returns true if {@code child} and {@code candidate} have the same label and the links from {@code child} are a subset of the links
     * from {@code candidate}.
     */
    def subsumeMatch(candidate: PhraseNode, child: PhraseNode) =
        if (candidate.label != child.label)
            false
        else
            child.linkMap.map{ case(key, subchild) =>
                candidate.linkMap.get(key) match {
                    case Some(otherSubchild) if otherSubchild.pointerHashCode == subchild.pointerHashCode => true
                    case _ => false
                }
            }.foldLeft(true)(_&&_)

    /**
     * Recursively walks down the chain of last nodes added starting at {@code node} and then checks if the last child of that node are in the
     * registry.  If an equivalent {@link PhraseNode} matching the last child is in the registry, this replaces the last child with the
     * registry node.  If no matching {@link PhraseNode} exists in the registry, then the last child is added to the registry.
     */
    def replaceOrRegister(node: PhraseNode) {
        // Recursively replace or register the last added child of the current node.
        val child = node.lastAdded
        if (child.linkMap.size != 0)
            replaceOrRegister(child)

        // Get the possible matches for the last child.
        val candidateChildren = register.get(child.label)
        // Select only the registry node which has an exact match to the last child.  We can also replace this equivalence check for a
        // subsumption check later on to condence the trie even more while breaking the automata contract.
        candidateChildren.filter(matchMethod(_, child)) match {
            // If such a child exists, merge the counts of the last child to the existing child and link the parent to the existing child.
            case existingChild :: tail => 
                existingChild.addCount(child.inCount)
                node.lastAdded = existingChild
                node.linkMap += (child.label -> existingChild)
            // If no chld exists, put the last child in the registery.
            case _ => register.put(child.label, child)
        }
    }

    def toJson = {
        val nodeLabels = new ArrayBuffer[(String, Double)]()
        val links = new ArrayBuffer[(Int, Int)]()
        addLinksAndLabels(root, new ObjectIndexer[PhraseNode](), new HashSet[PhraseNode](), nodeLabels, links)
        "{\n" +
        " \"nodes\": [\n" +
        nodeLabels.map{ case(label, weight) => "  {\"name\":\"%s\",\"weight\":%f},\n".format(label.replaceAll("\"", "'"), weight)}.mkString("") +
        " ],\n" +
        " \"links\":[\n" +
        links.map{ case(from, to) => "  {\"source\":%d,\"target\":%d,\"value\":10},\n".format(from, to)}.mkString("") +
        " ]\n" +
        "}"
    }

    def addLinksAndLabels(node: PhraseNode,
                          indexer: ObjectIndexer[PhraseNode], 
                          printed: HashSet[PhraseNode], 
                          labels: Buffer[(String, Double)], 
                          links: Buffer[(Int, Int)]) {
        if (printed.contains(node)) {
            println("already printed: " + node.label)
        } else {
            println("printing first of: " + node.label)
            labels.append((node.label, node.inCount))
            val n_index = indexer.index(node)
            printed.add(node)
            node.linkMap.map{ case(_, child ) => {
                val c_index = indexer.index(child)
                links.append( (n_index, c_index) )
                addLinksAndLabels(child, indexer, printed, labels, links)
            }}
        }
    }

    override def toString : String =
        "digraph CondensedTrie {\n" +
        "rankdir=LR;" +
        toString(root, new ObjectIndexer[PhraseNode](), new HashSet[PhraseNode]()) + 
        "}\n" 

    def toString(node: PhraseNode, indexer: Indexer[PhraseNode], printed: HashSet[PhraseNode]) : String = {
        if (printed.contains(node)) {
            println("already printed: " + node.label)
            ""
        } else {
            println("printing first of: " + node.label)
            val printLabel = node.label.replaceAll("[\\W]+", "") + indexer.index(node)
            printed.add(node)
            node.linkMap.map{ case(childLabel, child ) => {
                val printChildLabel = childLabel.replaceAll("[\\W]+", "") + indexer.index(child)
                "  %s -> %s; \n".format(printLabel, printChildLabel) +
                toString(child, indexer, printed)
            }}.mkString("") +
            "  %s [ label=\"%s, %d\" ]; \n".format(printLabel, node.label.replaceAll("[\\W+]", ""), node.inCount.toInt)
        }
    }
}

/**
 * A simple node structure that records a label, a weight, and a mapping from this node to other nodes using labeled arcs.  This
 * implementation overrides {@link hashCode} and {@link equals} such that only nodes with the same label and which point to the same exact
 * children (i.e.  same objects, not equivalent objects), are considered equal.
 */
class PhraseNode(val label: String) {

    /**
     * The internal weight for this {@link PhraseNode}.
     */
    var inCount = 0d

    /**
     * A mapping from this {@link PhraseNode} to children {@link PhraseNode}s using labeled arcs.
     */
    var linkMap = Map[String,PhraseNode]()

    /**
     * A record of the last {@link PhraseNode} added as a child to this {@link PhraseNode}.
     */
    var lastAdded:PhraseNode = null

    /**
     * Returns the {@link PhraseNode} connected to {@code this} {@link PhraseNode} via the arc {@code term}.  If no such node exists, a new
     * {@link PhraseNode} is created and returned.
     */
    def neighbor(term: String) =
        linkMap.get(term) match {
            case Some(node) => node
            case None => { lastAdded = new PhraseNode(term)
                           linkMap += (term -> lastAdded)
                           lastAdded
                         }
        }

    /**
     * Adds {@code delta} to the {@code inCount} and returns a pointer to {@code this} {@link PhraseNode}.
     */
    def addCount(delta: Double = 1) = {
        inCount += delta
        this
    }

    def toString(indent: String) : String  =
        linkMap.map{ case (term, child) =>
            indent + term + "\n" + child.toString(indent + "  ")
        }.mkString("")

    /**
     * Returns a hashcode based on java's internal hash code method for every object which uniquely identifies every object.
     */
    def pointerHashCode = super.hashCode

    /**
     * Override {@code hashCode} to use three factors:
     * <ol>
     *  <li>The hash code for {@code label}</li>
     *  <li>The hash code for {@code label} of each child node</li>
     *  <li>The hash code for {@code pointer} of each child node</li>
     * </ol>
     * This ensures that nodes only have the same hash code if they have the same label, same number of children, same links to those
     * children, and point to the very same children.  This is a cheap and fast way to ensure that we don't accidently consider two nodes
     * with the same link labels aren't equivalent.
     */
    override def hashCode = 
        linkMap.map{ case(childLabel, child) =>
            childLabel.hashCode ^ child.pointerHashCode
        }.foldLeft(label.hashCode)(_^_)

    /**
     * Override {@code equals} to use the same three factors as {@cod hachCode}:K
     * <ol>
     *  <li>The {@code label}</li>
     *  <li>The {@code label} of each child node</li>
     *  <li>The {@code pointer} of each child node</li>
     * </ol>
     * 
     * This ensures that nodes only equal when they have the same distinguishing meta data and point to the same children.
     */
    override def equals(that: Any) =
        that match {
            case other: PhraseNode => if (this.hashCode != other.hashCode) false
                                      else if (this.label != other.label) false
                                      else compareLinkMaps(this.linkMap, other.linkMap)
            case _ => false
        }

    /**
     * Returns true if the two maps have the same size, same keys, and the key in each map points to the same object.  We use this instead
     * of simply calling equals between the two maps because we want to check node equality using just the pointer hash code, which prevents
     * walking down the entire graph structure from each node.
     */
    def compareLinkMaps(lmap1: Map[String, PhraseNode], lmap2: Map[String, PhraseNode]) : Boolean = {
        if (lmap1.size != lmap2.size)
            return false
        for ( (key1, entry1) <- lmap1 ) {
            val matched = lmap2.get(key1) match {
                case Some(entry2) => entry2.pointerHashCode == entry1.pointerHashCode
                case None => false
            }
            if (!matched)
                return false
        }
        true
    }
}

object PhraseGraph {
    def main(args: Array[String]) {
        val sentences = List(
            "#archery zoidberg by the Republic of Korea in archery by a guy who is legally blind",
            "#archery by the Republic of Korea in archery by a guy who is legally blind",
            "#archery by the Republic of Korea and by the guy is legally blind",
            "Republic of Korea in archery by a guy who is legally blind",
            "#archery zoidberg by the Republic of Korea and by the guy is legally blind",
            "#archery by the Republic of Korea and the guy is legally blind"
        )

        val tokenizedSentences = sentences.map(_.split("\\s+").toList)

        {
        val condensedTrie = new CondensedTrie(false)
        condensedTrie.train(tokenizedSentences)
        val writer = new PrintWriter(args(0))
        writer.println(condensedTrie)
        writer.close
        }
        {
        val condensedTrie = new CondensedTrie(true)
        condensedTrie.train(tokenizedSentences)
        val writer = new PrintWriter(args(1))
        writer.println(condensedTrie)
        writer.close
        }

        /*
        val tokenBasis = args(0)
        val neBasis = args(1)
        val converter = TweetModeler.load("joint", tokenBasis, neBasis)
        val tweetIterator = converter.tweetIterator(args(2))
        val tweets = tweetIterator.toList
        val tokenizedTweets = tweets.map(_.text).map(converter.tokenize)
        val hashTagSet = converter.hashTagSet(tweets)
        val namedEntitySet = converter.namedEntitySet(tweets)
        val totalSet = hashTagSet ++ namedEntitySet
        val phraseGraph = new PhraseGraph2()
        phraseGraph.train(tokenizedTweets, totalSet)
        for ( (term, trie) <- phraseGraph.leftGraphs;
               if trie.root.linkMap.size > 0 ) {
            val writer = new PrintWriter(args(3) + ".left." + term.replaceAll(" ", "_") + ".dot")
            writer.println(trie)
            writer.close
        }
        for ( (term, trie) <- phraseGraph.rightGraphs;
               if trie.root.linkMap.size > 0 ) {
            val writer = new PrintWriter(args(3) + ".right." + term.replaceAll(" ", "_") + ".dot")
            writer.println(trie)
            writer.close
        }
        */
    }
}
