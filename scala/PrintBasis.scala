import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.util.SerializableUtil

val basis:BasisMapping[String, String] = SerializableUtil.load(args(0))
for (x <- 0 until basis.numDimensions) 
   println(basis.getDimensionDescription(x))
