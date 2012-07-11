package edu.ucla.sspace.experiment

import cc.mallet.types.Alphabet
import cc.mallet.types.Instance
import cc.mallet.types.InstanceList
import cc.mallet.pipe.Pipe
import cc.mallet.pipe.SerialPipes
import cc.mallet.pipe.CharSequence2TokenSequence
import cc.mallet.pipe.TokenSequence2FeatureSequence
import cc.mallet.pipe.TokenSequenceRemoveStopwords


object InstanceUtil {

    /**
     * Returns a mallet {@link Instance} object by splitting the given document
     * into a document id and the document text.  It is assumed that the
     * document id is the first token in the document and the text is all
     * remaining text.  This returns an empty {@link AnyVal} when the text is
     * empty.
     */
    def makeInstance(document: String, docId:Int) =
        new Instance(document, "noLabel", docId.toString, null)

    def filter(text:String, validWords:Set[String]) = 
        text.split("\\s+").filter(validWords.contains).mkString(" ")
    /**
     * Returns a mallet {@link InstanceList} built from a corpus file with one
     * document per line.  Each line will be transformed into an {@link
     * Instance} and added to the {@link InstanceList}.  Tokens in each document
     * will be tokenized based on whitespace.
     */
    def buildInstanceList(documents: Iterable[String], validWords: List[String]) = {
        val tokenAlphabet = new Alphabet(validWords.size())
        validWords.foreach(w => tokenAlphabet.lookupIndex(w))
        val instanceList = new InstanceList(tokenAlphabet, null)

        val validTokens = validWords.toSet
        val pipes = new SerialPipes(List(new CharSequence2TokenSequence("\\S+"),
                                         new TokenSequence2FeatureSequence(tokenAlphabet)))
        instanceList.setPipe(pipes)

        // Try to create the instance object from the line.  If no instance
        // was returned, just ignore it and don't add it to the instance
        // list.
        for ((doc, i) <- documents.zipWithIndex)
            instanceList.addThruPipe(makeInstance(filter(doc, validTokens), i))
        instanceList
    }
}
