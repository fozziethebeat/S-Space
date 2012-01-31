import cc.mallet.classify._
import cc.mallet.types._

import edu.ucla.sspace.basis.BasisMapping
import edu.ucla.sspace.matrix.MatrixIO
import edu.ucla.sspace.matrix.MatrixIO.Format
import edu.ucla.sspace.vector.DoubleVector
import edu.ucla.sspace.vector.SparseDoubleVector
import edu.ucla.sspace.util.SerializableUtil

import scala.collection.JavaConversions.asScalaBuffer
import scala.io.Source
import scala.util.Random

import java.io.File


object ClassifySemEval {

    def makeInstance(rowVector:DoubleVector, sense: String, id:
                     String, alphabet: Alphabet, classes: LabelAlphabet) = {
        val (nonZeros, values) = rowVector match {
            case sv:SparseDoubleVector => {
                val nz = sv.getNonZeroIndices
                (nz, nz.map( i => sv.get(i)))
            }
            case v:DoubleVector =>
                (0 until v.length toArray, v.toArray)
        }
        new Instance(new FeatureVector(alphabet, nonZeros, values),
                     classes.lookupLabel(sense), id, null)
    }
        
    def main(args:Array[String]) {
        System.err.println(args(1))
        val m = if (args(1).contains("lda") ||
                    args(1).contains("svd") ||
                    args(1).contains("nmf"))
                MatrixIO.readMatrix(new File(args(1)), Format.DENSE_TEXT)
            else
                MatrixIO.readSparseMatrix(args(1), Format.SVDLIBC_SPARSE_TEXT)

        val headers = Source.fromFile(args(2)).getLines.toList
        val labels = Source.fromFile(args(3)).getLines.toList
        val testLabels = Source.fromFile(args(4)).getLines.map(_.split("\\s+")(1)).toSet

        val terms:Array[Object] = (0 until m.columns).map(_.toString).toArray
        val alphabet = new Alphabet(terms)

        val classes = new LabelAlphabet()
        val instanceList = new InstanceList(alphabet, classes)
        for ( r <- 0 until m.rows; if !testLabels.contains(headers(r)) )
            instanceList.add(makeInstance(
                m.getRowVector(r), labels(r), headers(r), alphabet, classes))

        val trainer = args(0) match {
            case "nb" => new NaiveBayesTrainer()
            case "c45" => new C45Trainer()
            case "dt" => new DecisionTreeTrainer()
            case "me" => new MaxEntTrainer()
            case "bag" => new BaggedEnsembleTrainer(100)
        }

        System.err.println("reporter:status:Training with " + args(0))
        val classifier = trainer.train(instanceList)
        val term = headers(0).replaceAll(".[0-9]+", "")
        System.err.println("reporter:status:Labeling with " + args(0))
        for ( r <- 0 until m.rows; if testLabels.contains(headers(r))) {
            val i = makeInstance(m.getRowVector(r), labels(r), headers(r), alphabet, classes)
            printf("%s %s %s\n", term, headers(r),
                   classifier.classify(i).getLabeling.getBestLabel)
        }
    }

    class BaggedEnsembleTrainer(numTrees:Int) 
            extends ClassifierTrainer[ClassifierEnsemble] {
        // Create the desired number of decision trees.
        val decisionTrees = Array.fill(numTrees)(new DecisionTreeTrainer())
        // Give each tree even weight.
        val weights = Array.fill(numTrees)(1.0/numTrees.toDouble)

        def getClassifier = new ClassifierEnsemble(decisionTrees.map(_.getClassifier), weights)

        def train(trainingSet: InstanceList) = {
            val dataAlphabet = trainingSet.getDataAlphabet
            val targetAlphabet = trainingSet.getTargetAlphabet
            val numPoints = trainingSet.size
            // Train each decision tree by using simple bagging.  Also, do this
            // in parallel.
            decisionTrees.par.foreach( tree => {
                // Create a bag for each decision tree where the bag is the same
                // size as the original training set but is selected from the
                // training points at random with replacement.  Some points may
                // be overrepresented and others may not be represented at all
                // for each invidiual ensemble.
                val bag = new InstanceList(dataAlphabet, targetAlphabet)
                for (i <- 0 until numPoints)
                    bag.add(trainingSet.get(Random.nextInt(numPoints)))

                // Train the decision tree.
                tree.train(bag)
            })
            getClassifier
        }
    }
}
