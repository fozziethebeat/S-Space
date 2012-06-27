package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.AgglomerativeConsensusFunction
import edu.ucla.sspace.clustering.BestOfKConsensusFunction
import edu.ucla.sspace.clustering.BestOneElementMoveConsensusFunction
import edu.ucla.sspace.clustering.Partition

import scala.collection.JavaConversions.seqAsJavaList


object ComposeConsensusSolution {
    def main(args: Array[String]) {
        val numClusters = args(0).toInt
        val mergeType = args(1)
        val outFile = args(2)

        System.err.println("reporter:status:Loading partitions")
        val partitions = args.slice(3, args.size).map(Partition.read(_)).toList

        System.err.println("reporter:status:Finding Consensus Partition")
        val func = mergeType match {
            case "boem" => new BestOneElementMoveConsensusFunction()
            case "agglo" => new AgglomerativeConsensusFunction()
            case "bok" => new BestOfKConsensusFunction()
        }
        val best = func.consensus(partitions, numClusters)

        System.err.println("reporter:status:Done")
        PartitionUtil.write(best, outFile)
    }
}
