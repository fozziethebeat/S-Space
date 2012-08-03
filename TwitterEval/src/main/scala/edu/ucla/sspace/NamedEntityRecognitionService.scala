package edu.ucla.sspace

import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.CoreLabel
import edu.stanford.nlp.ling.Word
import edu.stanford.nlp.util.StringUtils
import edu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter
import edu.stanford.nlp.sequences.PlainTextDocumentReaderAndWriter.OutputStyle

import scala.collection.JavaConversions.collectionAsScalaIterable
import scala.collection.JavaConversions.seqAsJavaList
import scala.io.Source


object NamedEntityRecognitionService {
    def main(args: Array[String]) {
        val props = StringUtils.argsToProperties(args)
        val crf = new CRFClassifier[CoreLabel](props)
        val loadPath = crf.flags.loadClassifier;
        val textFile = crf.flags.textFile;
        crf.loadClassifierNoExceptions(loadPath, props)

        val readerAndWriter = new PlainTextDocumentReaderAndWriter[CoreLabel]()
        readerAndWriter.init(crf.flags)
        for (line<- Source.fromFile(textFile).getLines) {
            println(crf.classify(line)
                       .map(tagged => readerAndWriter.getAnswers(tagged, OutputStyle.INLINE_XML, true))
                       .mkString(" "))
        }
    }
}
