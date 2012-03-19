import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.sim.JaccardIndex
import edu.ucla.sspace.text.DependencyFileDocumentIterator
import edu.ucla.sspace.util.SerializableUtil
import edu.ucla.sspace.vector.CompactSparseVector

import scala.collection.JavaConversions.asScalaIterator
import scala.io.Source


// Arguments:
// (1) solution file
// (2) basis file
// (3) context file

val lines = Source.fromFile(args(0)).getLines
val Array(numPoints, numClusters) = lines.next.split("\\s+").map(_.toInt)
val clusters = Array.fill(numClusters)(new CompactSparseVector(numPoints))
for ( (line, cid) <- lines.zipWithIndex;
      if line != "";
      point <- line.split("\\s+") )
    clusters(cid).add(point.toInt, 1.0)

// Read in the basis mapping and set it to read only.
val basis:BasisMapping[String, String] = SerializableUtil.load(args(1))
basis.setReadOnly(true)

// Iterate through each document in the corpus and determine which cluster has
// the highset jaccard similarity to it.
val sim = new JaccardIndex()
for ( (header, v) <- readContexts(args(2), basis)) {
    val term = header.replaceAll(".[0-9]+", "")
    val label = clusters.zipWithIndex.map(x=>(sim.sim(x._1, v), x._2)).max._2
    printf("%s %s %s.%d\n", term, header, term, label)
}

def readContexts(contextFile: String, basis:BasisMapping[String, String]) = {
    val docIter = new DependencyFileDocumentIterator(contextFile)
    for (doc <- docIter) yield {
        val reader = doc.reader
        val header = reader.readLine
        val rowVector = new CompactSparseVector()
        var line = reader.readLine
        while (line != null) {
            val d = basis.getDimension(line.split("\\s+")(1))
            if (d >= 0)
                rowVector.set(d, 1)
            line = reader.readLine
        }
        (header, rowVector)
    }
}
