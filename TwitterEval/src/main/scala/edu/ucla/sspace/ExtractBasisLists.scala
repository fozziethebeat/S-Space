package edu.ucla.sspace

import Util._

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.vector.CompactSparseVector

import opennlp.tools.tokenize._

import scala.collection.JavaConversions.setAsJavaSet
import scala.io.Source
import scala.xml.XML

import java.io.FileInputStream
import java.io.PrintWriter


object ExtractBasisLists {
    def main(args: Array[String]) {
        if (args.size != 4) {
            println("args: <datafile> <tokenizer> <tokenBasis.out> <neBasis.out>")
            System.exit(1)
        }

        val neBasis = new StringBasisMapping()
        val tokenBasis = new StringBasisMapping()

        val data = Source.fromFile(args(0)).getLines
        data.next

        val tokenizerModel = new TokenizerModel(new FileInputStream(args(1)))
        val tokenizer = new TokenizerME(tokenizerModel)

        val tokenVector = new CompactSparseVector()
        val neVector = new CompactSparseVector()
        for ((line,i) <- data.zipWithIndex) {
            val Array(timestamp, tweet) = line.split("\\s+", 2)
            try {
                val tweetXml = XML.loadString(tweet)
                for (token <- tokenizer.tokenize(tweetXml.child
                                                         .filter(_.label!="PERSON")
                                                         .map(_.text)
                                                         .mkString(" ")
                                                         .toLowerCase)
                                       .filter(notUser)
                                       .map(normalize)
                                       .filter(validToken))
                    tokenVector.add(tokenBasis.getDimension(token), 1d)
                for (ne <- (tweetXml \ "PERSON").map(_.text))
                    neVector.add(neBasis.getDimension(ne), 1d)
            } catch {
                case e => System.err.println("Failed to handle tweet [%d]".format(i))
            }
        }

        val tokenWriter = new PrintWriter(args(2))
        for (i <- tokenVector.getNonZeroIndices)
            if (tokenVector.get(i) > 4 )
                tokenWriter.println(tokenBasis.getDimensionDescription(i))
        tokenWriter.close
        val neWriter = new PrintWriter(args(3))
        for (i <- neVector.getNonZeroIndices)
            if (neVector.get(i) > 4 )
                neWriter.println(neBasis.getDimensionDescription(i))
        neWriter.close
    }
}
