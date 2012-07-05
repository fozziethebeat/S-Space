package edu.ucla.sspace.experiment

import edu.ucla.sspace.clustering.Partition
import edu.ucla.sspace.similarity.CosineSimilarity
import edu.ucla.sspace.vector.CompactSparseVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.vector.VectorIO
import edu.ucla.sspace.vector.VectorMath

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.collection.JavaConversions.setAsJavaSet

import java.io.FileReader


object LabelFeatureVectors {
    def main(args: Array[String]) {
        val trainPartition = Partition.read(args(0))
        val trainData = VectorIO.readSparseVectors(new FileReader(args(1)))
        val numDimensions = trainData(0).length
        def newPrototype : SparseDoubleVector = new CompactSparseVector(numDimensions)
        def formPrototype(group: Iterable[java.lang.Integer]) =
            group.map(trainData(_)).foldLeft(newPrototype)(VectorMath.add(_, _))
        val prototypes = trainPartition.clusters.map(iterableAsScalaIterable).map(formPrototype)


        val testLabels = Array.fill(trainPartition.numClusters)(Set[Integer]())
        val testData = VectorIO.readSparseVectors(new FileReader(args(2)))
        val simFunc = new CosineSimilarity()
        for ( (testPoint, i) <- testData.zipWithIndex ) {
            val label =  prototypes.map(simFunc.sim(_, testPoint)).zipWithIndex.max._2
            testLabels(label) += new Integer(i)
        }
        PartitionUtil.write(new Partition(testLabels.map(setAsJavaSet).toList), args(3))
    }
}
