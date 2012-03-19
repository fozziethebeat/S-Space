import scala.io.Source

// Read the headers for each feature vector.
val headers = Source.fromFile(args(0)).getLines.toList
val term = headers(0).replaceAll(".[0-9]+", "")
// Read each clustering solution and report the cluster id for each instance in
// the sem eval format.
val solution = Source.fromFile(args(1)).getLines
solution.next
for ((line, clusterId) <- solution zipWithIndex;
     if line != "";
     x <- line.split("\\s+") )
    printf("%s %s %s.%d\n", term, headers(x.toInt), term, clusterId)
