package edu.ucla.sspace

import breeze.linalg.DenseVector
import breeze.stats.distributions.Beta
import breeze.stats.distributions.Gamma
import breeze.stats.distributions.Multinomial

import edu.ucla.sspace.matrix.Matrices
import edu.ucla.sspace.matrix.PointWiseMutualInformationTransform
import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.similarity.JaccardIndex
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorMath

import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Source
import scala.math.pow
import scala.util.Random

import java.io.PrintWriter


object ParticleFilterTweets {

    type Particle = (List[Int], // A list of cluster assignments for each point.
                     List[Int], // Number of points assigned to each cluster.
                     List[Tweet], // List of centroid tweets.
                     Double, // The current sum similarities between points and their centers.
                     Double, // The total objective score.
                     Double, // The lambda parameter in the time comparison.
                     Double, // The beta parameter in the time comparison.
                     Double // The alpha parameter in the dirichlet process.
                     )

    // Create a distribution for sampling lambda parameters for each particle.
    val lambda_dist = new Beta(2, 2)
    // Create a distribution for alpha prameters for each particle.
    val alpha_dist = new Gamma(1, 1)
    // Create a distribution for beta parameters used to scale the time decay.
    val beta_dist = new Gamma(1, 1)
    // Create a similarity method for comparing feature representations of tweets.
    val simFunc = new CosineSimilarity()
    // Create a set of weights for the tweet similarity function.  These should sum to 1.
    val w = (0.45, 0.45, .1)
    val ws = (0.45, 0.40, 0.05)

    // Specify the number of particles to create.
    val nParticles = 100
    var uniformTokenVector:CompactSparseVector = null
    var uniformNeVector:CompactSparseVector = null

    def main(args: Array[String]) {
        val taggedFile = args(0)
        val tokenBasisFile = args(1)
        val neBasisFile = args(2)
        val groupOutput = args(3)
        val summaryOutput = args(4)
        val featureModel = args(5)

        println("Loading configurations")
        val converter = TweetModeler.load(featureModel, tokenBasisFile, neBasisFile)
        val tweets = converter.tweetIterator(taggedFile).toList
        val tweetIter = tweets.iterator

        // Read in the first point to initialize each particle.
        val firstPoint = tweetIter.next

        println("Initializing Particles")
        // Create a list of particles.  Each particle will have the following parameters and a weight:
        //   paramters:
        //     1) z: assignments for each data point to a component id.  Note that these will be contiguous.
        //     2) n: the number of assignments made to each component.
        //     3) t: timestamps of the first element in each component.
        //     4) lambda: the exponential decay factor for difference in time values.
        //     5) beta: a scaling factor for the exponential decay
        //     6) alpha: smoothing parameter given to a new mixture in a Dirichlet Process.
        // Initially each particle has the first data point assigned to the first group
        var particleList:Array[(Double, Particle)] = Array.fill(nParticles)((1/nParticles.toDouble,
                                                                            (List(0), List(1), List(firstPoint),
                                                                            0d, 0d,
                                                                            lambda_dist.sample, beta_dist.sample*100, alpha_dist.sample)))

        println("Processing Tweets")
        // Process each data point by comparing it to each particle.  For each particle, determine if the point should be assigned to the
        // most recently created component or be allocated a new component.
        for ((point, i) <- tweetIter.zipWithIndex) {
            if (i % 100 == 0)
                printf("Processing Tweet [%d]\n", i)
            val newParticleList = particleList.map(p => selectComponent(point, p._2, converter))
            val particleWeights = new DenseVector[Double](newParticleList.map(_._1).map(p=> if (p < 0) .0001 else p))
            val particleDist = new Multinomial(particleWeights / particleWeights.sum)
            particleList = Array.fill(nParticles)(newParticleList(particleDist.sample))
        }

        println("Printing Assignments for each tweet")
        val bestParticle = particleList.sortWith(_._1 > _._1).head._2
        val (assignments, clustersizes, meanTweets, _, _, lambda, beta, alpha) = bestParticle

        val p = new PrintWriter(groupOutput)
        p.println("Time Group")
        assignments.zip(tweets).foreach(x => p.println("%d %d".format(x._2.timestamp, x._1)))
        p.close

        /*
        // For each group, compute the best median within the group.  This will simply be the tweet that maximizes the internal similarity
        // within the group.
        val summaryList = assignments.zip(tweets)
                                     .groupBy(_._1)
                                     .map{ case (groupId, group) => {
            (groupId, Tweet.medianSummary(group.map(_._2), Tweet.sim(_, _, lambda, beta, w, simFunc)))
        }}.toMap

        println("Printing summaries and time stamps for each group")
        val t = new PrintWriter(summaryOutput)
        t.println("StartTime MeanTime Group Summary")
        meanTweets.zipWithIndex.foreach{ case(meanTweet, groupId) => {
            val medianTweet = summaryList(groupId)
            val startTime = meanTweet.timestamp
            val meanTime = medianTweet.timestamp
            val summary = medianTweet.text
            t.println("%d %d %d \"%s\"".format(startTime, meanTime, groupId, summary))
        }}
        t.close

        println("Done!")
        // Weight the features using PMI so that we select the most saliently interesting features per group as opposed to just the most
        // frequent.
        val transform = new PointWiseMutualInformationTransform()
        val clusterMatrix = Matrices.asSparseMatrix(bestParticle._2._3.map(_.tokenVector))
        val weightedMatrix = transform.transform(clusterMatrix)

        val m = new PrintWriter(config.featureOutput.get)
        m.println("Top Words")
        times.map(_.tokenVector).map(v => v.getNonZeroIndices
                                                        .map(i => (v.get(i), i))
                                                        .sorted
                                                        .reverse
                                                        .take(10)
                                                        .map(_._2)
                                                        .map(converter.token)
                                                        .mkString(" "))
                                             .foreach(m.println)
        m.close
        */
    }

