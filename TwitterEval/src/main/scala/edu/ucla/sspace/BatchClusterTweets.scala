package edu.ucla.sspace

import edu.ucla.sspace.similarity.CosineSimilarity

import scala.collection.JavaConversions.seqAsJavaList
import scala.util.Random

import java.io.PrintWriter


object BatchClusterTweets {
    val lambda = 0.5
    val beta = 100
    val w = (0.45, 0.45, 0.10)
    val simFunc = new CosineSimilarity()
    var useMedian = false 
    var counter = 300

    def main(args: Array[String]) {
        val taggedFile = args(0)
        val tokenBasisFile = args(1)
        val neBasisFile = args(2)
        val numGroups = args(3).toInt
        val groupOutput = args(4)
        val summaryOutput = args(5)
        val featureModel = args(6)
        val medianArg = args(7)

        def sim(t1: Tweet, t2: Tweet) = Tweet.sim(t1, t2, lambda, beta, w, simFunc)

        useMedian = medianArg match {
            case "median" => true
            case "mean" => false 
            case _ => throw new IllegalArgumentException("Not a valid argument for the median method")
        }

        val converter = featureModel match {
            case "split" => TweetModeler.split(tokenBasisFile, neBasisFile)
            case "joint" => TweetModeler.joint(tokenBasisFile, neBasisFile)
            case _ => throw new IllegalArgumentException("Not a valid argument for the Tweet Modeler")
        }

        val tweetArray = converter.tweetIterator(taggedFile).toArray
        val tweets = tweetArray.toList
        printf("Processing [%d] tweets\n", tweets.size)

        val k = numGroups
        val assignments = Array.fill(tweets.size)(-1)
        // Extract a random set of tweets to act as medians.
        var medianList = selectMedians(tweets, k)
        var medianUpdated = true

        while (medianUpdated && counter > 0) {
            printf("Starting iteration [%d]\n", counter)
            // Assign the first set of tweets, i.e. those before the first median, to the first median.
            val (firstGroup, remainingTweets) = tweets.span(_.timestamp <= medianList.head.timestamp)
            var offset = assignTweets(firstGroup, assignments, 0, 0)

            // Iterate through each pairing of medians to compute the best cut point between each pair.  After computing the best cut point,
            // assign the tweets to their nearest medians.
            val lastItems = medianList.zipWithIndex
                                      .sliding(2)
                                      .foldLeft(remainingTweets)( (tweetList, medianPair) => {
                // Get the medians and their group identifiers.
                val ((m1, i1), (m2, i2)) = (medianPair.head, medianPair.last)
                // Get the sequence of tweets between the two medians.  Span will do this since the list starts just after the first median.
                val (window, rest) = tweetList.span(_.timestamp <= m2.timestamp)
                // Compute the object for the first possible cut location, i.e. everything is assigned to m2.
                var objectiveValue = sim(m1, m1) + window.map(t=>sim(t, m2)).sum
                // For each possible cut position, update the objective function and emit the value and timestamp.  After considering every
                // possible cut location, find the one with the highest objective score.
                var bestTime = window.map( t => {
                    // Updated objective value.
                    objectiveValue = objectiveValue - sim(t, m2) + sim(t, m1)
                    // Emiting value and timestamp of the cut.
                    (objectiveValue, t.timestamp)
                }).max._2

                // It's possible that the best cut point is at the same time as
                // the end median of this segment.  In that case, don't let that
                // get assigned to the previous group.
                if (bestTime == m2.timestamp)
                    bestTime = m2.timestamp - 1

                // With the cut point, get the group of points assigned to each median.
                val (g1, g2) = window.span(_.timestamp <= bestTime)
                // Make the assignment of points to medians.
                offset = assignTweets(g1, assignments, i1, offset)
                offset = assignTweets(g2, assignments, i2, offset)

                // The rest list contains all points after the second median.  Use this for future processing.
                rest
            })
            // Assign the last set of tweets, i.e. those after the last median, to the last median.
            assignTweets(lastItems, assignments, k-1, offset)

            // For each group, compute the best median within the group.  This will simply be the tweet that maximizes the internal similarity
            // within the group.
            val newMedianList = assignments.zip(tweets)
                       .groupBy(_._1)
                       .map{ case (medianIndex, group) => {
                val points = group.map(_._2)
                if (useMedian) {
                      points(points.map(pmedian => points.map(point => sim(pmedian, point)).sum)
                                   .zipWithIndex.max._2)
                } else {
                    val median = points(points.map(pmedian => points.map(point => sim(pmedian, point)).sum)
                                                .zipWithIndex.max._2)
                    val mean = points.foldLeft( converter.emptyTweet )( _+_ )
                    new Tweet(median.timestamp, mean.tokenVector, mean.neVector, median.text)
                }
            }}.toList.sortWith(_.timestamp < _.timestamp)

            medianUpdated = newMedianList.map(_.timestamp) != medianList.map(_.timestamp)
            medianList = newMedianList
            counter -= 1
        }

        val p = new PrintWriter(groupOutput)
        p.println("Time Group")
        assignments.zip(tweets).foreach(x => p.println("%d %d".format(x._2.timestamp, x._1)))
        p.close

        /*
        val s = new PrintWriter(summaryOutput)
        s.println("Summary")
        medianList.zipWithIndex.foreach{ case(medianTweet, groupId) => {
            val startTime = medianTweet.timestamp
            val meanTime = medianTweet.timestamp
            val summary = medianTweet.text
            s.println("%d %d %d \"%s\"".format(startTime, meanTime, groupId, summary))
        }}
        s.close

        val transform = new PointWiseMutualInformationTransform()
        val clusterMatrix = Matrices.asSparseMatrix(medianList.map(_.tokenVector))
        val weightedMatrix = transform.transform(clusterMatrix)

        val m = new PrintWriter(config.featureOutput.get)
        m.println("Top Words")
        medianList.map(_.tokenVector).map(v => v.getNonZeroIndices
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

    def assignTweets(items: List[Tweet], assignments: Array[Int], groupId: Int, offset: Int) = {
        for (i <- 0 until items.size)
            assignments(offset+i) = groupId
        offset + items.size
    }

    def selectMedians(tweets:List[Tweet], k:Int) : List[Tweet] = {
        while (true) {
            val medians = Random.shuffle(tweets).take(k).sortWith(_.timestamp < _.timestamp)
            // If we don't have enough tweets for k medians, just return the
            // median list.
            if (tweets.size < k) return medians
            // Check that we have k unique timestamps.  If we do, return the
            // medians.
            if (medians.map(_.timestamp).toSet.size == k) return medians
            // Otherwise continue
        }
        // Do this so horrible horrible things happen otherwise.
        return null
    }
}
