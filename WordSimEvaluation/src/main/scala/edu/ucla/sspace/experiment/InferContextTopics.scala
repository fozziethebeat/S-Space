package edu.ucla.sspace.experiment

import cc.mallet.topics.ParallelTopicModel

import scala.collection.JavaConversions.iterableAsScalaIterable

import java.io.PrintWriter


object InferContextTopics {
    def main(args: Array[String]) {
        val topicModel = ParallelTopicModel.read(new File(args(0)))
        val inferencer = topicModel.getInferencer
        val documents = Source.fromFile(args(1)).getLines
        val contentWords = Source.fromFile(args(2)).getLines
        val instances = InstanceUtil.buildInstanceList(documents, contentWords)
        var summaryTopic = DenseVector.zeros[Double](inferencer.numTopics)
        {
        val writer = new PrintWriter(args(3))
        for (instance <- instances) {
            val topicSignature = inferencer.getSampledDistribution(instance, 100, 10, 10)
            summaryTopic += topicSignature
            writer.println(topicSignature.data.mkString(" "))
        }
        writer.close
        }
        summaryTopic = summaryTopic / instances.size
        {
        val writer = new PrintWriter(args(4))
        writer.println(summaryTopic.data.mkString(" "))
        writer.close
        }
    }
}
