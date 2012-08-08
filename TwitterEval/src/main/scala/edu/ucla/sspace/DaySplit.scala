package edu.ucla.sspace

import scala.io.Source
import scala.xml.XML

import java.io.PrintWriter


object DaySplit {
    def main(args: Array[String]) {
        val tweetStream = Source.fromFile(args(0)).getLines
        tweetStream.next
        val dayTime = 86400
        var timeLimit = 1343026800
        var part = 0
        def makeFileName(partNum: Int) = 
            "%s.part.%03d.dat".format(args(1), part)

        part += 1
        var p = new PrintWriter(makeFileName(part))
        p.println("Time Tweet")
        timeLimit += dayTime

        for ( Array(timeStr, text) <- tweetStream.map(_.split("\\s+", 2));
              if canParse(text)) {
            val time = timeStr.toDouble.toLong
            if (time >= timeLimit) {
                p.close
                part += 1
                p = new PrintWriter(makeFileName(part))
                p.println("Time Tweet")
                timeLimit += dayTime
            }
            p.println("%d %s".format(time, text))
        }
        p.close
    }

    def canParse(text: String) =
        try {
            XML.loadString(text)
            true
        } catch {
            case _ => false
        }
}
