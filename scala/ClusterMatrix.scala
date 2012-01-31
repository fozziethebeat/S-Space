import edu.ucla.sspace.clustering.Clustering
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.ReflectionUtil
import java.io.File

val matrix = MatrixIO.readMatrix(
    new File(args(0)), Format.valueOf(args(1)))
val alg:Clustering = ReflectionUtil.getObjectInstance(args(2))
for ( as <- alg.cluster(matrix, args(3).toInt, System.getProperties).assignments)
    println(as(0));
