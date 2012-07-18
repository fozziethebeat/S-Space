package edu.ucla.sspace.experiment

import scala.io.Source

import java.io.PrintWriter


object SplitKey {
    def main(args: Array[String]) {
        val key = Source.fromFile(args(0)).getLines
                                          .map(_.split("\\s+"))
                                          .toList
                                          .groupBy(_(0))
        for ( (k,v) <- key) {
            val p = new PrintWriter(args(1) + "." + k + "." + args(2) + ".key")
            v.map(_.mkString(" ")).foreach(p.println)
            p.close
        }
    }
}
