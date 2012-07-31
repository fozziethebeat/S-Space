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
    var uniformTokenVector:CompactSparseVector = null
    var uniformNeVector:CompactSparseVector = null

    def main(args: Array[String]) {
        val config = Config(args(0))

        // Specify the number of particles to create.
        val nParticles = 50

        val converter = config.featureModel.get match {
            case "split" => TweetModeler.split(config.tokenBasis.get, config.neBasis.get)
            case "joint" => TweetModeler.joint(config.tokenBasis.get, config.neBasis.get, config.ngramSize.get)
        }
        val tweets = converter.tweetIterator(config.taggedFile.get).toList
        val tweetIter = tweets.iterator

        // Read in the first point to initialize each particle.
        val firstPoint = tweetIter.next

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

        // Process each data point by comparing it to each particle.  For each particle, determine if the point should be assigned to the
        // most recently created component or be allocated a new component.
        for (point <- tweetIter) {
            val newParticleList = particleList.map(p => selectComponent(point, p._2, converter))
            val particleWeights = new DenseVector[Double](newParticleList.map(_._1))
            val particleDist = new Multinomial(particleWeights / particleWeights.sum)
            particleList = Array.fill(nParticles)(newParticleList(particleDist.sample))
        }

        val bestParticle = particleList.sortWith(_._1 > _._1).head
        val p = new PrintWriter(config.groupOutput.get)
        p.println("Time Group")
        bestParticle._2._1.zip(tweets).foreach(x => p.println("%d %d".format(x._2.timestamp, x._1)))
        p.close

        val t = new PrintWriter(config.splitOutput.get)
        t.println("Time")
        bestParticle._2._3.map(_.timestamp).foreach(t.println)
        t.close

        // For each group, compute the best median within the group.  This will simply be the tweet that maximizes the internal similarity
        // within the group.

        val (z, n, times, _, _, lambda, beta, alpha) = bestParticle._2 
        val summaryList = z.zip(tweets)
                           .groupBy(_._1)
                           .map{ case (groupId, group) => {
            val points = group.map(_._2)
            val bestMedian = points.map(pmedian => points.map(point => Tweet.sim(pmedian, point, lambda, beta, w, simFunc)).sum)
                                   .zipWithIndex
                                   .max._2
            points(bestMedian)
        }}.toList.sortWith(_.timestamp < _.timestamp).map(_.text)

        val s = new PrintWriter(config.summaryOutput.get)
        s.println("Summary")
        summaryList.foreach(s.println)
        s.close

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
                                 Tweet.sim(point, t.avgTime(n), lambda, beta, currWeights, simFunc)
        val newLikelihood = alpha / (n + alpha) * // Chinese Restaurant Process
                            Tweet.sim(point, noiseTweet, lambda, beta, currWeights, simFunc)
        val total = existingLikelihood + newLikelihood
        val existingProb = existingLikelihood / total
        val newProb = newLikelihood / total
        if (Random.nextDouble <= existingProb) {
            val newId = t_list.size -1 
            (existingProb, (z :+ newId, (n+1) :: n_rest, (t+point) :: t_rest, simSum, objectiveScore, lambda, beta, alpha))
        } else {
            val newId = t_list.size
            val newTweet = (new Tweet(point.timestamp, 
                                     new CompactSparseVector(point.tokenVector.length),
                                     new CompactSparseVector(point.neVector.length),
                                     point.text) + point).avgTime(2)
            (newProb, (z :+ newId, 1 :: n_list, newTweet :: t_list, simSum, objectiveScore, lambda, beta, alpha))
        }
    }
}
