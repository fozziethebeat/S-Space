package edu.ucla.sspace.experiment

import breeze.linalg.DenseVector

import edu.ucla.sspace.clustering.Partition

import scala.collection.JavaConversions.iterableAsScalaIterable

import java.io.PrintWriter


object InferClusterTopics {
    def main(args: Array[String]) {
        val contextTopics = Source.fromFile(args(0))
                                  .getLines
                                  .map(_.split("\\s+").map(_.toDouble))
                                  .map(DenseVector[Double](_))
                                  .toArray
        val partitions = PartitionUtil.read(args(1))
        val writer = new PrintWriter(args(2))
        def averageTopic(group: Set[Integer]) =
            group.map(contextTopics).reduce(_+_) / group.size
        for (group <- partitions.clusters)
            writer.println(averageTopic(group).data.mkString(" "))
        writer.close
    }
}
