import edu.ucla.sspace.vector.CompactSparseVector

import scala.io.Source

import java.io.File


    def readCentroids(clusterFile: String, data:Matrix) = {
        val clusterData = Source.fromFile(clusterFile).getLines 
        if (!clusterData.hasNext)
            System.exit(0)

        val numDataPoints = clusterData.next.split("\\s+")(0).toDouble
        (clusterData map { line => 
            val centroid = new CompactSparseVector(data.columns)
            for ( x <- line.split("\\s+"))
                VectorMath.add(centroid, data.getRowVector(x.toInt))
            centroid
        }).toArray
    }

        val dataset = MatrixIO.readMatrix(
            new File(args(0)), Format.SVDLIBC_SPARSE_TEXT)
        val basis:BasisMapping[String, String] = SerializableUtil.load(args(2))

        for (centroid <- readCentroids(args(1), dataset)) {
            val top10 = (centroid.getNonZeroIndices.map { i =>
                (centroid.get(i), i)}).sorted.reverse.slice(0, 10)
            for ( (_, termId) <- top10)
                printf("%s ", basis.getDimensionDescription(termId))
            println
        }
