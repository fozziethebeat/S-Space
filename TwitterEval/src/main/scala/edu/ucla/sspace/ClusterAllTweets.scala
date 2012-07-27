package edu.ucla.sspace

import breeze.linalg.DenseVector
import breeze.linalg.NormCacheVector
import breeze.linalg.SparseVector
import breeze.linalg.Vector

import edu.ucla.sspace.graphical.em.KMedianMean
import edu.ucla.sspace.graphical.gibbs.InfiniteSphericalGaussianMixtureModel
import edu.ucla.sspace.graphical.gibbs.FiniteSphericalGaussianMixtureModel
import edu.ucla.sspace.graphical.SphericalGaussianRasmussen
import edu.ucla.sspace.graphical.DistanceMetrics.euclidean

import scala.math.pow

import java.io.PrintWriter


/**
 * This executible aims to cluster tweets into topical categories, for instance, if clustering tweets on the olympics, the goal should be to
 * group tweets based on their sport.  Later time series analysis can then be done for each grouping of tweets to determine relevant and
 * interesting events that can be summarized.  In order to focus on topical content, this ignores the person based named entities.  It does
 * utilize locations and organization named entites however. 
 */
object ClusterAllTweets {
    def main(args: Array[String]) {
        if (args.size != 6) {
            println("args: <tweets> <basis1> <basis2> <modelType> <nclusters> <group.out>")
            System.exit(1)
        }

        // Extract the tweet representations and put them in a breeze format.
        println("Loading the tweet model")
        val modeler = TweetModeler.split(args(1), args(2))
        println("Extracting the tweets")
        val tweetVectors = modeler.tweetIterator(args(0)).map(convertToBreeze).toList

        // Get the clustering algorithm that will be applied to the group of tweets.
        val nGroups = args(4).toInt
        val nTrials = 1000
        val learner = args(3) match {
            case "igmm" => new InfiniteSphericalGaussianMixtureModel(nTrials, 1, getGenerator(tweetVectors))
            case "gmm" => new FiniteSphericalGaussianMixtureModel(nTrials, 1, getGenerator(tweetVectors))
            case "kmean" => new KMedianMean(true)
            case "kmedian" => new KMedianMean(false)
        }

        // Group the tweets into interesting categories and then print out their group assignments.
        println("Clustering the tweets")
        val assignments = learner.train(tweetVectors, nGroups)
        val p = new PrintWriter(args(5))
        p.println("Group")
        assignments.foreach(p.println)
        p.close
    }

    /**
     * Given a tweet, this will extract the token vector representation and reformulate it as a Breeze vector.
     */
    def convertToBreeze(tweet: Tweet) = {
        val tokenVector = tweet.tokenVector
        val breezeVersion = SparseVector[Double](tokenVector.length)()
        for (i <- tokenVector.getNonZeroIndices)
            breezeVersion(i) = tokenVector.get(i)
        breezeVersion
    }

    /**
     * Given a list of data points, this will create the Component Generator needed for some of the learning algorithms.
     */
    def getGenerator(data: List[Vector[Double]]) = {
        val mu = new DenseVector((data.reduce(_+_) / data.size.toDouble).valuesIterator.toArray) with NormCacheVector[Double]
        val variance = data.map(euclidean(mu, _)).map(pow(_,2)).sum / data.size.toDouble
        new SphericalGaussianRasmussen(mu,  variance)
    }
}
