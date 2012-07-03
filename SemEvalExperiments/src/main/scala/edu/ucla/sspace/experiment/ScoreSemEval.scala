package edu.ucla.sspace.experiment 

import edu.ucla.sspace.clustering.PairedFScoreComparison
import edu.ucla.sspace.clustering.AdjustedMutualInformationComparison
import edu.ucla.sspace.clustering.VMeasureComparison
import edu.ucla.sspace.clustering.Partition

import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet
import scala.io.Source


object ScoreSemEval {
    def main(args: Array[String]) {
        val func = args(0) match {
            case "fscore" => new PairedFScoreComparison()
            case "vmeasure" => new VMeasureComparison()
            case "ami" => new AdjustedMutualInformationComparison()
        }

        val pos = if (args(3) == "all") "" else args(3)
        val solWords = answerMap(Source.fromFile(args(1)).getLines, pos)
        val goldWords = answerMap(Source.fromFile(args(2)).getLines, pos)

        val average = (for ( (word, solLabels) <- solWords.filter(x=>goldWords.contains(x._1))) yield {
            val goldLabels = goldWords(word)
            func.compare(toPartition(solLabels), toPartition(goldLabels))
        }).sum / goldWords.size.toDouble

        println(average)
    }

    def answerMap(lines: Iterator[String], pos: String) = 
        lines.toList
             .map(_.split("\\s+"))
             .filter(_(0).endsWith(pos))
             .groupBy(_(0))
             .toMap

    def toInt(i: Int) = new java.lang.Integer(i)
    def getId(items: Array[String]) = toInt(items(1).split("\\.")(2).toInt - 1)
    def toPartition(labels: Seq[Array[String]]) = 
        new Partition(labels.groupBy(_(2))
                            .toList
                            .map(_._2.map(getId).toSet)
                            .map(setAsJavaSet))
}
