import edu.ucla.sspace.matrix.SymmetricIntMatrix
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.io.Source

import java.io.File


object FormSystemConsensus {
    def addToSet(cluster:String, id:Int, wordMap:HashMap[String, HashSet[Int]]) {
        wordMap.get(cluster) match {
            case Some(idSet) => idSet.add(id)
            case None => val idSet = HashSet[Int](id)
                         wordMap(cluster) = idSet
         }
    }

    def main(args:Array[String]) {
        val wordMatrixMap = (Source.fromFile(args(0)).getLines.map { line =>
            val Array(count, word) = line.trim.split("\\s+")
            (word, new SymmetricIntMatrix(count.toInt, count.toInt))
        }).toMap

        for (systemKey <- args.slice(1, args.length)) {
            val clusterMap = HashMap[String, HashMap[String, HashSet[Int]]]()
            for (line <- Source.fromFile(systemKey).getLines) {
                val Array(word, instance, cluster) = line.split("\\s+")
                val id = instance.split("\\.")(2).toInt
                clusterMap.get(word) match {
                    case Some(wordMap) => addToSet(cluster, id, wordMap)
                    case None => val wordMap = HashMap[String, HashSet[Int]]()
                                 wordMap(cluster) = HashSet(id)
                                 clusterMap(word) = wordMap
                }
            }

            for ( (word, wordMap) <- clusterMap ) {
                val wordMatrix = wordMatrixMap.get(word).get
                for ( cluster <- wordMap.values;
                      List(x,y) <- cluster.toList.combinations(2)) {
                    wordMatrix.set(x-1,y-1, 1+wordMatrix.get(x-1,y-1))
                }
            }
        }

        for ( (word, matrix) <- wordMatrixMap)
            MatrixIO.writeMatrix(matrix, 
                                 new File("semeval07.%s.sys.cm".format(word)),
                                 Format.SVDLIBC_DENSE_TEXT)
    }
}
