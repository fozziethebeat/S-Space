package edu.ucla.sspace

import scala.io.Source

import java.util.Date
import java.text.SimpleDateFormat


object MergeDaySplitsLists {
    def main(args: Array[String]) {
        println("Start,Mean,Group,Summary")
        var groups = Set[String]()
        for ( (file, fi) <- args.zipWithIndex) {
            val tweetIter = Source.fromFile(file).getLines
            tweetIter.next
            for ( Array(startTime, meanTime, group, summary) <- tweetIter.map(_.split("\\s+", 4)) ) {
                groups += (fi + "-" + group)
                printf("%s,%s,%d,%s\n", startTime, meanTime, groups.size, summary.substring(1, summary.size-1).replaceAll(",", ""))
            }
        }
    }
}
