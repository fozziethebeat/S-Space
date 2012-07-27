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
import scala.util.Random

import java.io.PrintWriter


object ParticleFilterTweets {

    type Particle = (List[Int], Int, List[Tweet], Double, Double, Double)

    // Create a distribution for sampling lambda parameters for each particle.
    val lambda_dist = new Beta(2, 2)
    // Create a distribution for alpha prameters for each particle.
    val alpha_dist = new Gamma(1, 1)
    // Create a distribution for beta parameters used to scale the time decay.
    val beta_dist = new Gamma(1, 1)
    // Create a similarity method for comparing feature representations of tweets.
    val simFunc = new CosineSimilarity()
    // Create a set of weights for the tweet similarity function.  These should sum to 1.
    val w = (.7, .2, .1)

    def main(args: Array[String]) {
        if (args.size != 7) {
            System.err.println("Arguments: <tweets.txt> <tokenBasis> <neBasis> <labeled.out> <slices.out> <summaries.out> <topFeatures.out>")
            System.exit(1)
        }

        // Specify the number of particles to create.
        val nParticles = 50

        val converter = TweetModeler.joint(args(1), args(2), 4)
        val tweets = converter.tweetIterator(Source.fromFile(args(0)).getLines).toList
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
                                                                            (List(0), 1, List(firstPoint),
                                                                            lambda_dist.sample, beta_dist.sample*100, alpha_dist.sample)))

        // Process each data point by comparing it to each particle.  For each particle, determine if the point should be assigned to the
        // most recently created component or be allocated a new component.
        for (point <- tweetIter) {
            val newParticleList = particleList.map(p => selectComponent(point, p._2))
            val particleWeights = new DenseVector[Double](newParticleList.map(_._1))
            val particleDist = new Multinomial(particleWeights / particleWeights.sum)
            particleList = Array.fill(nParticles)(newParticleList(particleDist.sample))
        }

        val bestParticle = particleList.sortWith(_._1 > _._1).head
        val p = new PrintWriter(args(3))
        p.println("Time Group")
        bestParticle._2._1.zip(tweets).foreach(x => p.println("%d %d".format(x._2.timestamp, x._1)))
        p.close

        val t = new PrintWriter(args(4))
        t.println("Time")
        bestParticle._2._3.map(_.timestamp).foreach(t.println)
        t.close

        // For each group, compute the best median within the group.  This will simply be the tweet that maximizes the internal similarity
        // within the group.

        val (z, n, times, lambda, beta, alpha) = bestParticle._2 
        val summaryList = z.zip(tweets)
                           .groupBy(_._1)
                           .map{ case (groupId, group) => {
            val points = group.map(_._2)
            val bestMedian = points.map(pmedian => points.map(point => Tweet.sim(pmedian, point, lambda, beta, w, simFunc)).sum)
                                   .zipWithIndex
                                   .max._2
            points(bestMedian)
        }}.toList.sortWith(_.timestamp < _.timestamp).map(_.text)

        val s = new PrintWriter(args(5))
        s.println("Summary")
        summaryList.foreach(s.println)
        s.close

        // Weight the features using PMI so that we select the most saliently interesting features per group as opposed to just the most
        // frequent.
        val transform = new PointWiseMutualInformationTransform()
        val clusterMatrix = Matrices.asSparseMatrix(bestParticle._2._3.map(_.tokenVector))
        val weightedMatrix = transform.transform(clusterMatrix)

        val m = new PrintWriter(args(6))
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

    def selectComponent(point: Tweet, particle: Particle) = {
        val (z, n, t, lambda, beta, alpha) = particle
        val existingLikelihood = n / (n + alpha) * // Chinese Restaurant Process
                                 Tweet.sim(point, t.last, lambda, beta, w, simFunc)
        val newLikelihood = alpha / (n + alpha) * 1
        val total = existingLikelihood + newLikelihood
        val existingProb = existingLikelihood / total
        val newProb = newLikelihood / total
        if (Random.nextDouble <= existingProb) {
            val newId = t.size -1 
            VectorMath.add(t.last.tokenVector, point.tokenVector)
            VectorMath.add(t.last.neVector, point.neVector)
            (existingProb, (z :+ newId, n+1, t, lambda, beta, alpha))
        } else {
            val newId = t.size
            val newTweet = new Tweet(point.timestamp, 
                                     new CompactSparseVector(point.tokenVector.length),
                                     new CompactSparseVector(point.neVector.length),
                                     point.text)
            VectorMath.add(newTweet.tokenVector, point.tokenVector)
            VectorMath.add(newTweet.neVector, point.neVector)
            (newProb, (z :+ newId, 1, t :+ newTweet, lambda, beta, alpha))
        }
    }
}
