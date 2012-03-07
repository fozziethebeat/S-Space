import scala.collection.mutable.HashMap
import scala.io.Source


// Read in each line of the mapping, evaluation, and solution keys and organize
// it according to the word.
val solutionSet = readKey(args(0))
val mappingSet = readKey(args(1))
val evaluationSet = readKey(args(2))

// Iterate through each word and instance set in the solution set.
var correct =  0
var total = 0
for ( (word, instances) <- solutionSet ) {
    // Turn the instance set into a mapping from instance ids to assignments.
    val solutionMap = instanceSet(instances)
    val clusterIndexer = new HashMap[String, Int]() with Indexer
    val numClusters = solutionMap.values.toSet.size

    // Turn the mapping set for tihs word into the same style mapping.
    val trueMap = instanceSet(mappingSet(word))
    val labelIndexer = new HashMap[String, Int]() with Indexer
    val numClasses = trueMap.values.toSet.size

    // Create a matrix recording the joint probability of seeing instances with
    // each cluster id and each label id.
    //val matches = Array.fill(numClusters, numClasses)(0.0)
    val matches = Array.fill(numClusters, numClasses)(0.0)

    // Iterate through each instance id and cluster label pairing and compute
    // the counts of seeing each possible cluster id and class labeling.  Only
    // accept ids that are in the true map.  Ignore the others.
    for ( (id, clusterId) <- solutionMap;
          if trueMap.contains(id) ) {
        val i = clusterIndexer.getDim(clusterId)
        val j = labelIndexer.getDim(trueMap(id))
        matches(i)(j) += 1
    }

    // Get the test map for this word.
    val testMap = instanceSet(evaluationSet(word))

    // Iterate through the solution map again and accept only the ids in the
    // test map.  For each evaluation instance, compute the most likely class
    // label and test whether or not it's correct.
    for ( (id, clusterId) <- solutionMap;
          if testMap.contains(id) ) {
        val goldLabel = labelIndexer.getDim(testMap(id))
        val solutionLabel = matches(clusterIndexer.getDim(clusterId)).zipWithIndex.max._2
        if (goldLabel == solutionLabel)
            correct += 1
    }
    total += testMap.size
}
printf("%f\n", correct/total.toDouble)

def readKey(keyFile: String) =
    Source.fromFile(keyFile).getLines.map(_.split("\\s+")).toList.groupBy(_(0))

def instanceSet(instances: List[Array[String]]) =
    instances.map(a => (a(1), a(2))).toMap

trait Indexer extends HashMap[String, Int] {
    def getDim(key: String) =
        get(key) match {
            case Some(value) => value
            case None => val value = size
                         put(key, value)
                         value
        }
}
