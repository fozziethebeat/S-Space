package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorIO
import edu.ucla.sspace.vector.VectorMath

import scala.collection.mutable.Buffer
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList

import java.io.FileReader


object FormPrototypes {
    def main(args: Array[String]) {
        val partition = Partition.read(args(0))
        val data = VectorIO.readSparseVectors(new FileReader(args(1)))
        val numDimensions = data(0).length
        def newPrototype : SparseDoubleVector = new CompactSparseVector(numDimensions)
        def formPrototype(group: Iterable[java.lang.Integer]) =
            group.map(data(_)).foldLeft(newPrototype)(VectorMath.add(_, _))
        val prototypes = partition.clusters.map(iterableAsScalaIterable).map(formPrototype)
        VectorIO.writeVectors(prototypes.toList, args(2))
    }
}
