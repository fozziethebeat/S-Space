package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.Partition

import scala.io.Source
import scala.collection.JavaConversions.iterableAsScalaIterable


object LabelPartition {
    def main(args: Array[String]) {
        val word = args(0)
        val partition = Partition.read(args(1))
        val headers = Source.fromFile(args(2)).getLines.toArray
        for ((cluster, cid) <- partition.clusters.zipWithIndex;
             label <- cluster.map(headers(_)))
            printf("%s %s %s.%d\n", word, label, word, cid)
    }
}
