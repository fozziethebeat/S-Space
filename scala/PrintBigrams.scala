import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.util.SerializableUtil

val bigramMatrix = MatrixIO.readSparseMatrix(args(0),Format.SVDLIBC_SPARSE_TEXT)
val basis:BasisMapping[String,String] = SerializableUtil.load(args(1))

for ( r <- 0 until bigramMatrix.rows ) {
    val word = basis.getDimensionDescription(r)
    val rowVec = bigramMatrix.getRowVector(r)
    for (c <- rowVec.getNonZeroIndices)
        printf("%s %s %f\n", word, basis.getDimensionDescription(c),
        rowVec.get(c))
}
