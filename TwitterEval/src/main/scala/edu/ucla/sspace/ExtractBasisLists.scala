package edu.ucla.sspace

import Util._

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.vector.CompactSparseVector

import scala.collection.JavaConversions.setAsJavaSet
import scala.io.Source
import scala.xml.XML

import java.io.FileInputStream
import java.io.PrintWriter


object ExtractBasisLists {
    def main(args: Array[String]) {
        if (args.size != 3) {
            println("args: <datafile> <tokenBasis.out> <neBasis.out>")
            System.exit(1)
        }

        val neBasis = new StringBasisMapping()
        val tokenBasis = new StringBasisMapping()

        val data = Source.fromFile(args(0)).getLines
        data.next

        val entityLabelSet = Set("PERSON", "ORGANIZATION", "LOCATION")
        val tokenVector = new CompactSparseVector()
        val neVector = new CompactSparseVector()
        for ((line,i) <- data.zipWithIndex) {
            val Array(timestamp, tweet) = line.split("\\s+", 2)
            try {
                val tweetXml = XML.loadString(tweet)
                for (token <- tokenize(tweetXml.child
                                               .filter(!entityLabelSet.contains(_))
                                               .map(_.text)
                                               .mkString(" "))
                     if validToken(token))
                    tokenVector.add(tokenBasis.getDimension(token), 1d)
                for (entityLabel <- entityLabelSet;
                     ne <- (tweetXml \ entityLabel).map(_.text))
                    neVector.add(neBasis.getDimension(ne), 1d)
            } catch {
                case e => System.err.println("Failed to handle tweet [%d]".format(i))
            }
        }

        writeBasis(args(1), tokenBasis, tokenVector, 4)
        writeBasis(args(2), neBasis, neVector, 2)
    }

    def writeBasis(basisFile: String, basis: StringBasisMapping, vector: CompactSparseVector, limit: Int) {
        val writer = new PrintWriter(basisFile)
        for (i <- vector.getNonZeroIndices; if vector.get(i) > limit)
            writer.println(basis.getDimensionDescription(i))
        writer.close
    }
}
