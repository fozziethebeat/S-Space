package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.Partition

import scala.collection.JavaConversions.iterableAsScalaIterable

import java.io.PrintWriter 


object PartitionUtil {
    def write(p: Partition, outputFileName: String) {
        val pw = new PrintWriter(outputFileName)
        pw.println("%d %d".format(p.numPoints, p.numClusters))
        pw.println(p.clusters.filter(_.size > 0)
                    .map(_.mkString(" "))
                    .mkString("\n"))
        pw.close
    }
}

