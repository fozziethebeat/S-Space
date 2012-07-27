package edu.ucla.sspace

import scala.io.Source

import java.io.PrintWriter


object MergeTaggedData {
    def main(args: Array[String]) {
        val writer = new PrintWriter(args(0))

        val timestampedData = Source.fromFile(args(1)).getLines
        val nerTaggedData = Source.fromFile(args(2)).getLines

        writer.println(timestampedData.next)

        timestampedData.map(_.split("\\s+")(0)).zip(nerTaggedData).foreach(
            t => writer.println("%s %s".format(t._1, t._2)))
        writer.close
    }
}
