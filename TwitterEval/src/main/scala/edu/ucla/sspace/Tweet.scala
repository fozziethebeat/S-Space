package edu.ucla.sspace

import edu.ucla.sspace.similarity.SimilarityFunction
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorMath

import scala.math.abs
import scala.math.pow


class Tweet(val timestamp: Long,
            val tokenVector: SparseDoubleVector,
            val neVector: SparseDoubleVector,
            val text: String) {

    def +(t: Tweet) = {
        VectorMath.add(tokenVector, t.tokenVector)
        VectorMath.add(neVector, t.neVector)
        Tweet(timestamp + t.timestamp, tokenVector, neVector, text)
    }
}

object Tweet {

    def apply() = new Tweet(0, new CompactSparseVector(), new CompactSparseVector(), "")

    def apply(ts: Long, tVec: SparseDoubleVector, neVec: SparseDoubleVector, text: String) =
        new Tweet(ts, tVec, neVec, text)

    def sim(t1: Tweet, t2: Tweet,
            lambda: Double, beta: Double, weights: (Double, Double, Double),
            simFunc: SimilarityFunction) =
        weights._1*pow(lambda, abs(t1.timestamp - t2.timestamp)/beta) + // Time
        weights._2*simFunc.sim(t1.tokenVector, t2.tokenVector) + // Topic
        weights._3*simFunc.sim(t1.neVector, t2.neVector) // Named Entities
}
