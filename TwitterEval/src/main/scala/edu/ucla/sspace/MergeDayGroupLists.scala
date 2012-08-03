package edu.ucla.sspace

import scala.io.Source


object MergeDayGroupLists {
    def main(args: Array[String]) {
        println("Time,Group")
        var groups = Set[String]()
        for ( (file, fi) <- args.zipWithIndex) {
            val tweetIter = Source.fromFile(file).getLines
            tweetIter.next
            for ( Array(time, group) <- tweetIter.map(_.split("\\s+", 2)) ) {
                groups += (fi + "-" + group)
                printf("%s,%d\n",time,groups.size)
            }
        }
    }
}
