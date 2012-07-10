package edu.ucla.sspace.experiment

import edu.ucla.sspace.dependency.CoNLLDependencyExtractor
import edu.ucla.sspace.dependency.DependencyTreeNode
import edu.ucla.sspace.text.DependencyFileDocumentIterator

import scala.collection.JavaConversions.iteratorAsScalaIterator


object ExtractDependencyPath {
    def main(args: Array[String]) {
        val parser = new CoNLLDependencyExtractor()
        for (doc <- DependencyFileDocumentIterator(args(0))) {
            val reader = doc.reader
            val header = reader.nextLine
            val tree = parser.readNextTree(reader)
            val focusNode = tree.filter(_.lemma == header).first
            printf("%s: %s\n", header, buildParentPath(focusNode))
        }
    }

    def buildParentPath(node: DependencyTreeNode) {
        val link = node.parentLink
        if (link == null)
            node.toString
        else 
            buildParentPath(link.headNode) + "->" + 
            link.relation + "->" + node.toString
    }
}
