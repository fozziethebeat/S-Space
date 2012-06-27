package edu.ucla.sspace.experiment

import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.similarity.EuclideanSimilarity
import edu.ucla.sspace.similarity.LinSimilarity
import edu.ucla.sspace.vector.VectorIO

import scala.collection.JavaConversions.iterableAsScalaIterable

import java.io.FileReader


object ComparePrototypes {
    def main(args:Array[String]) {
        val simFunc = args(0) match {
            case "cosine" =>  new CosineSimilarity()
            case "euclidean" => new EuclideanSimilarity()
            case "lin" => new LinSimilarity()
            case _ => throw new IllegalArgumentException(
                args(0) + " is not a valid similarity function")
        }

        val matchFunc = args(1) match {
            case "avg" => average _
            case "max" => maximum _
        }

        try {
            val prototypes1 = VectorIO.readSparseVectors(new FileReader(args(2)))
            val prototypes2 = VectorIO.readSparseVectors(new FileReader(args(3)))

            val scores = for (p1 <- prototypes1; p2 <- prototypes2) yield simFunc.sim(p1, p2)
            println(matchFunc(scores))
        } catch {
            case _ => println(0d)
        }
    }

    def maximum(scores: Iterable[Double]) = scores.max
    def average(scores: Iterable[Double]) = {
        val scoreList = scores.toList
        val numScores = scoreList.size
        scoreList.sum / numScores
    }
}
