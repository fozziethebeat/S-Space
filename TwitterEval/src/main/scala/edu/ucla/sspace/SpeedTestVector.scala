package edu.ucla.sspace

import common.Similarity

import breeze.linalg.DenseVector
import breeze.linalg.NormCacheVector
import breeze.linalg.SparseVector
import breeze.linalg.Vector

import scala.math.{pow,sqrt}
import scala.util.Random


object SpeedTestVector {
    def main(args: Array[String]) {
        val nDim = args(0).toInt
        val dva = Array.fill(nDim)(Random.nextGaussian)

        // S-Space dense vector.
        val sdv = new vector.DenseVector(dva)
        // Breeze dense vector.
        val bdv: Vector[Double] = new DenseVector(dva)
        // Breeze dense vector with caching.
        val bdvc: Vector[Double] = new DenseVector(dva) with NormCacheVector[Double]

        val ssv = new vector.CompactSparseVector(nDim)
        val bsv: SparseVector[Double] = SparseVector[Double](nDim)()
        val indices = (for ( i <- Random.shuffle(0 until nDim).take(30) ) yield {
            val v = Random.nextGaussian
            ssv.set(i, v)
            bsv(i) = v
            i
        }).toList

        time("Time for breeze vector without cache", () => {
            for (i <- 0 until 10000)
                euclidean(bdv, bsv)
        })
        time("Time for breeze vector with cache", () => {
            for (i <- 0 until 10000)
                euclidean(bdvc, bsv)
        })
        time("Time for S-Space", () => {
            for (i <- 0 until 10000)
                Similarity.euclideanDistance(sdv, ssv)
        })
        time("Time for S-Space Sparse access", () => {
            for (t <- 0 until 10000; i <- indices)
                ssv.get(i)
        })
        time("Time for S-Space Sparse iteration", () => {
            for (t <- 0 until 10000; i <- ssv.getNonZeroIndices)
                ssv.get(i)
        })
        time("Time for Breeze Sparse access", () => {
            for (t <- 0 until 10000; i <- indices)
                bsv(i)
        })
        time("Time for Breeze Sparse mapActiveValues", () => {
            for (t <- 0 until 10000)
                bsv.mapActivePairs( (i,v) => v )
        })
        time("Time for Breeze noCache norm", () => {
            for (t <- 0 until 10000)
                bdv.norm(2)
        })
        time("Time for Breeze Cache norm", () => {
            for (t <- 0 until 10000) {
                val m = pow(bdvc.norm(2), 2)
                var dist = 0d
                bsv.mapActivePairs( (i, y) => {
                    dist += y
                    y
                })
                sqrt(m+dist)
            }
        })
        time("Time for S-Space norm", () => {
            for (t <- 0 until 10000)
                sdv.magnitude
        })
    }

    def time(msg: String, func: () => Unit) {
        val start = System.currentTimeMillis
        func()
        val end = System.currentTimeMillis
        println(msg + " " + (end-start))
    }

    def euclidean(v1: Vector[Double], v2: SparseVector[Double]) : Double = {
        var v1Magnitude = pow(v1.norm(2), 2)
        var dist = 0d
        v2.mapActivePairs( (i, y) => {
            val v1Val = v1(i)
            v1Magnitude -= v1Val * v1Val
            val diff = y - v1Val
            dist += diff * diff
            dist
        })
        sqrt(v1Magnitude + dist)
    }
}
