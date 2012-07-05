package edu.ucla.sspace.experiment

import edu.ucla.sspace.dependency.DependencyTreeNode
import edu.ucla.sspace.dependency.SimpleDependencyTreeNode

import gov.llnl.ontology.text.parse.TreeMaltLinearParser
import gov.llnl.ontology.text.tag.OpenNlpMEPOSTagger
import gov.llnl.ontology.text.tokenize.OpenNlpMETokenizer

import scala.collection.JavaConversions.asScalaBuffer

import java.io.PrintWriter


object ParseSemEval2007 {
    def main(args: Array[String]) {
        println("Loading utility serices")
        val tokenizer = new OpenNlpMETokenizer()
        val posTagger = new OpenNlpMEPOSTagger()
        val parser = new TreeMaltLinearParser()
        val xmlTree = NoDTDXml.loadFile(args(0))
        for (lexelt <- xmlTree \ "lexelt") {
            val word = (lexelt \ "@item").text
            println("Processing " + word)
            val writer = new PrintWriter(args(1) + word + ".conll")
            for (instance <- lexelt \ "instance") {
                val id = (instance \ "@id").text
                val tokenParts = instance.child.map(_.text)
                                               .map(tokenizer.tokenize)
                val focus = (if (instance.child(0).label == "head") 0
                             else tokenParts(0).size)
                val tokens = tokenParts.reduce(_++_)
                val tags = posTagger.tag(tokens)
                def lemma(i: Int) = if (i == focus) id else tokens(i)
                val tree = for (i <- 0 until tokens.size) yield
                    new SimpleDependencyTreeNode(tokens(i), tags(i), lemma(i), i)
                val parsed = parser.parse(tree.toArray)
                writer.println(id)
                for (node <- parsed) {
                    val (parent, relation) = getLink(node)
                    writer.println(toString(node, parent, relation))
                }
                writer.println
            }
            writer.close
        }
    }

    def getLink(node: DependencyTreeNode) =
        node.neighbors.foldLeft( (0, "null") ) ( (s, r) => if (r.dependentNode == node) (r.headNode.index+1, r.relation) else s )
    def toString(node: DependencyTreeNode, parent: Int, relation: String) = 
        "%d\t%s\t%s\t%s\t%s\t_\t%d\t%s".format(
            node.index, node.word, node.lemma, node.pos, node.pos, parent, relation)
}
