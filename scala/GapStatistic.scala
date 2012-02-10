import edu.ucla.sspace.clustering.Assignments
import edu.ucla.sspace.clustering.GapStatistic
import edu.ucla.sspace.clustering.CKVWSpectralClustering06
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format

import scala.io.Source


object GapStatistic {
    def main(args:Array[String]) {
        val headers = Source.fromFile(args(1)).getLines.toList
        val term = headers(0).replaceAll(".[0-9]+", "")
        val dataset = MatrixIO.readSparseMatrix(args(0), Format.SVDLIBC_SPARSE_TEXT)
        val clustering = new CKVWSpectralClustering06()
        val assignments = clustering.cluster(dataset, System.getProperties)
        for ( (instance, i) <- headers.zipWithIndex )
            printf("%s %s %s.%d\n", term, instance, term, assignments.get(i))
    }
}
