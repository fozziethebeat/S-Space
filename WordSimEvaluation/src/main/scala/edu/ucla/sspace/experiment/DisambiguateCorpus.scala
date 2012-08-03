package edu.ucla.sspace.experiment

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorIO

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.mutable.ArrayBuffer
import scala.io.Source

import java.io.PrintWriter
import java.io.StringReader


object DisambiguateCorpus {
    def main(args: Array[String]) {

        println("Loading keywords")
        val keyWords = Source.fromFile(args(0)).getLines.zipWithIndex.toList.toMap
        val prototypeFile = Source.fromFile(args(1)).getLines
        val numClusters = args(2).toInt

        println("Loading the wordspace")
        val wordSpace = prototypeFile.sliding(numClusters+1, numClusters+1)
                                     .map(_.mkString("\n"))
                                     .map(new StringReader(_))
                                     .map(VectorIO.readSparseVectors)
                                     .map(asScalaBuffer)
                                     .toArray

        println("Loading the feature basis")
        val basis = new StringBasisMapping()
        val windowSize = 20
        Source.fromFile(args(3)).getLines.foreach(basis.getDimension)
        basis.setReadOnly(true)

        val simFunc = new CosineSimilarity()

        println("Disambiguating the corpus")
        val writer = new PrintWriter(args(5))
        for ((doc, di) <- Source.fromFile(args(4)).getLines.zipWithIndex) {
            printf("Disambiguating document [%d]\n", di)
            var prev = List[String]()
            var next = List[String]() 
            val tokenIter = doc.split("\\s+").iterator
            while (next.size < windowSize && tokenIter.hasNext)
                next = next :+ tokenIter.next
        
            val disambiguatedTokens = ArrayBuffer[String]()
            while (!next.isEmpty) {
                val focus = next.head
                next = next.tail
                if (tokenIter.hasNext)
                    next = next :+ tokenIter.next

                val disambiguated = keyWords.get(focus) match {
                    case Some(vectorId) => {
                        val vectors = wordSpace(vectorId)
                        val v = new CompactSparseVector(basis.numDimensions)
                        for (tokenId <- next.map(basis.getDimension); if tokenId >= 0)
                            v.add(tokenId, 1d)
                        for (tokenId <- prev.map(basis.getDimension); if tokenId >= 0)
                            v.add(tokenId, 1d)
                        val bestSense = vectors.map(simFunc.sim(_, v)).zipWithIndex.max._2
                        focus + "-" + bestSense
                    }
                    case None => focus
                }
                disambiguatedTokens.append(disambiguated)

                prev = prev :+ focus
                if (prev.size > windowSize)
                    prev = prev.tail
            }
            
            writer.println(disambiguatedTokens.mkString(" "))
        }
        writer.close
    }
}
