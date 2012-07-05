package edu.ucla.sspace.experiment

import scala.io.Source

import java.io.PrintWriter


object ExtractWordSimContexts {
    def main(args: Array[String]) {
        val keyWords = Source.fromFile(args(0)).getLines.toSet
        val features = Source.fromFile(args(1)).getLines.toSet
        val mid = args(2).toInt
        val windowSize = 2 * mid + 1

        def acceptToken(token:String) = keyWords.contains(token) || features.contains(token)
        def acceptWindow(window: Array[String]) = window.size == windowSize && keyWords.contains(window(mid))

        var writerMap = Map[String, PrintWriter]()
        val docs = Source.fromFile(args(3)).getLines
        docs.map(_.split("\\s+").filter(acceptToken))
            .flatMap(_.sliding(windowSize))
            .filter(acceptWindow)
            .foreach( context => {
                val keyWord = context(mid)
                val writer = writerMap.get(keyWord) match {
                    case Some(w) => w
                    case None => { 
                        val w = new PrintWriter(args(4) + keyWord + ".txt")
                        writerMap = writerMap + ((keyWord, w))
                        w
                    }
                }
                writer.println(context.mkString(" "))
            })
        writerMap.values.foreach(_.close)
    }
}
