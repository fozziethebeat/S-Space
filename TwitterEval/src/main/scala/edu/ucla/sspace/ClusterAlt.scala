package edu.ucla.sspace

import edu.ucla.sspace.clustering.DirectClustering

import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.clustering.Partition

import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList

import java.io.PrintWriter


object ClusterAlt {
    def main(args: Array[String]) {
        if (args.size != 6) {
            println("args: <tweets> <basis1> <basis2> <modelType> <nclusters> <group.out>")
            System.exit(1)
        }

        // Extract the tweet representations and put them in a breeze format.
        println("Loading the tweet model")
        val modeler = TweetModeler.split(args(1), args(2))
        println("Extracting the tweets")
        val tweetVectors = modeler.tweetIterator(args(0)).map(_.tokenVector).toList
        val data = Matrices.asSparseMatrix(tweetVectors)

        // Get the clustering algorithm that will be applied to the group of tweets.
        val nGroups = args(4).toInt
        val learner = args(3) match {
            case "kmean" => new DirectClustering()
        }

        println("Clustering the tweets")
        val assignments = learner.cluster(data, nGroups, System.getProperties)
        val p = new PrintWriter(args(5))
        p.println("Group")
        assignments.assignments.map(_(0)).foreach(p.println)
        p.close
    }
}
