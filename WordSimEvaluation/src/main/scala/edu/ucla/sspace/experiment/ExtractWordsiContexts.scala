package edu.ucla.sspace.experiment

import edu.ucla.sspace.basis.StringBasisMapping
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.VectorIO

import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Source


object ExtractWordsiContexts {
    def main(args: Array[String]) {
        // Load the basis mapping from a fixed set of words.  Any words not in
        // this list will not be represented.
        val basis = new StringBasisMapping()
        Source.fromFile(args(0))
              .getLines
              .foreach(basis.getDimension)
        basis.setReadOnly(true)

        def toVector(wordIds: Array[Int]) = {
            val v = new CompactSparseVector(basis.numDimensions)
            wordIds.foreach(i => v.add(i, 1d))
            v
        }

        val contexts = Source.fromFile(args(1))
                             .getLines
                             .map(_.split("\\s+")
                                   .map(basis.getDimension)
                                   .filter(_>=0))
                             .map(toVector)

        VectorIO.writeVectors(contexts.toList, args(2));
    }
}