    def weightGen(n: Int) = 
        normedWeights(
        (w._1 + ws._1 * pow(.9, n),
         w._2 - ws._2 * pow(.9, n),
         w._3 - ws._3 * pow(.9, n)))

    def normedWeights(w: (Double, Double, Double)) = {
        val total = w._1 + w._2 + w._3
        (w._1 / total, w._2 / total, w._3 / total)
    }

    def selectComponent(point: Tweet, particle: Particle, converter: TweetModeler) = {
        val (z, n_list, t_list, simSum, objectiveScore, lambda, beta, alpha) = particle
        val (n, n_rest) = (n_list.head, n_list.tail)
        val (t, t_rest) = (t_list.head, t_list.tail)
        val noiseTweet = converter.uniformTweet(point.timestamp)
        val currWeights = weightGen(n)
        val existingLikelihood = n / (n + alpha) * // Chinese Restaurant Process
                                 Tweet.sim(point, t, lambda, beta, currWeights, simFunc)
        val newLikelihood = alpha / (n + alpha) * // Chinese Restaurant Process
                            Tweet.sim(point, noiseTweet, lambda, beta, currWeights, simFunc)
        val total = existingLikelihood + newLikelihood
        val existingProb = existingLikelihood / total
        val newProb = newLikelihood / total
        if (Random.nextDouble <= existingProb) {
            val newId = t_list.size -1 
            val newMean = t +? point
            val objective = simSum + Tweet.sim(point, newMean, lambda, beta, currWeights, simFunc)
            (objective, (z :+ newId, (n+1) :: n_rest, newMean :: t_rest, objective, objectiveScore, lambda, beta, alpha))
        } else {
            val newId = t_list.size
            val newTweet = Tweet(point)
            val penalty = t_rest.map(Tweet.sim(_, t, lambda, beta, w, simFunc)).sum
            val objective = simSum + 1 - 0.5 * penalty
            (objective, (z :+ newId, 1 :: n_list, newTweet :: t_list, objective, objectiveScore, lambda, beta, alpha))
        }
    }
}
