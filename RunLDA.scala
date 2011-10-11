import cc.mallet.types.Instance
import cc.mallet.types.InstanceList
import cc.mallet.pipe.Pipe
import cc.mallet.pipe.SerialPipes
import cc.mallet.pipe.CharSequence2TokenSequence
import cc.mallet.pipe.TokenSequence2FeatureSequence
import cc.mallet.topics.ParallelTopicModel

import edu.ucla.sspace.matrix.ArrayMatrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format

import java.io.File
import java.io.PrintWriter
import scala.collection.JavaConversions.asJavaCollection
import scala.io.Source


object RunLDA {
    /**
     * Returns a mallet {@link Instance} object by splitting the given document
     * into a document id and the document text.  It is assumed that the
     * document id is the first token in the document and the text is all
     * remaining text.  This returns an empty {@link AnyVal} when the text is
     * empty.
     */
    def makeInstance(document: String) = {
        val Array(docId, text) = document.split("\\s+", 2)
        if (text != "")
            new Instance(text, "noLabel", docId, null)
    }

    /**
     * Returns a mallet {@link InstanceList} built from a corpus file with one
     * document per line.  Each line will be transformed into a {@link Instance}
     * and added to the {@link InstanceList}.  Tokens in each document will be
     * tokenized based on whitespace.
     */
    def buildInstanceList(path: String) = {
        val pipes = new SerialPipes(List(new CharSequence2TokenSequence("\\S+"),
                                         new TokenSequence2FeatureSequence()))
        val instanceList = new InstanceList(pipes)
        for (line <- Source.fromFile(path).getLines)
            // Try to create the instance object from the line.  If no instance
            // was returned, just ignore it and don't add it to the instance
            // list.
            makeInstance(line) match {
                case inst:Instance => instanceList.addThruPipe(inst)
                case _ =>
            }
        instanceList
    }

    /**
     * Returns the number of available processors.
     */
    def allProcs = Runtime.getRuntime.availableProcessors

    /**
     * Creates and runs a {@link ParallelTopicModel} using Mallet.  The
     * following paramenters will be set automatically before processing.
     */
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

    def printTopWords(outBase:String, topicModel:ParallelTopicModel, 
                      wordsPerTopic:Int) {
        System.err.println("Printing top words")
        val writer = new PrintWriter(outBase + "-ws.dat.top" + wordsPerTopic)
        for (topicWords <- topicModel.getTopWords(wordsPerTopic)) {
            for (topicWord <- topicWords)
                writer.printf("%s ", topicWord)
            writer.println
        }
        writer.close
    }

    def printDocumentSpace(outBase:String, topicModel:ParallelTopicModel,
                           numDocuments:Int, numTopics:Int) {
        System.err.println("Printing Document Space")
        val tFile = File.createTempFile("ldaTheta", "dat")
        tFile.deleteOnExit
        topicModel.printDocumentTopics(tFile)
        val documentSpace = new ArrayMatrix(numDocuments, numTopics)
        for ((line, row) <- Source.fromFile(tFile).getLines.zipWithIndex;
             if row > 0) {
            val tokens = line.split("\\s+")
            for (Array(col, score) <- tokens.slice(2, tokens.length).sliding(2, 2))
                documentSpace.set(row-1, col.toInt, score.toDouble)
        }
        MatrixIO.writeMatrix(documentSpace, 
                             new File(outBase+"-ds.dat.transpose"),
                             Format.DENSE_TEXT)
    }

    def printWordSpace(outBase:String, topicModel:ParallelTopicModel,
                       numTopics:Int) {
        System.err.println("Printing Word Space")
        val tFile = File.createTempFile("ldaTheta", "dat")
        tFile.deleteOnExit
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

        MatrixIO.writeMatrix(wordSpace,
                             new File(outBase+"-ws.dat"), 
                             Format.DENSE_TEXT)
    }

    def main(args:Array[String]) {
        val instances = buildInstanceList(args(0))
        val numTopics = args(2).toInt
        val numDocuments = instances.size
        System.err.println("Training model")
        val topicModel = runLDA(instances, numTopics=numTopics,
                                optimizationInterval=args(3).toInt)
        val outBase = args(1)
        printWordSpace(outBase, topicModel, numTopics)
        printDocumentSpace(outBase, topicModel, numDocuments, numTopics)
        printTopWords(outBase, topicModel, 10)
        System.err.println("Done")
    }
}
