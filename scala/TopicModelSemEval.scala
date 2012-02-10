import cc.mallet.types.Instance
import cc.mallet.types.InstanceList
import cc.mallet.pipe.Pipe
import cc.mallet.pipe.SerialPipes
import cc.mallet.pipe.CharSequence2TokenSequence
import cc.mallet.pipe.TokenSequence2FeatureSequence
import cc.mallet.topics.ParallelTopicModel

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.text.DependencyFileDocumentIterator

import scala.collection.JavaConversions.asScalaIterator
import scala.io.Source

import java.io.File
import java.io.PrintWriter


object TopicModelSemEval {

    def allProcs = Runtime.getRuntime.availableProcessors

    def runLDA(instanceList:InstanceList, alpha:Double = 50.0, beta:Double = 0.01,
               showTopicInterval:Int = 50, topWordsPerInterval:Int = 10,
               numTopics:Int = 50, numIterations:Int = 500,
               optimizationInterval:Int = 25, optimizationBurnin:Int = 200,
               useSymmetricAlpha:Boolean = false, numThreads:Int = allProcs) = {
        val topicModel = new ParallelTopicModel(numTopics, alpha, beta)
        topicModel.addInstances(instanceList)
        topicModel.setTopicDisplay(showTopicInterval, topWordsPerInterval)
        topicModel.setNumIterations(numIterations)
        topicModel.setOptimizeInterval(optimizationInterval)
        topicModel.setBurninPeriod(optimizationBurnin)
        topicModel.setSymmetricAlpha(useSymmetricAlpha)
        topicModel.setNumThreads(numThreads)
        topicModel.estimate
        topicModel
    }

    def printWordSpace(outBase:String, topicModel:ParallelTopicModel,
                       numTopics:Int) {
        System.err.println("Printing Word Space")
        val tFile = File.createTempFile("ldaTheta", "dat")
        //tFile.deleteOnExit
        topicModel.printTopicWordWeights(tFile)

        val wordMap = (Source.fromFile(tFile).getLines.takeWhile { 
            line => line(0) == '0' } map {
            line => line.split("\\s+")(1) }).zipWithIndex.toMap
        val wordSpace = new ArrayMatrix(wordMap.size, numTopics)
        val rowSums = new Array[Double](wordMap.size)
        for (line <- Source.fromFile(tFile).getLines) {
            val Array(col, word, score) = line.split("\\s+")
            val row = wordMap(word)
            wordSpace.set(row, col.toInt, score.toDouble)
            rowSums(row) += score.toDouble
        }
        for (r <- 0 until wordSpace.rows;
             c <- 0 until wordSpace.columns)
            wordSpace.set(r, c, wordSpace.get(r, c) / rowSums(r))

        MatrixIO.writeMatrix(wordSpace, outBase, Format.DENSE_TEXT)
    }

    def printDocumentSpace(outBase:String, topicModel:ParallelTopicModel,
                           numDocuments:Int, numTopics:Int) {
        System.err.println("Printing Document Space")
        val tFile = File.createTempFile("ldaTheta", "dat")
        //tFile.deleteOnExit
        topicModel.printDocumentTopics(tFile)
        val documentSpace = new ArrayMatrix(numDocuments, numTopics)
        for ((line, row) <- Source.fromFile(tFile).getLines.zipWithIndex;
             if row > 0) {
            val tokens = line.split("\\s+")
            for (Array(col, score) <- tokens.slice(2, tokens.length).sliding(2, 2))
                documentSpace.set(row-1, col.toInt, score.toDouble)
        }
        MatrixIO.writeMatrix(documentSpace, new File(outBase), Format.DENSE_TEXT)
    }

    def main(args:Array[String]) {
        val docIter = new DependencyFileDocumentIterator(args(0))
        val excluded = Source.fromFile(args(1)).getLines.toSet
        val extractor = new CoNLLDependencyExtractor()
        val pipes = new SerialPipes(Array(new CharSequence2TokenSequence("\\S+"),
                                          new TokenSequence2FeatureSequence()))
        val instanceList = new InstanceList(pipes)
        for (doc <- docIter) {
            val header = doc.reader.readLine
            val depTree = extractor.readNextTree(doc.reader)
            val text = depTree.map(_.word).filter(!excluded.contains(_))
            instanceList.addThruPipe(new Instance(
                text.mkString(" "), header, header, null))
        }

        val numTopics = args(2).toInt
        val numDocuments = instanceList.size
        val topicModel = runLDA(instanceList, numTopics=numTopics)
        printDocumentSpace(args(3), topicModel, numDocuments, numTopics)
    }
}
